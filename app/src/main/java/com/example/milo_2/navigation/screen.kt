package com.example.milo_2.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {

    data object Start : Screen("start")

    data object Logging1 : Screen("logging1")

    data object Logging2 : Screen("logging2")

    data object Welcome : Screen("welcome")

    data object Signup : Screen("signup")

    data object Login : Screen("login")

    data object Dashboard : Screen("dashboard")

    data object OtpVerification : Screen("otp_verification/{destination}/{type}/{email}/{phone}/{devOtp}") {
        fun createRoute(
            destination: String,
            type: String = "EMAIL",
            email: String = "",
            phone: String = "",
            devOtp: String? = null
        ): String {
            val encDest = URLEncoder.encode(destination.ifBlank { "none" }, "UTF-8")
            val encType = URLEncoder.encode(type.ifBlank { "EMAIL" }, "UTF-8")
            val encEmail = URLEncoder.encode(email.ifBlank { "none" }, "UTF-8")
            val encPhone = URLEncoder.encode(phone.ifBlank { "none" }, "UTF-8")
            val encDevOtp = URLEncoder.encode(devOtp?.ifBlank { "none" } ?: "none", "UTF-8")
            return "otp_verification/$encDest/$encType/$encEmail/$encPhone/$encDevOtp"
        }
    }
}
