package com.example.milo_2.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.example.milo_2.dashboard.Dashboard
import com.example.milo_2.screens.StartScreen.StartScreen
import com.example.milo_2.screens.Logging1.Logging1
import com.example.milo_2.screens.Logging2.Logging2
import com.example.milo_2.screens.auth.LoginScreen
import com.example.milo_2.screens.auth.SignupScreen
import com.example.milo_2.screens.welcomescreen.WelcomeScreen


@Composable
fun MiloNavGraph() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Start.route
    ) {

        // =================================================
        // START SCREEN
        // =================================================

        composable(Screen.Start.route) {

            StartScreen(
                onFinished = {

                    navController.navigate(
                        Screen.Logging1.route
                    )
                }
            )
        }


        // =================================================
        // LOGGING 1
        // =================================================

        composable(Screen.Logging1.route) {

            Logging1(
                onNext = {

                    navController.navigate(
                        Screen.Logging2.route
                    )
                }
            )
        }


        // =================================================
        // LOGGING 2
        // =================================================

        composable(Screen.Logging2.route) {

            Logging2(
                onNext = {

                    navController.navigate(
                        Screen.Welcome.route
                    )
                }
            )
        }


        // =================================================
        // WELCOME SCREEN
        // =================================================

        composable(Screen.Welcome.route) {

            WelcomeScreen(

                // -----------------------------------------
                // LOG IN
                // -----------------------------------------

                onLogin = {

                    navController.navigate(
                        Screen.Login.route
                    )
                },


                // -----------------------------------------
                // GET STARTED → SIGNUP
                // -----------------------------------------

                onSignup = {

                    navController.navigate(
                        Screen.Signup.route
                    )
                }
            )
        }


        // =================================================
        // SIGNUP SCREEN
        // =================================================

        composable(Screen.Signup.route) {

            SignupScreen(

                // -----------------------------------------
                // BACK → WELCOME
                // -----------------------------------------

                onBack = {

                    navController.popBackStack()
                },


                // -----------------------------------------
                // SIGNUP SCREEN → LOGIN SCREEN
                // -----------------------------------------

                onLogin = {

                    navController.navigate(
                        Screen.Login.route
                    )
                },


                // -----------------------------------------
                // FIREBASE SIGNUP SUCCESS
                // -----------------------------------------

                onSignupSuccess = {

                    navController.navigate(
                        Screen.Dashboard.route
                    ) {

                        // Remove authentication screens
                        popUpTo(
                            Screen.Welcome.route
                        ) {
                            inclusive = true
                        }

                        // Prevent duplicate Dashboard
                        launchSingleTop = true
                    }
                }
            )
        }


        // =================================================
        // LOGIN SCREEN
        // =================================================

        composable(Screen.Login.route) {

            LoginScreen(

                // -----------------------------------------
                // BACK → PREVIOUS SCREEN
                // -----------------------------------------

                onBack = {

                    navController.popBackStack()
                },


                // -----------------------------------------
                // LOGIN → SIGNUP
                // -----------------------------------------

                onSignup = {

                    navController.navigate(
                        Screen.Signup.route
                    )
                },


                // -----------------------------------------
                // FIREBASE LOGIN SUCCESS
                // -----------------------------------------

                onLoginSuccess = {

                    navController.navigate(
                        Screen.Dashboard.route
                    ) {

                        // Remove Welcome/Login/Signup
                        // from back stack
                        popUpTo(
                            Screen.Welcome.route
                        ) {
                            inclusive = true
                        }

                        // Prevent duplicate Dashboard
                        launchSingleTop = true
                    }
                }
            )
        }


        // =================================================
        // DASHBOARD
        // =================================================

        composable(Screen.Dashboard.route) {

            Dashboard()
        }
    }
}