"""
MILO Repositories Package
=========================
Database persistence access layer.
"""

from app.repositories.user_repository import UserRepository

__all__ = [
    "UserRepository",
]
