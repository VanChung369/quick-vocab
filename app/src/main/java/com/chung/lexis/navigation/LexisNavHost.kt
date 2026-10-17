package com.chung.lexis.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.chung.lexis.feature.decks.DecksScreen
import com.chung.lexis.feature.home.HomeScreen
import com.chung.lexis.feature.review.ReviewScreen


@Composable
fun LexisNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {

    NavHost(
        navController = navController,
        startDestination = LexisDestination.Home.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) },
    ) {

        composable(
            route = LexisDestination.Home.route,
        ) {
            HomeScreen()
        }

        composable(
            route = LexisDestination.Decks.route,
        ) {
            DecksScreen()
        }

        composable(
            route = LexisDestination.Review.route,
        ) {
            ReviewScreen()
        }
    }
}