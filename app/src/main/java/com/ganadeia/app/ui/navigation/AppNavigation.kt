package com.ganadeia.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ganadeia.app.ui.screens.DashboardScreen
import com.ganadeia.app.ui.screens.LoginScreen
import com.ganadeia.app.ui.screens.ProfileScreen
import com.ganadeia.app.ui.screens.RegisterAnimalScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                onNavigateToProfile = {
                    navController.navigate("profile") {
                        popUpTo("dashboard")
                    }
                },
                onNavigateToRegisterAnimal = {
                    navController.navigate("register_animal")
                }
            )
        }
        composable("profile") {
            ProfileScreen(
                onNavigateToHome = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }
        composable("register_animal") {
            RegisterAnimalScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
