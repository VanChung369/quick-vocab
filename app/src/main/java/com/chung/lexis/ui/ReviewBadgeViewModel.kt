package com.chung.lexis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chung.lexis.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock

data class ReviewBadgeUiState(
    val dueCount: Int = 0,
    val isLoading: Boolean = true,
)

class ReviewBadgeViewModel(
    private val reviewRepository:
    ReviewRepository,

    private val clock:
    Clock = Clock.systemUTC(),
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            ReviewBadgeUiState()
        )

    val uiState:
            StateFlow<ReviewBadgeUiState> =
        _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {

        viewModelScope.launch {

            val previous =
                _uiState.value

            _uiState.value =
                previous.copy(
                    isLoading = true,
                )

            try {

                val summary =
                    reviewRepository
                        .getReviewDueSummary(
                            now =
                                clock.instant(),
                        )

                _uiState.value =
                    ReviewBadgeUiState(
                        dueCount =
                            summary.dueCount,

                        isLoading = false,
                    )

            } catch (_: Exception) {
                _uiState.value =
                    previous.copy(
                        isLoading = false,
                    )
            }
        }
    }

    class Factory(
        private val reviewRepository:
        ReviewRepository,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel>
                create(
            modelClass: Class<T>,
        ): T {

            require(
                modelClass.isAssignableFrom(
                    ReviewBadgeViewModel::class.java
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