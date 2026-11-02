"""
MILO Services Package
=====================
Business domain services orchestrating security, workflows, and persistence.
"""

from app.services.auth_service import (
    AuthService,
    AuthError,
    EmailAlreadyExistsError,
    UsernameAlreadyExistsError,
    PhoneNumberAlreadyExistsError,
    InvalidCredentialsError,
    AccountInactiveError,
    AccountSuspendedError,
    InvalidTokenError,
    SessionRevokedOrExpiredError,
    UserNotFoundError,
    InvalidOtpError,
    OtpExpiredError,
    OtpMaxAttemptsError,
)

__all__ = [
    "AuthService",
    "AuthError",
    "EmailAlreadyExistsError",
    "UsernameAlreadyExistsError",
    "PhoneNumberAlreadyExistsError",
    "InvalidCredentialsError",
    "AccountInactiveError",
    "AccountSuspendedError",
    "InvalidTokenError",
    "SessionRevokedOrExpiredError",
    "UserNotFoundError",
    "InvalidOtpError",
    "OtpExpiredError",
    "OtpMaxAttemptsError",
]
