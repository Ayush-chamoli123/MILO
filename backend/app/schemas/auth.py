"""
MILO Authentication & Verification Schemas
==========================================
Pydantic v2 request and response schemas for user registration, authentication,
token refresh, OTP email/phone verification, and user profile serialization.
Strictly excludes sensitive attributes such as `password_hash`.
"""

from datetime import datetime
import re
from typing import Any
from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.models.entities import ProfileVisibility, UserStatus, VerificationType, VerificationStatus

# Database constraint regex compliant with ck_users_email_format in models/entities.py
EMAIL_PATTERN = r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"


# ==============================================================================
# REQUEST SCHEMAS
# ==============================================================================

class UserRegisterRequest(BaseModel):
    """Payload for registering a new user account."""
    full_name: str = Field(
        ...,
        min_length=1,
        max_length=150,
        description="Full legal or display name",
        examples=["Alice Wonderland"],
    )
    username: str = Field(
        ...,
        min_length=3,
        max_length=100,
        pattern=r"^[A-Za-z0-9_.-]+$",
        description="Unique username handle containing letters, numbers, underscores, dots, or dashes",
        examples=["alice_w"],
    )
    email: str = Field(
        ...,
        max_length=150,
        description="Valid and unique user email address",
        examples=["alice@example.com"],
    )
    password: str = Field(
        ...,
        min_length=8,
        max_length=128,
        description="Plaintext password (minimum 8 characters)",
        examples=["SuperSecret123!"],
    )
    phone_number: str | None = Field(
        default=None,
        max_length=20,
        description="Optional unique phone number for SMS/OTP verification (AUTH-03)",
        examples=["+15551234567"],
    )
    profile_picture: str | None = Field(
        default=None,
        max_length=500,
        description="URL pointing to user's profile avatar",
    )
    age: int | None = Field(
        default=None,
        ge=13,
        le=120,
        description="User age in years (must be at least 13)",
    )
    gender: str | None = Field(
        default=None,
        max_length=50,
        description="Self-identified gender",
    )
    city: str | None = Field(
        default=None,
        max_length=100,
        description="Primary residential city",
    )
    languages: list[str] | None = Field(
        default=None,
        description="List of spoken languages",
        examples=[["English", "Spanish"]],
    )
    profile_visibility: ProfileVisibility = Field(
        default=ProfileVisibility.PUBLIC,
        description="Profile visibility level (PUBLIC, PRIVATE, FRIENDS_ONLY)",
    )

    @field_validator("email")
    @classmethod
    def validate_and_normalize_email(cls, v: str) -> str:
        cleaned = v.strip().lower()
        if not re.match(EMAIL_PATTERN, cleaned):
            raise ValueError("Invalid email format (must match standard name@domain.tld)")
        return cleaned

    @field_validator("username")
    @classmethod
    def clean_username(cls, v: str) -> str:
        return v.strip()


class UserLoginRequest(BaseModel):
    """
    Payload for authenticating an existing user.
    Supports login via email, username, or unified `email_or_username`.
    Also supports passwordless login via OTP.
    """
    email: str | None = Field(
        default=None,
        description="Email address of the account",
        examples=["alice@example.com"],
    )
    username: str | None = Field(
        default=None,
        description="Handle or username of the account",
        examples=["alice_w"],
    )
    email_or_username: str | None = Field(
        default=None,
        description="Unified identifier (accepts either email or handle)",
        examples=["alice@example.com"],
    )
    password: str | None = Field(
        default=None,
        max_length=128,
        description="Plaintext password (optional if login_with_otp is True)",
        examples=["SuperSecret123!"],
    )
    login_with_otp: bool = Field(
        default=False,
        description="If True, requests a 6-digit OTP to be dispatched for sign-in instead of password verification",
    )
    device_name: str | None = Field(
        default=None,
        max_length=100,
        description="Optional client device descriptor (e.g., Pixel 8 Pro / Android 14)",
    )
    ip_address: str | None = Field(
        default=None,
        max_length=50,
        description="Client network IP address",
    )

    @field_validator("email")
    @classmethod
    def normalize_optional_email(cls, v: str | None) -> str | None:
        if v is not None:
            cleaned = v.strip().lower()
            if not re.match(EMAIL_PATTERN, cleaned):
                raise ValueError("Invalid email format")
            return cleaned
        return None

    @model_validator(mode="after")
    def check_password_or_otp(self) -> "UserLoginRequest":
        if not self.login_with_otp and not self.password:
            raise ValueError("Password is required unless login_with_otp is specified.")
        return self


class RefreshTokenRequest(BaseModel):
    """Payload to exchange a refresh token for a new access token."""
    refresh_token: str = Field(
        ...,
        description="Valid active refresh token",
    )


class PasswordChangeRequest(BaseModel):
    """Payload for authenticated users to update their password."""
    current_password: str = Field(
        ...,
        min_length=1,
        description="Current account password for confirmation",
    )
    new_password: str = Field(
        ...,
        min_length=8,
        max_length=128,
        description="New password (minimum 8 characters)",
    )


class SendOtpRequest(BaseModel):
    """
    Request to generate and send an OTP code to email or mobile phone.
    Works for both authenticated users and public registration flows.
    """
    type: VerificationType = Field(
        default=VerificationType.EMAIL,
        description="Verification channel: EMAIL or PHONE",
        examples=["EMAIL"],
    )
    email: str | None = Field(
        default=None,
        description="Target email address (required if user is unauthenticated)",
        examples=["alice@example.com"],
    )
    phone_number: str | None = Field(
        default=None,
        description="Target phone number (required if verifying phone while unauthenticated)",
        examples=["+15551234567"],
    )

    @field_validator("email")
    @classmethod
    def normalize_email(cls, v: str | None) -> str | None:
        if v is not None:
            return v.strip().lower()
        return None


class VerifyOtpRequest(BaseModel):
    """
    Request to submit and validate a 6-digit OTP code for email or mobile.
    """
    type: VerificationType = Field(
        default=VerificationType.EMAIL,
        description="Verification channel: EMAIL or PHONE",
        examples=["EMAIL"],
    )
    otp_code: str = Field(
        ...,
        min_length=4,
        max_length=10,
        description="4 to 6 digit numeric OTP verification code",
        examples=["492810"],
    )
    email: str | None = Field(
        default=None,
        description="Target email address (if unauthenticated)",
        examples=["alice@example.com"],
    )
    phone_number: str | None = Field(
        default=None,
        description="Target phone number (if unauthenticated)",
        examples=["+15551234567"],
    )
    device_name: str | None = Field(
        default=None,
        description="Device name for issuing session tokens upon successful verification",
    )

    @field_validator("otp_code")
    @classmethod
    def clean_otp(cls, v: str) -> str:
        return v.strip()


# ==============================================================================
# RESPONSE SCHEMAS (Notice: NEVER includes password_hash)
# ==============================================================================

class UserResponse(BaseModel):
    """
    Public / Authenticated user representation.
    Ensures `password_hash` is never exposed.
    """
    user_id: str
    full_name: str
    username: str
    email: str
    phone_number: str | None = None
    profile_picture: str | None = None
    age: int | None = None
    gender: str | None = None
    city: str | None = None
    languages: list[str] | None = None
    profile_visibility: ProfileVisibility = ProfileVisibility.PUBLIC
    status: UserStatus = UserStatus.ACTIVE
    is_email_verified: bool = False
    is_phone_verified: bool = False
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class TokenResponse(BaseModel):
    """Bearer token pair and expiry metadata."""
    access_token: str
    refresh_token: str | None = None
    token_type: str = "bearer"
    expires_in: int = Field(
        ...,
        description="Access token lifespan in seconds",
    )


class AuthResponse(BaseModel):
    """Unified payload returned upon successful registration or login."""
    user: UserResponse
    tokens: TokenResponse
    requires_otp: bool = Field(
        default=False,
        description="Indicates whether OTP verification is required before full session access",
    )
    otp_channel: VerificationType | None = Field(
        default=None,
        description="Verification channel: EMAIL or PHONE",
    )
    masked_destination: str | None = Field(
        default=None,
        description="Masked target address where OTP was dispatched",
    )
    debug_otp: str | None = Field(
        default=None,
        description="Development preview of OTP code for testing",
    )


class TokenPayload(BaseModel):
    """Decoded JWT payload data model."""
    sub: str
    exp: int | None = None
    iat: int | None = None
    type: str | None = None


class OtpSentResponse(BaseModel):
    """Confirmation payload returned when an OTP is dispatched."""
    message: str
    type: VerificationType
    destination: str = Field(
        ...,
        description="Masked destination address (e.g. al***@example.com or +1555***4567)",
    )
    expires_in_seconds: int = Field(
        default=600,
        description="OTP validity period in seconds (default: 10 minutes)",
    )
    debug_otp: str | None = Field(
        default=None,
        description="Development preview of OTP code (only in non-production / simulation)",
    )


class VerificationResponse(BaseModel):
    """Confirmation payload returned upon successful OTP verification."""
    message: str
    verified: bool
    type: VerificationType
    user_id: str
    is_email_verified: bool
    is_phone_verified: bool
    user: UserResponse | None = Field(
        default=None,
        description="User profile if authenticated via OTP",
    )
    tokens: TokenResponse | None = Field(
        default=None,
        description="JWT session tokens if issued upon verification",
    )


# Backward-compatible aliases
UserCreate = UserRegisterRequest
RegisterRequest = UserRegisterRequest
LoginRequest = UserLoginRequest
UserRead = UserResponse
UserOut = UserResponse
