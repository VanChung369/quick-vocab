package com.chung.lexis

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.chung.lexis.navigation.LexisBottomBar
import com.chung.lexis.navigation.LexisNavHost
import com.chung.lexis.ui.ReviewBadgeViewModel

@Composable
fun LexisApp(
    reviewBadgeViewModel:
    ReviewBadgeViewModel,
) {

    val navController =
        rememberNavController()

    val reviewBadgeUiState by
    reviewBadgeViewModel
        .uiState
        .collectAsState()

    Scaffold(
        modifier =
            Modifier.fillMaxSize(),

        bottomBar = {

            LexisBottomBar(
                navController =
                    navController,

                reviewDueCount =
                    reviewBadgeUiState
                        .dueCount,
            )
        },
    ) { innerPadding ->

        LexisNavHost(
            navController =
                navController,

            modifier =
                Modifier.padding(
                    innerPadding
                ),
        )
    }
}