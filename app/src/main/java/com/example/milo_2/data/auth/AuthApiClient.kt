package com.example.milo_2.data.auth

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Communicates with the FastAPI MILO authentication and OTP verification endpoints.
 */
class AuthApiClient(context: Context) {

    private val sessionManager = SessionManager(context)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    val baseUrl: String
        get() = sessionManager.baseUrl

    /**
     * Check if the backend health endpoint is reachable.
     */
    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/health")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Register a new user account with FastAPI backend.
     */
    suspend fun register(
        fullName: String,
        username: String,
        email: String,
        password: String,
        phoneNumber: String? = null
    ): Result<AuthResult> = withContext(Dispatchers.IO) {
        for (attempt in 1..2) {
            try {
                val payload = JSONObject().apply {
                    put("full_name", fullName)
                    put("username", username)
                    put("email", email)
                    put("password", password)
                    if (!phoneNumber.isNullOrBlank()) {
                        put("phone_number", phoneNumber)
                    }
                }

                val requestBody = payload.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url("$baseUrl/api/v1/auth/register")
                    .post(requestBody)
                    .build()

                return@withContext client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string().orEmpty()

                    if (response.isSuccessful) {
                        val result = parseAuthResult(bodyString)
                        sessionManager.saveSession(result.user, result.tokens)
                        Result.success(result)
                    } else {
                        val errorMessage = extractErrorMessage(bodyString, response.code)
                        Result.failure(Exception(errorMessage))
                    }
                }
            } catch (e: IOException) {
                if (attempt < 2) {
                    kotlinx.coroutines.delay(1200)
                } else {
                    return@withContext Result.failure(
                        Exception(
                            "Network error connecting to backend ($baseUrl). Ensure FastAPI is running on host."
                        )
                    )
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
        Result.failure(Exception("Unknown error connecting to backend ($baseUrl)."))
    }

    /**
     * Authenticate an existing user with username/email and password, or trigger OTP.
     */
    suspend fun login(
        emailOrUsername: String,
        password: String? = null,
        loginWithOtp: Boolean = false,
        deviceName: String = "Android Device"
    ): Result<AuthResult> = withContext(Dispatchers.IO) {
        for (attempt in 1..2) {
            try {
                val payload = JSONObject().apply {
                    put("email_or_username", emailOrUsername)
                    if (!password.isNullOrBlank()) {
                        put("password", password)
                    }
                    put("login_with_otp", loginWithOtp)
                    put("device_name", deviceName)
                }

                val requestBody = payload.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url("$baseUrl/api/v1/auth/login")
                    .post(requestBody)
                    .build()

                return@withContext client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string().orEmpty()

                    if (response.isSuccessful) {
                        val result = parseAuthResult(bodyString)
                        sessionManager.saveSession(result.user, result.tokens)
                        Result.success(result)
                    } else {
                        val errorMessage = extractErrorMessage(bodyString, response.code)
                        Result.failure(Exception(errorMessage))
                    }
                }
            } catch (e: IOException) {
                if (attempt < 2) {
                    kotlinx.coroutines.delay(1200)
                } else {
                    return@withContext Result.failure(
                        Exception(
                            "Network error connecting to backend ($baseUrl). Ensure FastAPI is running on host."
                        )
                    )
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
        Result.failure(Exception("Unknown error connecting to backend ($baseUrl)."))
    }

    /**
     * Dispatch a 6-digit OTP verification code to the target email or phone.
     */
    suspend fun sendOtp(
        email: String? = null,
        phoneNumber: String? = null,
        type: String = "EMAIL"
    ): Result<OtpSentResult> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("type", type.uppercase())
                if (!email.isNullOrBlank()) put("email", email.trim().lowercase())
                if (!phoneNumber.isNullOrBlank()) put("phone_number", phoneNumber.trim())
            }

            val requestBody = payload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/api/v1/auth/otp/send")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(bodyString)
                    val result = OtpSentResult(
                        message = json.optString("message", "OTP sent"),
                        type = json.optString("type", type),
                        destination = json.optString("destination", email ?: phoneNumber ?: ""),
                        expiresInSeconds = json.optInt("expires_in_seconds", 600),
                        debugOtp = if (json.has("debug_otp") && !json.isNull("debug_otp")) json.getString("debug_otp") else null
                    )
                    Result.success(result)
                } else {
                    val errorMessage = extractErrorMessage(bodyString, response.code)
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error connecting to backend ($baseUrl). Ensure FastAPI is running on host."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate a user-submitted 6-digit OTP verification code.
     */
    suspend fun verifyOtp(
        otpCode: String,
        email: String? = null,
        phoneNumber: String? = null,
        type: String = "EMAIL",
        deviceName: String = "Android Device"
    ): Result<VerificationResult> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("otp_code", otpCode.trim())
                put("type", type.uppercase())
                if (!email.isNullOrBlank()) put("email", email.trim().lowercase())
                if (!phoneNumber.isNullOrBlank()) put("phone_number", phoneNumber.trim())
                put("device_name", deviceName)
            }

            val requestBody = payload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/api/v1/auth/otp/verify")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(bodyString)
                    var user: AuthUser? = null
                    var tokens: AuthTokens? = null

                    if (json.has("user") && !json.isNull("user")) {
                        val userObj = json.getJSONObject("user")
                        user = AuthUser(
                            userId = userObj.optString("user_id"),
                            fullName = userObj.optString("full_name"),
                            username = userObj.optString("username"),
                            email = userObj.optString("email"),
                            phoneNumber = if (userObj.isNull("phone_number")) null else userObj.optString("phone_number"),
                            city = if (userObj.isNull("city")) null else userObj.optString("city"),
                            isEmailVerified = userObj.optBoolean("is_email_verified", true),
                            isPhoneVerified = userObj.optBoolean("is_phone_verified", false)
                        )
                    }

                    if (json.has("tokens") && !json.isNull("tokens")) {
                        val tokensObj = json.getJSONObject("tokens")
                        tokens = AuthTokens(
                            accessToken = tokensObj.getString("access_token"),
                            refreshToken = tokensObj.optString("refresh_token", ""),
                            tokenType = tokensObj.optString("token_type", "bearer"),
                            expiresIn = tokensObj.optInt("expires_in", 3600)
                        )
                    }

                    if (user != null && tokens != null) {
                        sessionManager.saveSession(user, tokens)
                    }

                    Result.success(
                        VerificationResult(
                            message = json.optString("message", "Verification successful"),
                            verified = json.optBoolean("verified", true),
                            type = json.optString("type", type),
                            userId = json.optString("user_id"),
                            isEmailVerified = json.optBoolean("is_email_verified", true),
                            isPhoneVerified = json.optBoolean("is_phone_verified", false),
                            user = user,
                            tokens = tokens
                        )
                    )
                } else {
                    val errorMessage = extractErrorMessage(bodyString, response.code)
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error connecting to backend ($baseUrl). Ensure FastAPI is running on host."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Revoke active session on server and clear local session state.
     */
    suspend fun logout(): Result<Boolean> = withContext(Dispatchers.IO) {
        val refreshToken = sessionManager.getRefreshToken()
        try {
            if (!refreshToken.isNullOrBlank()) {
                val payload = JSONObject().apply {
                    put("refresh_token", refreshToken)
                }
                val requestBody = payload.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url("$baseUrl/api/v1/auth/logout")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().close()
            }
            sessionManager.clearSession()
            Result.success(true)
        } catch (e: Exception) {
            sessionManager.clearSession()
            Result.success(true)
        }
    }

    /**
     * Fetch current user profile details from /api/v1/auth/me.
     */
    suspend fun getCurrentUser(): Result<AuthUser> = withContext(Dispatchers.IO) {
        val token = sessionManager.getAccessToken()
            ?: return@withContext Result.failure(Exception("No active session found"))

        try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/auth/me")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(bodyString)
                    val user = AuthUser(
                        userId = json.optString("user_id"),
                        fullName = json.optString("full_name"),
                        username = json.optString("username"),
                        email = json.optString("email"),
                        phoneNumber = if (json.isNull("phone_number")) null else json.optString("phone_number"),
                        city = if (json.isNull("city")) null else json.optString("city"),
                        isEmailVerified = json.optBoolean("is_email_verified", false),
                        isPhoneVerified = json.optBoolean("is_phone_verified", false)
                    )
                    Result.success(user)
                } else {
                    Result.failure(Exception(extractErrorMessage(bodyString, response.code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseAuthResult(jsonString: String): AuthResult {
        val root = JSONObject(jsonString)
        val userObj = root.getJSONObject("user")
        val tokensObj = root.getJSONObject("tokens")

        val user = AuthUser(
            userId = userObj.getString("user_id"),
            fullName = userObj.getString("full_name"),
            username = userObj.getString("username"),
            email = userObj.getString("email"),
            phoneNumber = if (userObj.isNull("phone_number")) null else userObj.optString("phone_number"),
            city = if (userObj.isNull("city")) null else userObj.optString("city"),
            isEmailVerified = userObj.optBoolean("is_email_verified", false),
            isPhoneVerified = userObj.optBoolean("is_phone_verified", false)
        )

        val tokens = AuthTokens(
            accessToken = tokensObj.getString("access_token"),
            refreshToken = tokensObj.optString("refresh_token", ""),
            tokenType = tokensObj.optString("token_type", "bearer"),
            expiresIn = tokensObj.optInt("expires_in", 3600)
        )

        val requiresOtp = root.optBoolean("requires_otp", false)
        val otpChannel = if (root.has("otp_channel") && !root.isNull("otp_channel")) root.getString("otp_channel") else null
        val maskedDestination = if (root.has("masked_destination") && !root.isNull("masked_destination")) root.getString("masked_destination") else null
        val debugOtp = if (root.has("debug_otp") && !root.isNull("debug_otp")) root.getString("debug_otp") else null

        return AuthResult(user, tokens, requiresOtp, otpChannel, maskedDestination, debugOtp)
    }

    private fun extractErrorMessage(bodyString: String, statusCode: Int): String {
        try {
            if (bodyString.isNotBlank()) {
                val json = JSONObject(bodyString)
                if (json.has("detail")) {
                    val detail = json.get("detail")
                    if (detail is String) {
                        return detail
                    } else if (detail is JSONArray) {
                        val messages = mutableListOf<String>()
                        for (i in 0 until detail.length()) {
                            val item = detail.optJSONObject(i)
                            if (item != null && item.has("msg")) {
                                messages.add(item.getString("msg"))
                            }
                        }
                        if (messages.isNotEmpty()) {
                            return messages.joinToString("; ")
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // fallback
        }

        return when (statusCode) {
            400 -> "Bad request. Please verify your submitted information."
            401 -> "Invalid credentials. Please verify your details."
            403 -> "Account access is restricted."
            404 -> "Account not found."
            409 -> "An account with these details already exists."
            422 -> "Validation error. Please verify input fields."
            429 -> "Too many attempts. Please try again later."
            else -> "Server returned error ($statusCode). Please try again."
        }
    }
}
