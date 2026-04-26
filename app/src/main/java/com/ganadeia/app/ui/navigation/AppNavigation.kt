package com.ganadeia.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ganadeia.app.ui.screens.AnimalsScreen
import com.ganadeia.app.ui.screens.AnimalDetailScreen
import com.ganadeia.app.ui.screens.DashboardScreen
import com.ganadeia.app.ui.screens.IaAnalysisScreen
import com.ganadeia.app.ui.screens.LoginScreen
import com.ganadeia.app.ui.screens.ProfileScreen
import com.ganadeia.app.ui.screens.RegisterAnimalScreen
import com.ganadeia.app.ui.screens.RegisterScreen
import com.ganadeia.app.ui.viewmodel.AnimalViewModel
import com.ganadeia.app.ui.viewmodel.AuthViewModel
import com.ganadeia.app.ui.viewmodel.IaAnalysisViewModel
import com.ganadeia.app.ui.viewmodel.SplashState

@Composable
fun AppNavigation() {
    val navController    = rememberNavController()
    val authViewModel: AuthViewModel     = viewModel()
    val animalViewModel: AnimalViewModel = viewModel()
    val iaAnalysisViewModel: IaAnalysisViewModel = viewModel()

    val splashState by authViewModel.splashState.collectAsState()

    // Una vez que el ViewModel termina de revisar la sesión, navegamos.
    // El startDestination SIEMPRE es "login" para evitar el crash de NavHost dinámico.
    // Si hay sesión activa, se redirige al dashboard por LaunchedEffect.
    LaunchedEffect(splashState) {
        when (splashState) {
            is SplashState.SessionActive -> {
                navController.navigate("dashboard") {
                    popUpTo("login") { inclusive = true }
                }
            }
            else -> { /* NoSession o Checking: quedarse en login */ }
        }
    }

    NavHost(
        navController    = navController,
        startDestination = "login"   // ← FIJO, nunca dinámico
    ) {

        // ── Auth ──────────────────────────────────────────────────────────────
        composable("login") {
            LoginScreen(
                viewModel            = authViewModel,
                onLoginSuccess       = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                viewModel         = authViewModel,
                onRegisterSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateBack    = { navController.popBackStack() }
            )
        }

        // ── Main screens ──────────────────────────────────────────────────────
        composable("dashboard") {
            DashboardScreen(
                onNavigateToProfile        = {
                    navController.navigate("profile") { popUpTo("dashboard") }
                },
                onNavigateToRegisterAnimal = {
                    navController.navigate("register_animal")
                },
                onNavigateToIaAnalysis     = {
                    navController.navigate("ia_analysis") { popUpTo("dashboard") }
                },
                onNavigateToAnimals        = {
                    navController.navigate("animals") { popUpTo("dashboard") }
                }
            )
        }

        composable("profile") {
            ProfileScreen(
                authViewModel       = authViewModel,
                onNavigateToHome    = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                onNavigateToIaAnalysis = {
                    navController.navigate("ia_analysis") { popUpTo("dashboard") }
                },
                onNavigateToAnimals = {
                    navController.navigate("animals") { popUpTo("dashboard") }
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("register_animal") {
            RegisterAnimalScreen(
                viewModel      = animalViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("ia_analysis") {
            IaAnalysisScreen(
                viewModel = iaAnalysisViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("animals") {
            AnimalsScreen(
                viewModel          = animalViewModel,
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToDetail = { id ->
                    navController.navigate("animal_detail/$id")
                }
            )
        }

        composable("animal_detail/{animalId}") { backStackEntry ->
            val animalId = backStackEntry.arguments?.getString("animalId") ?: ""
            AnimalDetailScreen(
                viewModel      = animalViewModel,
                animalId       = animalId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
