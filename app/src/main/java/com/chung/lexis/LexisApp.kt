package com.chung.lexis

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.chung.lexis.navigation.LexisBottomBar
import com.chung.lexis.navigation.LexisNavHost

@Composable
fun LexisApp() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),

        bottomBar = {
            LexisBottomBar(
                navController = navController,
            )
        },
    ) { innerPadding ->

        LexisNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}