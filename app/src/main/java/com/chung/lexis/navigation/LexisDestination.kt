package com.chung.lexis.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.ui.graphics.vector.ImageVector


sealed class LexisDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {

    data object Home : LexisDestination(
        route = "home",
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    )

    data object Decks : LexisDestination(
        route = "decks",
        label = "Decks",
        selectedIcon = Icons.AutoMirrored.Filled.LibraryBooks,
        unselectedIcon = Icons.AutoMirrored.Outlined.LibraryBooks,
    )

    data object Review : LexisDestination(
        route = "review",
        label = "Review",
        selectedIcon = Icons.Filled.School,
        unselectedIcon = Icons.Outlined.School,
    )

}

val topLevelDestinations = listOf(
    LexisDestination.Home,
    LexisDestination.Decks,
    LexisDestination.Review,
)