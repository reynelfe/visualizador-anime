package com.animeav.tv.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.animeav.tv.PlayerActivity
import com.animeav.tv.ui.screens.CatalogScreen
import com.animeav.tv.ui.screens.DetailScreen
import com.animeav.tv.ui.screens.DownloadsScreen
import com.animeav.tv.ui.screens.HomeScreen
import com.animeav.tv.ui.screens.LibraryScreen
import com.animeav.tv.ui.screens.SearchScreen
import com.animeav.tv.ui.screens.SettingsScreen

@Composable
fun AppNav() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
    ) {
        composable("home") { HomeScreen(navController) }
        composable("catalog") { CatalogScreen(navController) }
        composable("search") { SearchScreen(navController) }
        composable("library") { LibraryScreen(navController) }
        composable("downloads") { DownloadsScreen(navController) }
        composable("settings") { SettingsScreen(navController) }

        composable(
            route = "detail/{slug}",
            arguments = listOf(navArgument("slug") { type = NavType.StringType }),
        ) { entry ->
            DetailScreen(
                nav = navController,
                slug = entry.arguments?.getString("slug").orEmpty(),
            )
        }

        composable(
            route = "player/{slug}/{number}",
            arguments = listOf(
                navArgument("slug") { type = NavType.StringType },
                navArgument("number") {
                    type = NavType.IntType
                    defaultValue = 1
                },
            ),
        ) { entry ->
            val slug = entry.arguments?.getString("slug").orEmpty()
            val number = entry.arguments?.getInt("number") ?: 1
            val context = LocalContext.current

            LaunchedEffect(slug, number) {
                if (slug.isNotBlank()) {
                    context.startActivity(PlayerActivity.intent(context, slug, number))
                }
                navController.popBackStack()
            }
        }
    }
}
