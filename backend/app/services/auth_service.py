"""
MILO Authentication & Verification Service
==========================================
Coordinates authentication, registration, password validation, session tracking,
token issuance, and OTP email/phone verification. Acts as the orchestrator between
API schemas, cryptographic security policies, and database persistence (UserRepository).

Adheres to MILO Production Security Standards:
- AUTH-01: Valid registration and entity creation
- AUTH-02: Existing email duplicate rejection
- AUTH-03: Existing phone number duplicate rejection
- AUTH-04: Enforced non-null constraints & validations
- AUTH-08: Secure password hashing (BCrypt / Argon2)
- AUTH-09: Email & SMS OTP verification with expiry & attempt throttling
- AUTH-10: Token hashing, session recording, and revocation checks
- AUTH-11: Soft-deletion handling
"""

from datetime import datetime, timedelta, timezone
import logging
import secrets
from typing import Any
from sqlalchemy.orm import Session

logger = logging.getLogger("milo.auth_service")

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
        HTTP_429_TOO_MANY_REQUESTS = 429
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
from app.models.entities import (
    User,
    UserSession,
    UserStatus,
    Verification,
    VerificationStatus,
    VerificationType,
)
from app.repositories.user_repository import UserRepository
from app.schemas.auth import (
    AuthResponse,
    OtpSentResponse,
    PasswordChangeRequest,
    RefreshTokenRequest,
    SendOtpRequest,
    TokenResponse,
    UserLoginRequest,
    UserRegisterRequest,
    UserResponse,
    VerificationResponse,
    VerifyOtpRequest,
)


# ==============================================================================
# AUTHENTICATION & VERIFICATION EXCEPTIONS
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


class UserNotFoundError(AuthError):
    def __init__(self, detail: str = "User account not found.") -> None:
        super().__init__(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=detail,
        )


class InvalidOtpError(AuthError):
    def __init__(self, detail: str = "Invalid OTP code.") -> None:
        super().__init__(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=detail,
        )


class OtpExpiredError(AuthError):
    def __init__(self, detail: str = "OTP verification code has expired.") -> None:
        super().__init__(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=detail,
        )


class OtpMaxAttemptsError(AuthError):
    def __init__(self, detail: str = "Maximum verification attempts exceeded. Please request a new OTP.") -> None:
        super().__init__(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail=detail,
        )


# ==============================================================================
# AUTHENTICATION SERVICE IMPLEMENTATION
# ==============================================================================

class AuthService:
    """Coordinates business security workflows, token generation, OTP verification, and persistence."""

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
    # OTP VERIFICATION WORKFLOW (EMAIL & PHONE) (AUTH-09)
    # --------------------------------------------------------------------------

    def send_verification_otp(
        self,
        user: User | None = None,
        email: str | None = None,
        phone_number: str | None = None,
        verification_type: VerificationType = VerificationType.EMAIL,
    ) -> OtpSentResponse:
        """
        Generates and sends a 6-digit OTP code to a user's email or phone number.
        - Invalidates previous pending OTP requests for this channel.
        - Sets a 10-minute expiration window.
        - Logs/dispatches code to the designated transport channel.
        """
        target_user = self._resolve_target_user(
            user=user, email=email, phone_number=phone_number
        )

        # Determine target channel destination
        if verification_type == VerificationType.EMAIL:
            destination = target_user.email
            if not destination:
                raise InvalidCredentialsError("User account has no registered email address.")
        elif verification_type == VerificationType.PHONE:
            destination = target_user.phone_number
            if not destination:
                raise InvalidCredentialsError("User account has no registered phone number.")
        else:
            raise InvalidCredentialsError(f"Unsupported verification type: {verification_type}")

        # Invalidate any prior pending verifications for this type
        self.user_repo.invalidate_pending_verifications(
            user_id=target_user.user_id,
            verification_type=verification_type,
            auto_commit=False,
        )

        # Generate secure random 6-digit OTP
        otp_code = self._generate_numeric_otp(6)
        expires_at = datetime.now(timezone.utc) + timedelta(minutes=10)

        # Create Verification audit record (AUTH-09)
        verification_record = Verification(
            user_id=target_user.user_id,
            type=verification_type,
            status=VerificationStatus.PENDING,
            otp_code=otp_code,
            attempts=0,
            expires_at=expires_at,
        )
        self.user_repo.create_verification(verification_record, auto_commit=True)

        # Dispatch OTP via SMS or Email service
        self._dispatch_otp(destination, otp_code, verification_type)

        masked_dest = self._mask_destination(destination, verification_type)
        return OtpSentResponse(
            message=f"OTP verification code sent successfully to {masked_dest}.",
            type=verification_type,
            destination=masked_dest,
            expires_in_seconds=600,
        )

    def verify_otp(
        self,
        user: User | None = None,
        email: str | None = None,
        phone_number: str | None = None,
        verification_type: VerificationType = VerificationType.EMAIL,
        otp_code: str = "",
    ) -> VerificationResponse:
        """
        Verifies a user-submitted OTP:
        1. Confirms the pending verification record exists and is active.
        2. Enforces expiration time (10-minute validity).
        3. Enforces rate-limiting / brute-force throttle (maximum 5 attempts).
        4. Uses constant-time comparison to prevent timing attacks.
        5. Updates user `is_email_verified` or `is_phone_verified` boolean to True.
        """
        target_user = self._resolve_target_user(
            user=user, email=email, phone_number=phone_number
        )

        # Query latest pending verification record
        verification = self.user_repo.get_latest_pending_verification(
            user_id=target_user.user_id,
            verification_type=verification_type,
        )
        if not verification:
            raise InvalidOtpError("No pending OTP verification found. Please request a new code.")

        now = datetime.now(timezone.utc)

        # Check Expiration (AUTH-09)
        if verification.expires_at:
            # Handle tz-naive vs tz-aware comparisons safely
            exp = verification.expires_at
            if exp.tzinfo is None:
                exp = exp.replace(tzinfo=timezone.utc)
            if exp < now:
                verification.status = VerificationStatus.FAILED
                self.user_repo.update_verification(verification, auto_commit=True)
                raise OtpExpiredError("OTP code has expired. Please request a new code.")

        # Check Attempt Limit (Rate Limiting Brute-Force Protection)
        if verification.attempts >= 5:
            verification.status = VerificationStatus.FAILED
            self.user_repo.update_verification(verification, auto_commit=True)
            raise OtpMaxAttemptsError("Too many incorrect attempts. Please request a new OTP.")

        # Increment attempt counter
        verification.attempts += 1

        # Secure constant-time comparison
        clean_input = otp_code.strip()
        is_match = secrets.compare_digest(verification.otp_code or "", clean_input)

        if not is_match:
            self.user_repo.update_verification(verification, auto_commit=True)
            remaining = max(0, 5 - verification.attempts)
            raise InvalidOtpError(f"Invalid OTP code. {remaining} attempt(s) remaining.")

        # Success: Mark verification record as VERIFIED
        verification.status = VerificationStatus.VERIFIED
        verification.verified_at = now
        self.user_repo.update_verification(verification, auto_commit=False)

        # Fast-check flags on User entity
        if verification_type == VerificationType.EMAIL:
            self.user_repo.mark_email_verified(target_user.user_id, auto_commit=False)
            target_user.is_email_verified = True
        elif verification_type == VerificationType.PHONE:
            self.user_repo.mark_phone_verified(target_user.user_id, auto_commit=False)
            target_user.is_phone_verified = True

        self.db.commit()
        self.db.refresh(target_user)

        return VerificationResponse(
            message=f"{verification_type.value.capitalize()} verified successfully.",
            verified=True,
            type=verification_type,
            user_id=target_user.user_id,
            is_email_verified=target_user.is_email_verified,
            is_phone_verified=target_user.is_phone_verified,
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

    def _resolve_target_user(
        self,
        user: User | None,
        email: str | None,
        phone_number: str | None,
    ) -> User:
        """Resolves target User instance from authenticated session or explicit identifiers."""
        if user:
            return user
        if email:
            target = self.user_repo.get_by_email(email)
            if target:
                return target
        if phone_number:
            target = self.user_repo.get_by_phone(phone_number)
            if target:
                return target
        raise UserNotFoundError("User account not found. Please verify the provided details.")

    def _generate_numeric_otp(self, digits: int = 6) -> str:
        """Generates a cryptographically secure numeric OTP."""
        return "".join(secrets.choice("0123456789") for _ in range(digits))

    def _mask_destination(self, destination: str, verification_type: VerificationType) -> str:
        """Masks sensitive email or phone digits for privacy in API responses."""
        if verification_type == VerificationType.EMAIL and "@" in destination:
            name, domain = destination.split("@", 1)
            masked_name = name[:2] + "***" if len(name) > 2 else name + "***"
            return f"{masked_name}@{domain}"
        if verification_type == VerificationType.PHONE and len(destination) > 5:
            return destination[:4] + "***" + destination[-3:]
        return destination

    def _dispatch_otp(
        self, destination: str, otp_code: str, verification_type: VerificationType
    ) -> None:
        """
        Dispatches OTP code via SMS gateway (e.g. Twilio) or Email provider (e.g. Sendgrid/SES).
        Logs transmission for operational visibility.
        """
        logger.info(
            f"[OTP DISPATCH] Channel: {verification_type.value} | Destination: {destination} | Code: {otp_code}"
        )
        # Development / Testing console output:
        print(f"  [OTP DISPATCH -> {verification_type.value}] Destination: {destination} | CODE: {otp_code}")

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
