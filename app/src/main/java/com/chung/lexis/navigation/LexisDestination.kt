package com.chung.lexis.navigation

import androidx.annotation.DrawableRes
import com.chung.lexis.R


sealed class LexisDestination(
    val route: String,
    val label: String,

    @param:DrawableRes
    val selectedIconRes: Int,

    @param:DrawableRes
    val unselectedIconRes: Int,
) {

    data object Home : LexisDestination(
        route = "home",
        label = "Home",
        selectedIconRes = R.drawable.ic_home_rounded_filled,
        unselectedIconRes = R.drawable.ic_home_rounded,
    )

    data object Decks : LexisDestination(
        route = "decks",
        label = "Decks",
        selectedIconRes = R.drawable.ic_stacks_rounded_filled,
        unselectedIconRes = R.drawable.ic_stacks_rounded,
    )

    data object Review : LexisDestination(
        route = "review",
        label = "Review",
        selectedIconRes = R.drawable.ic_style_rounded_filled,
        unselectedIconRes = R.drawable.ic_style_rounded,
    )

}

val topLevelDestinations = listOf(
    LexisDestination.Home,
    LexisDestination.Decks,
    LexisDestination.Review,
)