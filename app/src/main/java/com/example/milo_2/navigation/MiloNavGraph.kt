package com.example.milo_2.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.milo_2.screens.StartScreen.StartScreen
import com.example.milo_2.screens.Logging1.Logging1
import com.example.milo_2.screens.Logging2.Logging2
import com.example.milo_2.screens.welcomescreen.WelcomeScreen
import com.example.milo_2.screens.auth.LoginScreen
import com.example.milo_2.screens.auth.SignupScreen
import com.example.milo_2.screens.auth.OtpVerificationScreen
import com.example.milo_2.screens.dashboard.Dashboard
import java.net.URLDecoder

@Composable
fun MiloNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Start.route
    ) {
        composable(Screen.Start.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val sessionManager = androidx.compose.runtime.remember { com.example.milo_2.data.auth.SessionManager(context) }
            StartScreen(
                onFinished = {
                    if (sessionManager.isLoggedIn()) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Start.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Logging1.route) {
                            popUpTo(Screen.Start.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Logging1.route) {
            Logging1(
                onNext = {
                    navController.navigate(Screen.Logging2.route)
                }
            )
        }

        composable(Screen.Logging2.route) {
            Logging2(
                onNext = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Logging1.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onSignup = {
                    navController.navigate(Screen.Signup.route)
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSignup = {
                    navController.navigate(Screen.Signup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToOtp = { destination, type, email, phone ->
                    navController.navigate(
                        Screen.OtpVerification.createRoute(destination, type, email, phone)
                    )
                }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onBack = {
                    navController.popBackStack()
                },
                onLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                },
                onSignupSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToOtp = { destination, type, email, phone ->
                    navController.navigate(
                        Screen.OtpVerification.createRoute(destination, type, email, phone)
                    )
                }
            )
        }

        composable(
            route = Screen.OtpVerification.route,
            arguments = listOf(
                navArgument("destination") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType },
                navArgument("email") { type = NavType.StringType },
                navArgument("phone") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val destinationRaw = backStackEntry.arguments?.getString("destination") ?: ""
            val typeRaw = backStackEntry.arguments?.getString("type") ?: "EMAIL"
            val emailRaw = backStackEntry.arguments?.getString("email") ?: ""
            val phoneRaw = backStackEntry.arguments?.getString("phone") ?: ""

            val destination = URLDecoder.decode(destinationRaw, "UTF-8").let { if (it == "none") "" else it }
            val type = URLDecoder.decode(typeRaw, "UTF-8").let { if (it == "none") "EMAIL" else it }
            val email = URLDecoder.decode(emailRaw, "UTF-8").let { if (it == "none") "" else it }
            val phone = URLDecoder.decode(phoneRaw, "UTF-8").let { if (it == "none") "" else it }

            OtpVerificationScreen(
                destination = destination,
                verificationType = type,
                email = email,
                phoneNumber = phone,
                onBack = {
                    navController.popBackStack()
                },
                onVerificationSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            Dashboard(
                onLogout = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
