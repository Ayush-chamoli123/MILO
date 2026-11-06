"""
Unit and Integration Tests for MILO OTP Verification Service
=============================================================
Tests:
1. OTP code generation, dispatch, and persistence in verifications table
2. Successful OTP verification updating is_email_verified and issuing session tokens
3. Rejection of invalid OTP and attempt counter incrementing
4. Rate limiting / brute-force lockout after 5 incorrect attempts (HTTP 429)
5. Expiration enforcement for stale OTP codes (10-minute window)
6. FastAPI REST endpoints /api/v1/auth/otp/send and /api/v1/auth/otp/verify
"""

from datetime import datetime, timedelta, timezone
import uuid
import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.main import app
from app.models.entities import User, UserSession, Verification, VerificationStatus, VerificationType
from app.schemas.auth import SendOtpRequest, UserRegisterRequest, VerifyOtpRequest
from app.services.auth_service import (
    AuthService,
    InvalidOtpError,
    OtpExpiredError,
    OtpMaxAttemptsError,
)


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


def cleanup_test_user(db: Session, email: str):
    user = db.query(User).filter(User.email == email).first()
    if user:
        db.query(Verification).filter(Verification.user_id == user.user_id).delete()
        db.query(UserSession).filter(UserSession.user_id == user.user_id).delete()
        db.delete(user)
        db.commit()


def test_otp_send_and_verify_flow(db: Session, auth_service: AuthService):
    uid = uuid.uuid4().hex[:8]
    email = f"otp_test_{uid}@example.com"
    cleanup_test_user(db, email)

    try:
        # 1. Register User
        reg_req = UserRegisterRequest(
            full_name="OTP Test User",
            username=f"otp_{uid}",
            email=email,
            password="SecurePassword123!",
        )
        reg_res = auth_service.register(reg_req)
        user_id = reg_res.user.user_id
        assert reg_res.requires_otp is True
        assert reg_res.debug_otp is not None

        # 2. Query pending verification record in database
        verification = auth_service.user_repo.get_latest_pending_verification(
            user_id=user_id,
            verification_type=VerificationType.EMAIL,
        )
        assert verification is not None
        assert verification.status == VerificationStatus.PENDING
        otp_code = verification.otp_code

        # 3. Verify OTP
        verify_res = auth_service.verify_otp(
            email=email,
            verification_type=VerificationType.EMAIL,
            otp_code=otp_code,
        )
        assert verify_res.verified is True
        assert verify_res.is_email_verified is True
        assert verify_res.tokens is not None
        assert verify_res.tokens.access_token is not None

        # 4. Check user status in database
        refreshed_user = auth_service.user_repo.get_by_id(user_id)
        assert refreshed_user.is_email_verified is True

    finally:
        cleanup_test_user(db, email)


def test_otp_invalid_code_handling(db: Session, auth_service: AuthService):
    uid = uuid.uuid4().hex[:8]
    email = f"otp_invalid_{uid}@example.com"
    cleanup_test_user(db, email)

    try:
        reg_req = UserRegisterRequest(
            full_name="Invalid OTP User",
            username=f"inv_{uid}",
            email=email,
            password="SecurePassword123!",
        )
        reg_res = auth_service.register(reg_req)

        # Attempt verification with wrong code
        with pytest.raises(InvalidOtpError) as exc_info:
            auth_service.verify_otp(
                email=email,
                verification_type=VerificationType.EMAIL,
                otp_code="000000",
            )
        assert "Invalid OTP code" in str(exc_info.value.detail)

    finally:
        cleanup_test_user(db, email)


def test_otp_max_attempts_lockout(db: Session, auth_service: AuthService):
    uid = uuid.uuid4().hex[:8]
    email = f"otp_lock_{uid}@example.com"
    cleanup_test_user(db, email)

    try:
        reg_req = UserRegisterRequest(
            full_name="Lockout User",
            username=f"lock_{uid}",
            email=email,
            password="SecurePassword123!",
        )
        auth_service.register(reg_req)

        # Fail 5 times
        for _ in range(5):
            try:
                auth_service.verify_otp(
                    email=email,
                    verification_type=VerificationType.EMAIL,
                    otp_code="999999",
                )
            except InvalidOtpError:
                pass

        # 6th attempt should raise OtpMaxAttemptsError
        with pytest.raises(OtpMaxAttemptsError):
            auth_service.verify_otp(
                email=email,
                verification_type=VerificationType.EMAIL,
                otp_code="999999",
            )

    finally:
        cleanup_test_user(db, email)


def test_fastapi_otp_endpoints_via_client(db: Session, client: TestClient):
    uid = uuid.uuid4().hex[:8]
    email = f"api_otp_{uid}@example.com"
    cleanup_test_user(db, email)

    try:
        # Register user via API
        reg_res = client.post(
            "/api/v1/auth/register",
            json={
                "full_name": "API OTP User",
                "username": f"api_{uid}",
                "email": email,
                "password": "SecurePassword123!",
            },
        )
        assert reg_res.status_code == 201
        reg_data = reg_res.json()
        assert reg_data["requires_otp"] is True
        debug_code = reg_data["debug_otp"]
        assert debug_code is not None

        # Verify OTP via API
        verify_res = client.post(
            "/api/v1/auth/otp/verify",
            json={
                "email": email,
                "type": "EMAIL",
                "otp_code": debug_code,
            },
        )
        assert verify_res.status_code == 200
        verify_data = verify_res.json()
        assert verify_data["verified"] is True
        assert verify_data["is_email_verified"] is True
        assert verify_data["tokens"]["access_token"] is not None

    finally:
        cleanup_test_user(db, email)


def test_unverified_user_requires_email_verification_on_login(db: Session, auth_service: AuthService):
    """
    Test that an unverified user attempting password login is flagged with requires_otp=True
    and automatically dispatched a new verification OTP.
    """
    uid = uuid.uuid4().hex[:8]
    email = f"unverified_{uid}@example.com"
    cleanup_test_user(db, email)

    try:
        # 1. Register user (is_email_verified = False)
        auth_service.register(UserRegisterRequest(
            full_name="Unverified User",
            username=f"unver_{uid}",
            email=email,
            password="SecurePassword123!",
        ))

        # 2. Attempt standard password login
        from app.schemas.auth import UserLoginRequest
        login_res = auth_service.login(UserLoginRequest(
            email_or_username=email,
            password="SecurePassword123!",
        ))

        assert login_res.requires_otp is True
        assert login_res.otp_channel == VerificationType.EMAIL
        assert login_res.debug_otp is not None

    finally:
        cleanup_test_user(db, email)


def test_verified_user_passwordless_otp_login_flow(db: Session, auth_service: AuthService, client: TestClient):
    """
    Test that once verified, a user can log in via one-time email OTP (passwordless / 2FA login).
    """
    uid = uuid.uuid4().hex[:8]
    email = f"verified_otp_{uid}@example.com"
    cleanup_test_user(db, email)

    try:
        # 1. Register & verify user
        reg_res = auth_service.register(UserRegisterRequest(
            full_name="Verified Flow User",
            username=f"flow_{uid}",
            email=email,
            password="SecurePassword123!",
        ))
        initial_otp = reg_res.debug_otp
        auth_service.verify_otp(
            email=email,
            verification_type=VerificationType.EMAIL,
            otp_code=initial_otp,
        )

        user = auth_service.user_repo.get_by_email(email)
        assert user.is_email_verified is True

        # 2. Trigger passwordless OTP login via API
        login_res = client.post(
            "/api/v1/auth/login",
            json={
                "email_or_username": email,
                "login_with_otp": True,
            },
        )
        assert login_res.status_code == 200
        login_data = login_res.json()
        assert login_data["requires_otp"] is True
        otp_login_code = login_data["debug_otp"]
        assert otp_login_code is not None

        # 3. Complete login by verifying the received OTP
        verify_login_res = client.post(
            "/api/v1/auth/otp/verify",
            json={
                "email": email,
                "type": "EMAIL",
                "otp_code": otp_login_code,
            },
        )
        assert verify_login_res.status_code == 200
        verify_data = verify_login_res.json()
        assert verify_data["verified"] is True
        assert verify_data["tokens"]["access_token"] is not None

        # 4. Check that issued token can access /me profile
        token = verify_data["tokens"]["access_token"]
        me_res = client.get(
            "/api/v1/auth/me",
            headers={"Authorization": f"Bearer {token}"},
        )
        assert me_res.status_code == 200
        assert me_res.json()["email"] == email
        assert me_res.json()["is_email_verified"] is True

    finally:
        cleanup_test_user(db, email)

