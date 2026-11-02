"""
Integration and Unit Tests for MILO User Authentication Service and FastAPI Endpoints
=====================================================================================
Tests:
1. User registration with valid data
2. Duplicate email rejection (AUTH-02)
3. Duplicate username rejection (AUTH-01/AUTH-02)
4. Duplicate phone rejection (AUTH-03)
5. User login via email and password
6. User login via username and password
7. Invalid password rejection
8. Non-existent user login rejection
9. JWT token validation and get_current_user
10. Refresh token exchange for new access token
11. Logout and token revocation
12. Refresh token failure after revocation
13. Password change and session invalidation
14. FastAPI REST Endpoints via TestClient (/api/v1/auth/register, login, me, refresh, logout)
"""

import uuid
import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session

from app.main import app
from app.db.session import SessionLocal
from app.services.auth_service import (
    AuthService,
    EmailAlreadyExistsError,
    UsernameAlreadyExistsError,
    PhoneNumberAlreadyExistsError,
    InvalidCredentialsError,
    SessionRevokedOrExpiredError,
)
from app.schemas.auth import (
    UserRegisterRequest,
    UserLoginRequest,
    RefreshTokenRequest,
    PasswordChangeRequest,
)
from app.models.entities import User, UserSession


@pytest.fixture
def db():
    session = SessionLocal()
    try:
        yield session
    finally:
        session.close()


@pytest.fixture
def auth_service(db: Session):
    return AuthService(db=db)


@pytest.fixture
def client():
    return TestClient(app)


def cleanup_user(db: Session, email: str):
    """Clean up user and associated sessions by email."""
    user = db.query(User).filter(User.email == email).first()
    if user:
        db.query(UserSession).filter(UserSession.user_id == user.user_id).delete()
        db.delete(user)
        db.commit()


def test_auth_service_registration_and_duplicate_handling(db: Session, auth_service: AuthService):
    uid = uuid.uuid4().hex[:8]
    email = f"test_{uid}@example.com"
    username = f"user_{uid}"
    phone = f"+1555{uid[:7]}"

    cleanup_user(db, email)

    try:
        # 1. Registration
        reg_req = UserRegisterRequest(
            full_name="Test User",
            username=username,
            email=email,
            password="SecurePassword123!",
            phone_number=phone,
        )
        auth_resp = auth_service.register(reg_req)

        assert auth_resp.user.email == email
        assert auth_resp.user.username == username
        assert auth_resp.tokens.access_token is not None
        assert auth_resp.tokens.refresh_token is not None
        assert not hasattr(auth_resp.user, "password_hash")

        # 2. Duplicate email rejected
        with pytest.raises(EmailAlreadyExistsError):
            auth_service.register(
                UserRegisterRequest(
                    full_name="Duplicate Email",
                    username=f"other_{uid}",
                    email=email,
                    password="Password123!",
                )
            )

        # 3. Duplicate username rejected
        with pytest.raises(UsernameAlreadyExistsError):
            auth_service.register(
                UserRegisterRequest(
                    full_name="Duplicate User",
                    username=username,
                    email=f"unique_{uid}@example.com",
                    password="Password123!",
                )
            )

        # 4. Duplicate phone rejected
        with pytest.raises(PhoneNumberAlreadyExistsError):
            auth_service.register(
                UserRegisterRequest(
                    full_name="Duplicate Phone",
                    username=f"phone_{uid}",
                    email=f"phone_{uid}@example.com",
                    password="Password123!",
                    phone_number=phone,
                )
            )

    finally:
        cleanup_user(db, email)


def test_auth_service_login_and_token_workflows(db: Session, auth_service: AuthService):
    uid = uuid.uuid4().hex[:8]
    email = f"login_{uid}@example.com"
    username = f"login_{uid}"
    password = "MyStrongPassword123!"

    cleanup_user(db, email)

    try:
        # Register user
        auth_service.register(
            UserRegisterRequest(
                full_name="Login Test",
                username=username,
                email=email,
                password=password,
            )
        )

        # 1. Login with email
        login_resp_email = auth_service.login(
            UserLoginRequest(email=email, password=password)
        )
        assert login_resp_email.user.email == email
        assert login_resp_email.tokens.access_token is not None

        # 2. Login with username
        login_resp_user = auth_service.login(
            UserLoginRequest(username=username, password=password)
        )
        assert login_resp_user.user.username == username

        # 3. Login with wrong password
        with pytest.raises(InvalidCredentialsError):
            auth_service.login(
                UserLoginRequest(email=email, password="WrongPassword999!")
            )

        # 4. Login non-existent user
        with pytest.raises(InvalidCredentialsError):
            auth_service.login(
                UserLoginRequest(email="nonexistent@example.com", password=password)
            )

        # 5. Token inspection / get_current_user
        current_user = auth_service.get_current_user(login_resp_email.tokens.access_token)
        assert current_user.email == email

        # 6. Refresh token
        refreshed = auth_service.refresh_token(
            RefreshTokenRequest(refresh_token=login_resp_email.tokens.refresh_token)
        )
        assert refreshed.access_token is not None

        # 7. Logout
        logout_success = auth_service.logout(login_resp_email.tokens.refresh_token)
        assert logout_success is True

        # 8. Refresh after logout fails
        with pytest.raises(SessionRevokedOrExpiredError):
            auth_service.refresh_token(
                RefreshTokenRequest(refresh_token=login_resp_email.tokens.refresh_token)
            )

    finally:
        cleanup_user(db, email)


def test_auth_service_change_password(db: Session, auth_service: AuthService):
    uid = uuid.uuid4().hex[:8]
    email = f"pwd_{uid}@example.com"
    username = f"pwd_{uid}"
    old_password = "OldPassword123!"
    new_password = "NewPassword456!"

    cleanup_user(db, email)

    try:
        reg = auth_service.register(
            UserRegisterRequest(
                full_name="Pwd Test",
                username=username,
                email=email,
                password=old_password,
            )
        )
        user_id = reg.user.user_id

        # Change password successfully
        res = auth_service.change_password(
            user_id,
            PasswordChangeRequest(
                current_password=old_password,
                new_password=new_password,
            ),
        )
        assert res is True

        # Old password can no longer log in
        with pytest.raises(InvalidCredentialsError):
            auth_service.login(UserLoginRequest(email=email, password=old_password))

        # New password can log in
        new_login = auth_service.login(UserLoginRequest(email=email, password=new_password))
        assert new_login.user.email == email

    finally:
        cleanup_user(db, email)


def test_fastapi_endpoints_integration(client: TestClient, db: Session):
    uid = uuid.uuid4().hex[:8]
    email = f"api_{uid}@example.com"
    username = f"api_{uid}"
    password = "ApiPassword123!"

    cleanup_user(db, email)

    try:
        # 1. POST /api/v1/auth/register
        reg_response = client.post(
            "/api/v1/auth/register",
            json={
                "full_name": "API Tester",
                "username": username,
                "email": email,
                "password": password,
            },
        )
        assert reg_response.status_code == 201
        reg_data = reg_response.json()
        assert reg_data["user"]["email"] == email
        access_token = reg_data["tokens"]["access_token"]
        refresh_token = reg_data["tokens"]["refresh_token"]

        # 2. GET /api/v1/auth/me
        me_response = client.get(
            "/api/v1/auth/me",
            headers={"Authorization": f"Bearer {access_token}"},
        )
        assert me_response.status_code == 200
        assert me_response.json()["email"] == email

        # 3. POST /api/v1/auth/login
        login_response = client.post(
            "/api/v1/auth/login",
            json={
                "email": email,
                "password": password,
            },
        )
        assert login_response.status_code == 200
        new_tokens = login_response.json()["tokens"]

        # 4. POST /api/v1/auth/refresh
        refresh_resp = client.post(
            "/api/v1/auth/refresh",
            json={"refresh_token": new_tokens["refresh_token"]},
        )
        assert refresh_resp.status_code == 200
        assert "access_token" in refresh_resp.json()

        # 5. POST /api/v1/auth/logout
        logout_resp = client.post(
            "/api/v1/auth/logout",
            json={"refresh_token": new_tokens["refresh_token"]},
        )
        assert logout_resp.status_code == 200
        assert logout_resp.json()["session_revoked"] is True

        # 6. Verify revoked token rejected
        refresh_again = client.post(
            "/api/v1/auth/refresh",
            json={"refresh_token": new_tokens["refresh_token"]},
        )
        assert refresh_again.status_code == 401

    finally:
        cleanup_user(db, email)
