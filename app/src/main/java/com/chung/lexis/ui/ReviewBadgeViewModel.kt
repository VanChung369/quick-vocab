package com.chung.lexis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chung.lexis.domain.repository.ReviewRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import java.time.Clock

data class ReviewBadgeUiState(
    val dueCount: Int = 0,
    val isLoading: Boolean = true,
)

class ReviewBadgeViewModel(
    reviewRepository: ReviewRepository,

    clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    val uiState:
            StateFlow<ReviewBadgeUiState> =

        reviewRepository
            .observeReviewDueSummary(
                clock = clock,
            )
            .map { summary ->

                ReviewBadgeUiState(
                    dueCount =
                        summary.dueCount,

                    isLoading =
                        false,
                )
            }
            .retryWhen {
                    cause,
                    attempt,
                ->

                if (
                    cause is
                            CancellationException
                ) {
                    return@retryWhen false
                }

                val retryDelayMillis =
                    (
                            attempt + 1L
                            )
                        .coerceAtMost(5L) *
                            1_000L

                delay(
                    retryDelayMillis
                )

                true
            }
            .stateIn(
                scope =
                    viewModelScope,

                /*
                 * stopTimeout = 0: delay() trên Main dùng
                 * uptimeMillis, không chạy khi máy ngủ sâu.
                 *
                 * Cancel upstream ngay khi app background để
                 * lúc quay lại luôn tính lại theo clock thật.
                 */
                started =
                    SharingStarted
                        .WhileSubscribed(
                            stopTimeoutMillis =
                                0L,
                        ),

                initialValue =
                    ReviewBadgeUiState(),
            )

    class Factory(
        private val reviewRepository:
        ReviewRepository,
    ) : ViewModelProvider.Factory {

        @Suppress(
            "UNCHECKED_CAST"
        )
        override fun <T : ViewModel>
                create(
            modelClass: Class<T>,
        ): T {

            require(
                modelClass
                    .isAssignableFrom(
                        ReviewBadgeViewModel::
                        class.java
                    )
            ) {
                "Unknown ViewModel class: " +
                        modelClass.name
            }

            return ReviewBadgeViewModel(
                reviewRepository =
                    reviewRepository,
            ) as T
        }
    }
}