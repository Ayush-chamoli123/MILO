package com.example.milo_2.data.auth

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user session persistence, authentication state, and backend configuration
 * using Android SharedPreferences.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "milo_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone_number"
        private const val KEY_CITY = "city"
        private const val KEY_BASE_URL = "custom_base_url"

        // Default to Android Emulator loopback alias (10.0.2.2:8000)
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000"
    }

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) {
            prefs.edit().putString(KEY_BASE_URL, value.trimEnd('/')).apply()
        }

    fun saveSession(user: AuthUser, tokens: AuthTokens) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, tokens.accessToken)
            putString(KEY_REFRESH_TOKEN, tokens.refreshToken)
            putString(KEY_USER_ID, user.userId)
            putString(KEY_FULL_NAME, user.fullName)
            putString(KEY_USERNAME, user.username)
            putString(KEY_EMAIL, user.email)
            putString(KEY_PHONE, user.phoneNumber)
            putString(KEY_CITY, user.city)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getUser(): AuthUser? {
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val fullName = prefs.getString(KEY_FULL_NAME, "") ?: ""
        val username = prefs.getString(KEY_USERNAME, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val phone = prefs.getString(KEY_PHONE, null)
        val city = prefs.getString(KEY_CITY, null)

        return AuthUser(
            userId = userId,
            fullName = fullName,
            username = username,
            email = email,
            phoneNumber = phone,
            city = city
        )
    }

    fun isLoggedIn(): Boolean {
        return !getAccessToken().isNullOrBlank()
    }

    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_FULL_NAME)
            remove(KEY_USERNAME)
            remove(KEY_EMAIL)
            remove(KEY_PHONE)
            remove(KEY_CITY)
            apply()
        }
    }
}
