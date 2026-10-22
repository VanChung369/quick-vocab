package com.chung.lexis.feature.review


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chung.lexis.domain.model.ReviewRating


@Suppress("UNUSED_PARAMETER")
@Composable
fun ReviewScreen(
    uiState: ReviewSessionUiState,
    onRevealAnswer: () -> Unit,
    onSubmitRating: (ReviewRating) -> Unit,
    modifier: Modifier = Modifier,
) {

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Review",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}