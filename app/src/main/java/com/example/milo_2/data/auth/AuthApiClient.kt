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
 * Communicates with the FastAPI MILO authentication endpoints.
 */
class AuthApiClient(context: Context) {

    private val sessionManager = SessionManager(context)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val baseUrl: String
        get() = sessionManager.baseUrl

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

            client.newCall(request).execute().use { response ->
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
            Result.failure(
                Exception(
                    "Network error connecting to backend ($baseUrl). Ensure FastAPI is running on host."
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Authenticate an existing user with username/email and password.
     */
    suspend fun login(
        emailOrUsername: String,
        password: String,
        deviceName: String = "Android Device"
    ): Result<AuthResult> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("email_or_username", emailOrUsername)
                put("password", password)
                put("device_name", deviceName)
            }

            val requestBody = payload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/api/v1/auth/login")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
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
            Result.failure(
                Exception(
                    "Network error connecting to backend ($baseUrl). Ensure FastAPI is running on host."
                )
            )
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
                        city = if (json.isNull("city")) null else json.optString("city")
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
            city = if (userObj.isNull("city")) null else userObj.optString("city")
        )

        val tokens = AuthTokens(
            accessToken = tokensObj.getString("access_token"),
            refreshToken = tokensObj.optString("refresh_token", ""),
            tokenType = tokensObj.optString("token_type", "bearer"),
            expiresIn = tokensObj.optInt("expires_in", 3600)
        )

        return AuthResult(user, tokens)
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
            400 -> "Bad request. Please verify your input."
            401 -> "Invalid credentials or unauthorized access."
            403 -> "Account inactive or permission denied."
            404 -> "Resource not found on server."
            409 -> "An account with these details already exists."
            422 -> "Validation failed. Please check field formats."
            500 -> "Internal server error occurred."
            else -> "Request failed with HTTP status code $statusCode"
        }
    }
}
