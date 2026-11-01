"""
MILO Authentication Schemas
===========================
Pydantic v2 request and response schemas for user registration, authentication,
token refresh, and user profile serialization.
Strictly excludes sensitive attributes such as `password_hash`.
"""

from datetime import datetime
import re
from typing import Any
from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.models.entities import ProfileVisibility, UserStatus

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
    password: str = Field(
        ...,
        min_length=1,
        max_length=128,
        description="Plaintext password",
        examples=["SuperSecret123!"],
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


class TokenPayload(BaseModel):
    """Decoded JWT payload data model."""
    sub: str
    exp: int | None = None
    iat: int | None = None
    type: str | None = None


# Backward-compatible aliases
UserCreate = UserRegisterRequest
RegisterRequest = UserRegisterRequest
LoginRequest = UserLoginRequest
UserRead = UserResponse
UserOut = UserResponse
