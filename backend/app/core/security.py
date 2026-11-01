"""
MILO Cryptographic Security & Token Management
==============================================
Provides password hashing (BCrypt & Argon2) and JWT token generation/validation.
Adheres to AUTH-08 (secure salted password hashes) and AUTH-10 (session token hashing).
"""

from datetime import datetime, timedelta, timezone
import hashlib
from typing import Any
from uuid import uuid4
import bcrypt
import jwt

from app.core.config import settings

try:
    import argon2
    _argon2_hasher = argon2.PasswordHasher()
except ImportError:
    _argon2_hasher = None


def hash_password(password: str) -> str:
    """
    Hashes a plaintext password using BCrypt with automatic salting.
    Returns the UTF-8 encoded string format suitable for database persistence.
    """
    salt = bcrypt.gensalt(rounds=12)
    return bcrypt.hashpw(password.encode("utf-8"), salt).decode("utf-8")


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """
    Verifies a plaintext password against a stored hash.
    Transparently supports both BCrypt ($2b$, $2a$, $2y$) and Argon2 ($argon2id$).
    """
    try:
        if hashed_password.startswith("$argon2"):
            if _argon2_hasher is not None:
                return _argon2_hasher.verify(hashed_password, plain_password)
            return False
        return bcrypt.checkpw(
            plain_password.encode("utf-8"),
            hashed_password.encode("utf-8"),
        )
    except Exception:
        return False


def create_access_token(
    subject: str | Any,
    expires_delta: timedelta | None = None,
    extra_claims: dict[str, Any] | None = None,
) -> str:
    """Generates a signed JWT access token for user authentication."""
    now = datetime.now(timezone.utc)
    if expires_delta:
        expire = now + expires_delta
    else:
        expire = now + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)

    payload: dict[str, Any] = {
        "sub": str(subject),
        "exp": expire,
        "iat": now,
        "type": "access",
    }
    if extra_claims:
        payload.update(extra_claims)

    return jwt.encode(payload, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)


def create_refresh_token(
    subject: str | Any,
    expires_delta: timedelta | None = None,
    extra_claims: dict[str, Any] | None = None,
) -> str:
    """Generates a signed JWT refresh token for session lifecycle management."""
    now = datetime.now(timezone.utc)
    if expires_delta:
        expire = now + expires_delta
    else:
        expire = now + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)

    payload: dict[str, Any] = {
        "sub": str(subject),
        "jti": str(uuid4()),
        "exp": expire,
        "iat": now,
        "type": "refresh",
    }
    if extra_claims:
        payload.update(extra_claims)

    return jwt.encode(payload, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)


def decode_token(token: str) -> dict[str, Any]:
    """
    Decodes and cryptographically verifies a JWT token.
    Raises jwt.PyJWTError (ExpiredSignatureError, InvalidTokenError) on failure.
    """
    return jwt.decode(
        token,
        settings.JWT_SECRET_KEY,
        algorithms=[settings.JWT_ALGORITHM],
    )


def hash_token(token: str) -> str:
    """
    Computes a cryptographic SHA-256 digest of a refresh token.
    Used for safe storage in the `user_sessions.refresh_token_hash` table (AUTH-10).
    """
    return hashlib.sha256(token.encode("utf-8")).hexdigest()
