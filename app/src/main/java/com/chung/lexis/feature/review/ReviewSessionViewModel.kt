package com.chung.lexis.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chung.lexis.domain.model.ReviewCard
import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.repository.ReviewRepository
import com.chung.lexis.domain.repository.SaveReviewResult
import com.chung.lexis.domain.usecase.SubmitReviewUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock

sealed interface ReviewSessionUiState {
    data object Idle : ReviewSessionUiState

    data object Loading : ReviewSessionUiState

    data class Active(
        val card: ReviewCard,
        val isAnswerRevealed: Boolean = false,
        val isSubmitting: Boolean = false,
        val reviewedCount: Int,
        val remainingCount: Int,
        val error: ReviewSessionError? = null,
    ) : ReviewSessionUiState

    data class Completed(
        val reviewedCount: Int,
    ) : ReviewSessionUiState

    data class Failed(
        val error: ReviewSessionError,
    ) : ReviewSessionUiState
}

enum class ReviewSessionError {
    LOAD_FAILED,
    SAVE_FAILED,
    STALE_PROGRESS,
}


class ReviewSessionViewModel(
    private val reviewRepository: ReviewRepository,

    private val submitReviewUseCase: SubmitReviewUseCase,

    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<ReviewSessionUiState>(
            ReviewSessionUiState.Idle
        )

    val uiState:
            StateFlow<ReviewSessionUiState> =
        _uiState.asStateFlow()

    private val pendingLexemeIds = ArrayDeque<Long>()

    private var reviewedCount = 0

    fun startSession(
        dueLimit: Int,
        newLimit: Int,
    ) {

        require(dueLimit >= 0) {
            "dueLimit must be >= 0"
        }

        require(newLimit >= 0) {
            "newLimit must be >= 0"
        }

        if (
            _uiState.value != ReviewSessionUiState.Idle
        ) {
            return
        }

        _uiState.value = ReviewSessionUiState.Loading

        viewModelScope.launch {

            try {

                val queue =
                    reviewRepository
                        .getReviewQueue(
                            now = clock.instant(),

                            dueLimit = dueLimit,

                            newLimit = newLimit,
                        )

                val lexemeIds = queue.map {
                    it.lexemeId
                }

                check(
                    lexemeIds.size ==
                            lexemeIds
                                .toSet()
                                .size
                ) {
                    "Review queue contains duplicate lexemeId"
                }

                pendingLexemeIds.clear()
                pendingLexemeIds.addAll(
                    lexemeIds
                )

                reviewedCount = 0

                loadNextCard()

            } catch (
                cancellation:
                CancellationException,
            ) {

                throw cancellation

            } catch (_: Exception) {

                _uiState.value =
                    ReviewSessionUiState.Failed(
                        error =
                            ReviewSessionError
                                .LOAD_FAILED,
                    )
            }
        }
    }

    fun revealAnswer() {

        val current =
            _uiState.value
                    as? ReviewSessionUiState.Active
                ?: return

        if (
            current.isSubmitting ||
            current.isAnswerRevealed
        ) {
            return
        }

        _uiState.value =
            current.copy(
                isAnswerRevealed = true,
                error = null,
            )
    }

    fun submitRating(
        rating: ReviewRating,
    ) {

        val current =
            _uiState.value
                    as? ReviewSessionUiState.Active
                ?: return

        if (
            !current.isAnswerRevealed ||
            current.isSubmitting
        ) {
            return
        }

        _uiState.value =
            current.copy(
                isSubmitting = true,
                error = null,
            )

        viewModelScope.launch {

            try {

                when (
                    val result =
                        submitReviewUseCase(
                            lexemeId =
                                current
                                    .card
                                    .lexeme
                                    .id,

                            rating =
                                rating,

                            reviewedAt =
                                clock.instant(),
                        )
                ) {

                    is SaveReviewResult.Saved -> {

                        reviewedCount += 1

                        loadNextCard()
                    }

                    SaveReviewResult.LexemeNotFound -> {
                        loadNextCard()
                    }

                    is SaveReviewResult.StaleProgress -> {
                        reloadCurrentAfterStale(
                            lexemeId =
                                current
                                    .card
                                    .lexeme
                                    .id,
                        )
                    }
                }

            } catch (
                cancellation:
                CancellationException,
            ) {

                throw cancellation

            } catch (_: Exception) {

                val latest =
                    _uiState.value

                if (
                    latest is
                            ReviewSessionUiState.Active
                ) {

                    _uiState.value =
                        latest.copy(
                            isSubmitting =
                                false,

                            error =
                                ReviewSessionError
                                    .SAVE_FAILED,
                        )
                }
            }
        }
    }

    private suspend fun loadNextCard() {

        while (
            pendingLexemeIds.isNotEmpty()
        ) {

            val lexemeId =
                pendingLexemeIds
                    .removeFirst()

            val card =
                reviewRepository
                    .getReviewCard(
                        lexemeId
                    )
                    ?: continue

            _uiState.value =
                ReviewSessionUiState.Active(
                    card = card,

                    isAnswerRevealed =
                        false,

                    isSubmitting =
                        false,

                    reviewedCount =
                        reviewedCount,

                    /*
                     * +1 = current card.
                     */
                    remainingCount =
                        pendingLexemeIds.size +
                                1,

                    error =
                        null,
                )

            return
        }

        _uiState.value =
            ReviewSessionUiState.Completed(
                reviewedCount =
                    reviewedCount,
            )
    }

    private suspend fun reloadCurrentAfterStale(
        lexemeId: Long,
    ) {

        val card =
            reviewRepository
                .getReviewCard(
                    lexemeId
                )

        if (card == null) {

            loadNextCard()
            return
        }

        _uiState.value =
            ReviewSessionUiState.Active(
                card =
                    card,
                
                isAnswerRevealed =
                    false,

                isSubmitting =
                    false,

                reviewedCount =
                    reviewedCount,

                remainingCount =
                    pendingLexemeIds.size +
                            1,

                error =
                    ReviewSessionError
                        .STALE_PROGRESS,
            )
    }

    class Factory(
        private val reviewRepository:
        ReviewRepository,

        private val submitReviewUseCase:
        SubmitReviewUseCase,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel>
                create(
            modelClass: Class<T>,
        ): T {

            require(
                modelClass.isAssignableFrom(
                    ReviewSessionViewModel::
                    class.java
                )
            ) {
                "Unknown ViewModel class: " +
                        modelClass.name
            }

            return ReviewSessionViewModel(
                reviewRepository =
                    reviewRepository,

                submitReviewUseCase =
                    submitReviewUseCase,
            ) as T
        }
    }
}