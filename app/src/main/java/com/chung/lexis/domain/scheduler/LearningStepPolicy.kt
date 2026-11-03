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

        require(phase != ReviewState.REVIEW) {
            "REVIEW does not use learning steps"
        }

        if (phase == ReviewState.NEW) {
            require(currentStep == null) {
                "NEW state must not have a current step"
            }
        }

        val steps =
            if (phase == ReviewState.RELEARNING) {
                relearningSteps
            } else {
                learningSteps
            }

        validateCurrentStep(
            currentStep = currentStep,
        )

        /*
         * Chưa có step (NEW, hoặc vừa REVIEW → RELEARNING)
         * → coi như đang ở step 0, giống py-fsrs:
         * GOOD lần đầu sang step 1 thay vì lặp step 0.
         *
         * Step đã lưu có thể nằm ngoài cấu hình hiện tại
         * (vd. rút ngắn learning steps khi card đang ở step cũ).
         *
         * Coi như đang ở step cuối:
         * GOOD → Graduate, HARD → lặp step cuối.
         */
        val step =
            (currentStep ?: 0).coerceAtMost(
                steps.lastIndex
            )

        return when (rating) {

            ReviewRating.AGAIN ->
                schedule(
                    step = 0,
                    steps = steps,
                )

            ReviewRating.HARD ->
                hard(
                    step = step,
                    steps = steps,
                )

            ReviewRating.GOOD ->
                good(
                    currentStep = step,
                    steps = steps,
                )

            ReviewRating.EASY ->
                LearningStepDecision.Graduate
        }
    }

    /*
     * HARD giữ nguyên step.
     *
     * Ở step 0, delay nằm giữa AGAIN và GOOD
     * thay vì trùng AGAIN (giống py-fsrs):
     * - 1 step: 1.5 × step 0
     * - ≥ 2 step: trung bình step 0 và step 1
     */
    private fun hard(
        step: Int,
        steps: List<Duration>,
    ): LearningStepDecision.Schedule {

        if (step != 0) {
            return schedule(
                step = step,
                steps = steps,
            )
        }

        val delay =
            if (steps.size == 1) {
                steps[0]
                    .multipliedBy(3)
                    .dividedBy(2)
            } else {
                steps[0]
                    .plus(steps[1])
                    .dividedBy(2)
            }

        return LearningStepDecision.Schedule(
            step = 0,
            delay = delay,
        )
    }

    private fun good(
        currentStep: Int,
        steps: List<Duration>,
    ): LearningStepDecision {

        val nextStep =
            currentStep + 1

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
    ) {
        if (currentStep == null) {
            return
        }

        require(currentStep >= 0) {
            "currentStep $currentStep must be >= 0"
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