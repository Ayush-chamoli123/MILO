package com.example.milo_2.data.auth

/**
 * Domain and network models for MILO User Authentication.
 */
data class AuthUser(
    val userId: String,
    val fullName: String,
    val username: String,
    val email: String,
    val phoneNumber: String? = null,
    val city: String? = null
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "bearer",
    val expiresIn: Int = 3600
)

data class AuthResult(
    val user: AuthUser,
    val tokens: AuthTokens
)
