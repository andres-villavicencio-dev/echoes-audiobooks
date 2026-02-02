package com.echoesapp.audiobooks.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.echoesapp.audiobooks.ui.screens.book.BookDetailScreen
import com.echoesapp.audiobooks.ui.screens.library.LibraryScreen
import com.echoesapp.audiobooks.ui.screens.player.PlayerScreen
import com.echoesapp.audiobooks.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object BookDetail : Screen("book/{bookId}") {
        fun createRoute(bookId: String) = "book/$bookId"
    }
    data object Player : Screen("player")
    data object Settings : Screen("settings")
}

@Composable
fun EchoesNavHost(
    navController: NavHostController,
    onNavigateToPlayer: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Library.route,
    ) {
        composable(Screen.Library.route) {
            LibraryScreen(
                onBookClick = { bookId ->
                    navController.navigate(Screen.BookDetail.createRoute(bookId))
                },
                onNavigateToPlayer = onNavigateToPlayer,
            )
        }

        composable(
            route = Screen.BookDetail.route,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
            BookDetailScreen(
                bookId = bookId,
                onBackClick = { navController.popBackStack() },
                onPlayClick = onNavigateToPlayer,
            )
        }

        composable(Screen.Player.route) {
            PlayerScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
