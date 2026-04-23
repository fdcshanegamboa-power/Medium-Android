package com.connect.medium.ui.auth

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.connect.medium.ui.auth.screens.LoginScreen
import com.connect.medium.ui.auth.screens.RegisterScreen

@Composable
fun AuthNavGraph(viewModel: AuthViewModel, onAuthSuccess: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { navController.navigate("register") },
                onAuthSuccess = onAuthSuccess
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onAuthSuccess = onAuthSuccess
            )
        }
    }
}