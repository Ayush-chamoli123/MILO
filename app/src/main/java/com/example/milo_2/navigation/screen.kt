package com.example.milo_2.navigation

sealed class Screen(val route: String) {

    data object Start : Screen("start")

    data object Logging1 : Screen("logging1")

    data object Logging2 : Screen("logging2")

    data object Welcome : Screen("welcome")

    data object Signup : Screen("signup")

    data object Login : Screen("login")

    data object Dashboard : Screen("dashboard")
}