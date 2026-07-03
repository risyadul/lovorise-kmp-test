package com.lovorise.discover.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.lovorise.discover.feature.home.HomeScreen
import com.lovorise.discover.feature.search.SearchScreen
import com.lovorise.discover.feature.story.StoryViewerScreen

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val STORY = "story/{storyId}?connectionsOnly={connectionsOnly}"
    fun story(storyId: String, connectionsOnly: Boolean = false) =
        "story/$storyId?connectionsOnly=$connectionsOnly"
}

@Composable
fun LovoriseNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(
            route = Routes.HOME,
            exitTransition = { fadeOut(tween(220)) },
            popEnterTransition = { fadeIn(tween(220)) },
        ) {
            HomeScreen(
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenStory = { storyId, connectionsOnly ->
                    navController.navigate(Routes.story(storyId, connectionsOnly))
                },
            )
        }

        composable(
            route = Routes.SEARCH,
            enterTransition = {
                slideInHorizontally(tween(280)) { it } + fadeIn(tween(280))
            },
            popExitTransition = {
                slideOutHorizontally(tween(240)) { it } + fadeOut(tween(240))
            },
        ) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenStory = { storyId -> navController.navigate(Routes.story(storyId)) },
            )
        }

        composable(
            route = Routes.STORY,
            arguments = listOf(
                navArgument("connectionsOnly") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
            enterTransition = {
                slideInVertically(tween(320)) { it / 3 } + fadeIn(tween(320))
            },
            popExitTransition = {
                slideOutVertically(tween(260)) { it / 3 } + fadeOut(tween(260))
            },
        ) { backStackEntry ->
            val storyId = backStackEntry.arguments?.read { getStringOrNull("storyId") }.orEmpty()
            StoryViewerScreen(
                startStoryId = storyId,
                onClose = { navController.popBackStack() },
            )
        }
    }
}
