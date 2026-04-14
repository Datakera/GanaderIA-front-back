package com.ganadeia.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganadeia.app.ui.screens.DashboardScreen
import com.ganadeia.app.ui.screens.IaAnalysisScreen
import com.ganadeia.app.ui.screens.LoginScreen
import com.ganadeia.app.ui.screens.ProfileScreen
import com.ganadeia.app.ui.screens.RegisterAnimalScreen
import com.ganadeia.app.ui.screens.AnimalsScreen
import com.ganadeia.app.ui.screens.AnimalDetailScreen
import com.ganadeia.app.ui.viewmodel.AnimalViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val animalViewModel: AnimalViewModel = viewModel()

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
                },
                onNavigateToIaAnalysis = {
                    navController.navigate("ia_analysis") {
                        popUpTo("dashboard")
                    }
                },
                onNavigateToAnimals = {
                    navController.navigate("animals") {
                        popUpTo("dashboard")
                    }
                }
            )
        }
        composable("profile") {
            ProfileScreen(
                onNavigateToHome = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                onNavigateToIaAnalysis = {
                    navController.navigate("ia_analysis") {
                        popUpTo("dashboard")
                    }
                },
                onNavigateToAnimals = {
                    navController.navigate("animals") {
                        popUpTo("dashboard")
                    }
                }
            )
        }
        composable("register_animal") {
            RegisterAnimalScreen(
                viewModel = animalViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("ia_analysis") {
            IaAnalysisScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("animals") {
            AnimalsScreen(
                viewModel = animalViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { id ->
                    navController.navigate("animal_detail/$id")
                }
            )
        }
        composable("animal_detail/{animalId}") { backStackEntry ->
            val animalId = backStackEntry.arguments?.getString("animalId") ?: ""
            AnimalDetailScreen(
                viewModel = animalViewModel,
                animalId = animalId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
