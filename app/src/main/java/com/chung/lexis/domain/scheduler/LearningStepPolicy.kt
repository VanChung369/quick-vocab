package com.chung.lexis.domain.scheduler

import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.model.ReviewState
import java.time.Duration

class LearningStepPolicy(
    learningSteps: List<Duration>,
    relearningSteps: List<Duration>,
) {

    private val learningSteps =
        learningSteps.toList()

    private val relearningSteps =
        relearningSteps.toList()

    init {
        require(this.learningSteps.isNotEmpty()) {
            "learningSteps must not be empty"
        }

        require(this.relearningSteps.isNotEmpty()) {
            "relearningSteps must not be empty"
        }

        require(
            this.learningSteps.all { it > Duration.ZERO }
        ) {
            "learningSteps must contain only positive durations"
        }

        require(
            this.relearningSteps.all { it > Duration.ZERO }
        ) {
            "relearningSteps must contain only positive durations"
        }
    }

    fun decide(
        phase: ReviewState,
        currentStep: Int?,
        rating: ReviewRating,
    ): LearningStepDecision {

        val steps =
            when (phase) {

                ReviewState.NEW,
                ReviewState.LEARNING ->
                    learningSteps

                ReviewState.RELEARNING ->
                    relearningSteps

                ReviewState.REVIEW ->
                    error(
                        "REVIEW does not use learning steps"
                    )
            }

        validateCurrentStep(
            currentStep = currentStep,
            steps = steps,
        )

        return when (rating) {

            ReviewRating.AGAIN ->
                schedule(
                    step = 0,
                    steps = steps,
                )

            ReviewRating.HARD ->
                schedule(
                    step = currentStep ?: 0,
                    steps = steps,
                )

            ReviewRating.GOOD ->
                good(
                    currentStep = currentStep,
                    steps = steps,
                )

            ReviewRating.EASY ->
                LearningStepDecision.Graduate
        }
    }

    private fun good(
        currentStep: Int?,
        steps: List<Duration>,
    ): LearningStepDecision {

        val nextStep =
            if (currentStep == null) {
                0
            } else {
                currentStep + 1
            }

        return if (nextStep >= steps.size) {

            LearningStepDecision.Graduate

        } else {

            schedule(
                step = nextStep,
                steps = steps,
            )
        }
    }

    private fun schedule(
        step: Int,
        steps: List<Duration>,
    ): LearningStepDecision.Schedule =
        LearningStepDecision.Schedule(
            step = step,
            delay = steps[step],
        )

    private fun validateCurrentStep(
        currentStep: Int?,
        steps: List<Duration>,
    ) {
        if (currentStep == null) {
            return
        }

        require(currentStep in steps.indices) {
            "currentStep $currentStep is outside configured steps"
        }
    }
}

sealed interface LearningStepDecision {

    data class Schedule(
        val step: Int,
        val delay: Duration,
    ) : LearningStepDecision

    data object Graduate : LearningStepDecision
}