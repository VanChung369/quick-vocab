package com.chung.lexis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.chung.lexis.ui.ReviewBadgeViewModel
import com.chung.lexis.ui.theme.LexisTheme

class MainActivity :
    ComponentActivity() {

    private val reviewBadgeViewModel:
            ReviewBadgeViewModel by lazy {

        val application =
            application as LexisApplication

        val factory =
            ReviewBadgeViewModel.Factory(
                reviewRepository =
                    application
                        .appContainer
                        .reviewRepository,
            )

        ViewModelProvider(
            this,
            factory,
        )[ReviewBadgeViewModel::class.java]
    }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()

        setContent {

            LexisTheme {

                LexisApp(
                    reviewBadgeViewModel =
                        reviewBadgeViewModel,
                )
            }
        }
    }
}