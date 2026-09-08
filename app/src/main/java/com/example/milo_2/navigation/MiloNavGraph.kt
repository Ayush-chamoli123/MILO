package com.example.milo_2.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.milo_2.screens.StartScreen.StartScreen
import com.example.milo_2.screens.Logging1.Logging1
import com.example.milo_2.screens.Logging2.Logging2
import com.example.milo_2.screens.welcomescreen.WelcomeScreen
import com.example.milo_2.screens.auth.LoginScreen
import com.example.milo_2.screens.auth.SignupScreen
import com.example.milo_2.screens.dashboard.Dashboard

@Composable
fun MiloNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Start.route
    ) {
        composable(Screen.Start.route) {
            StartScreen(
                onFinished = {
                    navController.navigate(Screen.Logging1.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
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
                }
            )
        }

        composable(Screen.Dashboard.route) {
            Dashboard()
        }
    }
}
