package com.chung.lexis.domain.scheduler

import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.model.ReviewState

class ReviewStateMachine {

    /**
     * @param learningStepCompleted only meaningful for GOOD while in
     * NEW, LEARNING or RELEARNING: true when this answer finishes the
     * last learning step.
     */
    fun transition(
        currentState: ReviewState,
        rating: ReviewRating,
        learningStepCompleted: Boolean = false,
    ): ReviewTransition {

        require(
            !learningStepCompleted ||
                    rating == ReviewRating.GOOD
        ) {
            "learningStepCompleted is only meaningful for GOOD"
        }

        require(
            !learningStepCompleted ||
                    currentState != ReviewState.REVIEW
        ) {
            "learningStepCompleted is not meaningful in REVIEW"
        }

        return when (currentState) {

            ReviewState.NEW,
            ReviewState.LEARNING ->
                fromLearningPhase(
                    rating = rating,
                    learningStepCompleted =
                        learningStepCompleted,
                    stayIn = ReviewState.LEARNING,
                )

            ReviewState.REVIEW ->
                fromReview(
                    rating = rating,
                )

            ReviewState.RELEARNING ->
                fromLearningPhase(
                    rating = rating,
                    learningStepCompleted =
                        learningStepCompleted,
                    stayIn = ReviewState.RELEARNING,
                )
        }

    }

    private fun fromLearningPhase(
        rating: ReviewRating,
        learningStepCompleted: Boolean,
        stayIn: ReviewState,
    ): ReviewTransition =
        when (rating) {

            ReviewRating.AGAIN,
            ReviewRating.HARD ->
                ReviewTransition(
                    nextState = stayIn,
                )

            ReviewRating.GOOD ->
                ReviewTransition(
                    nextState =
                        if (learningStepCompleted) {
                            ReviewState.REVIEW
                        } else {
                            stayIn
                        },
                )

            ReviewRating.EASY ->
                ReviewTransition(
                    nextState = ReviewState.REVIEW,
                )
        }

    private fun fromReview(
        rating: ReviewRating,
    ): ReviewTransition =
        when (rating) {

            ReviewRating.AGAIN ->
                ReviewTransition(
                    nextState = ReviewState.RELEARNING,
                    isLapse = true,
                )

            ReviewRating.HARD,
            ReviewRating.GOOD,
            ReviewRating.EASY ->
                ReviewTransition(
                    nextState = ReviewState.REVIEW,
                )
        }

}

data class ReviewTransition(
    val nextState: ReviewState,
    val isLapse: Boolean = false,
)
