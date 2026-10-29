package com.tinhcd.myesalesmanager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tinhcd.myesalesmanager.feature.auth.LoginScreen
import com.tinhcd.myesalesmanager.feature.shell.MainShell

object Routes {
    const val LOGIN = "login"
    const val SHELL = "shell"
}

@Composable
fun AppNavHost(startDestination: String) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onSignedIn = {
                    nav.navigate(Routes.SHELL) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
            )
        }
        composable(Routes.SHELL) {
            MainShell(
                onSignedOut = {
                    nav.navigate(Routes.LOGIN) { popUpTo(Routes.SHELL) { inclusive = true } }
                },
            )
        }
    }
}
