package com.chung.lexis.feature.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ReviewRoute(
    viewModel:
    ReviewSessionViewModel,

    modifier:
    Modifier = Modifier,
) {

    val uiState by viewModel
        .uiState
        .collectAsStateWithLifecycle()

    LifecycleEventEffect(
        event =
            Lifecycle.Event.ON_START,
    ) {

        viewModel.onForeground()
    }

    ReviewScreen(
        uiState = uiState,

        onRevealAnswer = viewModel::revealAnswer,

        onSubmitRating = viewModel::submitRating,

        modifier = modifier,
    )
}
