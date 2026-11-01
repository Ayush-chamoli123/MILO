"""
MILO Authentication Service
===========================
Coordinates authentication, registration, password validation, session tracking,
and token issuance. Acts as the orchestrator between API schemas, cryptographic
security policies, and database persistence (UserRepository).

Adheres to MILO Production Security Standards:
- AUTH-01: Valid registration and entity creation
- AUTH-02: Existing email duplicate rejection
- AUTH-03: Existing phone number duplicate rejection
- AUTH-04: Enforced non-null constraints & validations
- AUTH-08: Secure password hashing (BCrypt / Argon2)
- AUTH-10: Token hashing, session recording, and revocation checks
- AUTH-11: Soft-deletion handling
"""

from datetime import datetime, timedelta, timezone
from typing import Any
from sqlalchemy.orm import Session

try:
    from fastapi import HTTPException, status
    _BaseAuthException = HTTPException
except ImportError:
    class _BaseAuthException(Exception):  # type: ignore[no-redef]
        def __init__(self, status_code: int, detail: str) -> None:
            super().__init__(detail)
            self.status_code = status_code
            self.detail = detail

    class _Status:
        HTTP_400_BAD_REQUEST = 400
        HTTP_401_UNAUTHORIZED = 401
        HTTP_403_FORBIDDEN = 403
        HTTP_404_NOT_FOUND = 404
        HTTP_409_CONFLICT = 409
    status = _Status()  # type: ignore[assignment]

from app.core.config import settings
from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_password,
    hash_token,
    verify_password,
)
from app.models.entities import User, UserSession, UserStatus
from app.repositories.user_repository import UserRepository
from app.schemas.auth import (
    AuthResponse,
    PasswordChangeRequest,
    RefreshTokenRequest,
    TokenResponse,
    UserLoginRequest,
    UserRegisterRequest,
    UserResponse,
)


# ==============================================================================
# AUTHENTICATION EXCEPTIONS
# ==============================================================================

class AuthError(_BaseAuthException):
    """Base class for authentication exceptions mapping to HTTP status codes."""
    def __init__(self, status_code: int, detail: str) -> None:
        super().__init__(status_code=status_code, detail=detail)


class EmailAlreadyExistsError(AuthError):
    def __init__(self, email: str) -> None:
        super().__init__(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"An account with email '{email}' already exists.",
        )


class UsernameAlreadyExistsError(AuthError):
    def __init__(self, username: str) -> None:
        super().__init__(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Username '{username}' is already taken.",
        )


class PhoneNumberAlreadyExistsError(AuthError):
    def __init__(self, phone_number: str) -> None:
        super().__init__(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Phone number '{phone_number}' is already registered.",
        )


class InvalidCredentialsError(AuthError):
    def __init__(self, detail: str = "Invalid email or password.") -> None:
        super().__init__(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=detail,
        )


class AccountInactiveError(AuthError):
    def __init__(self, detail: str = "User account is inactive or has been deleted.") -> None:
        super().__init__(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=detail,
        )


class AccountSuspendedError(AuthError):
    def __init__(self, detail: str = "User account has been suspended.") -> None:
        super().__init__(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=detail,
        )


class InvalidTokenError(AuthError):
    def __init__(self, detail: str = "Could not validate authentication token.") -> None:
        super().__init__(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=detail,
        )


class SessionRevokedOrExpiredError(AuthError):
    def __init__(self, detail: str = "Session has expired or was revoked upon logout.") -> None:
        super().__init__(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=detail,
        )


# ==============================================================================
# AUTHENTICATION SERVICE IMPLEMENTATION
# ==============================================================================

class AuthService:
    """Coordinates business security workflows, token generation, and persistence."""

    def __init__(self, db: Session, user_repo: UserRepository | None = None) -> None:
        self.db = db
        self.user_repo = user_repo or UserRepository(db)

    # --------------------------------------------------------------------------
    # REGISTRATION WORKFLOW
    # --------------------------------------------------------------------------

    def register(self, request: UserRegisterRequest) -> AuthResponse:
        """
        Coordinates new user onboarding:
        1. Checks uniqueness of email, username, and optional phone (AUTH-02, AUTH-03).
        2. Generates salted cryptographic hash of password (AUTH-08).
        3. Instantiates User domain model (excluding password from response).
        4. Persists user via UserRepository.
        5. Issues access and refresh tokens, registering an active UserSession (AUTH-10).
        """
        # 1. Uniqueness Validations
        if self.user_repo.exists_by_email(request.email):
            raise EmailAlreadyExistsError(request.email)

        if self.user_repo.exists_by_username(request.username):
            raise UsernameAlreadyExistsError(request.username)

        if request.phone_number and self.user_repo.exists_by_phone(request.phone_number):
            raise PhoneNumberAlreadyExistsError(request.phone_number)

        # 2. Hash Password (AUTH-08)
        hashed_password = hash_password(request.password)

        # 3. Create User Entity
        new_user = User(
            full_name=request.full_name,
            username=request.username,
            email=request.email,
            phone_number=request.phone_number,
            password_hash=hashed_password,
            profile_picture=request.profile_picture,
            age=request.age,
            gender=request.gender,
            city=request.city,
            languages=request.languages,
            profile_visibility=request.profile_visibility,
            status=UserStatus.ACTIVE,
            is_email_verified=False,
            is_phone_verified=False,
        )

        # 4. Save to Database
        persisted_user = self.user_repo.create(new_user, auto_commit=True)

        # 5. Issue Tokens & Register Session (AUTH-10)
        tokens = self._issue_session_tokens(
            user=persisted_user,
            device_name="Web / Mobile App",
            ip_address=None,
        )

        return AuthResponse(
            user=UserResponse.model_validate(persisted_user),
            tokens=tokens,
        )

    # --------------------------------------------------------------------------
    # LOGIN WORKFLOW
    # --------------------------------------------------------------------------

    def login(self, request: UserLoginRequest) -> AuthResponse:
        """
        Authenticates an existing user:
        1. Finds user by email, username, or unified email_or_username identifier.
        2. Verifies account is active, non-suspended, and not soft-deleted.
        3. Verifies plaintext password against stored hash.
        4. Issues new JWT access and refresh tokens.
        5. Records login session in `user_sessions` table.
        """
        # 1. Resolve User
        user: User | None = None
        if request.email_or_username:
            user = self.user_repo.get_by_email_or_username(request.email_or_username)
        elif request.email:
            user = self.user_repo.get_by_email(request.email)
        elif request.username:
            user = self.user_repo.get_by_username(request.username)
        else:
            raise InvalidCredentialsError("Email or username is required for login.")

        if not user:
            raise InvalidCredentialsError("Invalid credentials.")

        # 2. Check Account Status
        if user.status == UserStatus.SUSPENDED:
            raise AccountSuspendedError("Account has been suspended.")

        if user.status in (UserStatus.INACTIVE, UserStatus.DELETED) or user.deleted_at is not None:
            raise AccountInactiveError("Account is inactive or has been deleted.")

        # 3. Verify Password (AUTH-08)
        if not verify_password(request.password, user.password_hash):
            raise InvalidCredentialsError("Invalid credentials.")

        # 4. Issue Tokens and Register Session (AUTH-10)
        tokens = self._issue_session_tokens(
            user=user,
            device_name=request.device_name,
            ip_address=request.ip_address,
        )

        return AuthResponse(
            user=UserResponse.model_validate(user),
            tokens=tokens,
        )

    # --------------------------------------------------------------------------
    # TOKEN REFRESH WORKFLOW (AUTH-10)
    # --------------------------------------------------------------------------

    def refresh_token(self, request: RefreshTokenRequest) -> TokenResponse:
        """
        Exchanges a valid refresh token for a newly issued access token.
        Validates token signature and checks database session revocation state.
        """
        try:
            payload = decode_token(request.refresh_token)
        except Exception:
            raise InvalidTokenError("Invalid or expired refresh token.")

        if payload.get("type") != "refresh":
            raise InvalidTokenError("Supplied token is not a refresh token.")

        user_id: str | None = payload.get("sub")
        if not user_id:
            raise InvalidTokenError("Missing subject in refresh token.")

        # Verify against active session record in database (AUTH-10)
        token_hash = hash_token(request.refresh_token)
        session = self.user_repo.get_active_session(token_hash)
        if not session:
            raise SessionRevokedOrExpiredError(
                "Refresh token session is expired or revoked. Please log in again."
            )

        # Verify user account is still valid
        user = self.user_repo.get_by_id(user_id)
        if not user or user.status != UserStatus.ACTIVE:
            raise AccountInactiveError("Account associated with this session is no longer active.")

        # Issue newly refreshed access token
        access_token = create_access_token(
            subject=user.user_id,
            extra_claims={"email": user.email, "username": user.username},
        )

        return TokenResponse(
            access_token=access_token,
            refresh_token=request.refresh_token,
            token_type="bearer",
            expires_in=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        )

    # --------------------------------------------------------------------------
    # LOGOUT WORKFLOW (AUTH-10)
    # --------------------------------------------------------------------------

    def logout(self, refresh_token: str) -> bool:
        """
        Revokes the active user session associated with the provided refresh token.
        Prevents subsequent use of this token (AUTH-10).
        """
        token_hash = hash_token(refresh_token)
        session = self.user_repo.get_session_by_token_hash(token_hash)
        if session:
            return self.user_repo.revoke_session(session.session_id)
        return False

    def logout_all_devices(self, user_id: str) -> int:
        """
        Revokes all active sessions for a user across all devices.
        Returns the number of sessions revoked.
        """
        return self.user_repo.revoke_all_user_sessions(user_id)

    # --------------------------------------------------------------------------
    # PASSWORD UPDATE WORKFLOW
    # --------------------------------------------------------------------------

    def change_password(self, user_id: str, request: PasswordChangeRequest) -> bool:
        """
        Safely changes user password and invalidates all prior sessions.
        """
        user = self.user_repo.get_by_id(user_id)
        if not user:
            raise InvalidCredentialsError("User not found.")

        if not verify_password(request.current_password, user.password_hash):
            raise InvalidCredentialsError("Current password incorrect.")

        user.password_hash = hash_password(request.new_password)
        self.user_repo.update(user, auto_commit=True)

        # Invalidate all prior sessions on password change
        self.user_repo.revoke_all_user_sessions(user_id, auto_commit=True)
        return True

    # --------------------------------------------------------------------------
    # TOKEN INSPECTION & CURRENT USER RESOLUTION
    # --------------------------------------------------------------------------

    def get_current_user(self, access_token: str) -> User:
        """
        Decodes access token and resolves active user from database.
        """
        try:
            payload = decode_token(access_token)
        except Exception:
            raise InvalidTokenError("Invalid access token.")

        if payload.get("type") != "access":
            raise InvalidTokenError("Expected access token.")

        user_id: str | None = payload.get("sub")
        if not user_id:
            raise InvalidTokenError("Token contains no user identifier.")

        user = self.user_repo.get_by_id(user_id)
        if not user or user.status != UserStatus.ACTIVE:
            raise AccountInactiveError("User account not found or inactive.")

        return user

    # --------------------------------------------------------------------------
    # INTERNAL HELPERS
    # --------------------------------------------------------------------------

    def _issue_session_tokens(
        self,
        user: User,
        device_name: str | None,
        ip_address: str | None,
    ) -> TokenResponse:
        """Issues JWT token pair and records the session hash into the database (AUTH-10)."""
        # 1. Create Access Token
        access_token = create_access_token(
            subject=user.user_id,
            extra_claims={"email": user.email, "username": user.username},
        )

        # 2. Create Refresh Token
        refresh_token = create_refresh_token(subject=user.user_id)

        # 3. Compute Token Hash for Session Table
        token_hash = hash_token(refresh_token)
        expires_at = datetime.now(timezone.utc) + timedelta(
            days=settings.REFRESH_TOKEN_EXPIRE_DAYS
        )

        # 4. Persist UserSession
        session_record = UserSession(
            user_id=user.user_id,
            refresh_token_hash=token_hash,
            device_name=device_name,
            ip_address=ip_address,
            expires_at=expires_at,
            is_revoked=False,
        )
        self.user_repo.create_session(session_record, auto_commit=True)

        return TokenResponse(
            access_token=access_token,
            refresh_token=refresh_token,
            token_type="bearer",
            expires_in=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        )
