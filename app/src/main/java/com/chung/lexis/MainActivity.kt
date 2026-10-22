package com.chung.lexis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.chung.lexis.feature.review.ReviewSessionViewModel
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

    private val reviewSessionViewModel:
            ReviewSessionViewModel by lazy {

        val application =
            application as LexisApplication

        val container =
            application.appContainer

        val factory =
            ReviewSessionViewModel.Factory(
                reviewRepository =
                    container.reviewRepository,

                submitReviewUseCase =
                    container.submitReviewUseCase,
            )

        ViewModelProvider(
            this,
            factory,
        )[ReviewSessionViewModel::class.java]
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

                    reviewSessionViewModel =
                        reviewSessionViewModel,
                )
            }
        }
    }
}