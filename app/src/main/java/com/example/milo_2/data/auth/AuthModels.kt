package com.example.milo_2.data.auth

/**
 * Domain and network models for MILO User Authentication and OTP Verification.
 */
data class AuthUser(
    val userId: String,
    val fullName: String,
    val username: String,
    val email: String,
    val phoneNumber: String? = null,
    val city: String? = null,
    val isEmailVerified: Boolean = false,
    val isPhoneVerified: Boolean = false
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "bearer",
    val expiresIn: Int = 3600
)

data class AuthResult(
    val user: AuthUser,
    val tokens: AuthTokens,
    val requiresOtp: Boolean = false,
    val otpChannel: String? = null,
    val maskedDestination: String? = null,
    val debugOtp: String? = null
)

data class OtpSentResult(
    val message: String,
    val type: String,
    val destination: String,
    val expiresInSeconds: Int = 600,
    val debugOtp: String? = null
)

data class VerificationResult(
    val message: String,
    val verified: Boolean,
    val type: String,
    val userId: String,
    val isEmailVerified: Boolean,
    val isPhoneVerified: Boolean,
    val user: AuthUser? = null,
    val tokens: AuthTokens? = null
)
