package com.chung.lexis.domain.scheduler

import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.model.ReviewState
import java.time.Duration
import java.time.Instant

class DefaultReviewScheduler(
    private val stateMachine: ReviewStateMachine,
    private val stepPolicy: LearningStepPolicy,
    private val memoryPolicy: ReviewMemoryPolicy,
) : ReviewScheduler {

    override fun schedule(
        current: LearningProgress,
        rating: ReviewRating,
        reviewedAt: Instant,
    ): ReviewSchedule {

        validateCurrent(
            current = current,
            reviewedAt = reviewedAt,
        )

        val elapsedDays =
            calculateElapsedDays(
                lastReviewedAt = current.lastReviewedAt,
                reviewedAt = reviewedAt,
            )

        /*
         * REVIEW không sử dụng LearningStepPolicy.
         *
         * Với NEW / LEARNING / RELEARNING,
         * ta cần biết user còn learning step hay đã Graduate.
         */
        val currentStepDecision =
            if (current.state == ReviewState.REVIEW) {
                null
            } else {
                stepPolicy.decide(
                    phase = current.state,
                    currentStep = current.step,
                    rating = rating,
                )
            }

        /*
         * Chỉ GOOD + Graduate mới mang nghĩa
         * "đã hoàn thành learning steps".
         *
         * EASY tự graduate theo state machine.
         */
        val learningStepCompleted =
            rating == ReviewRating.GOOD &&
                    currentStepDecision is
                            LearningStepDecision.Graduate

        val transition =
            stateMachine.transition(
                currentState = current.state,
                rating = rating,
                learningStepCompleted =
                    learningStepCompleted,
            )

        /*
         * Memory model được update ở mọi review.
         *
         * Nếu vẫn đang learning/relearning,
         * intervalDays chưa được dùng làm nextReviewAt,
         * nhưng stability/difficulty vẫn được cập nhật.
         */
        val memory =
            memoryPolicy.evaluate(
                current = current,
                rating = rating,
                elapsedDays = elapsedDays,
            )

        val due =
            when (transition.nextState) {

                ReviewState.LEARNING ->
                    shortTermDue(
                        reviewedAt = reviewedAt,
                        decision =
                            requireSchedule(
                                currentStepDecision
                            ),
                    )

                ReviewState.RELEARNING -> {
                    /*
                     * REVIEW + AGAIN là case đặc biệt:
                     *
                     * current đang REVIEW nên phía trên
                     * chưa có stepDecision.
                     *
                     * Bây giờ vừa chuyển sang RELEARNING,
                     * ta tạo relearning step đầu tiên.
                     */
                    val decision =
                        if (
                            current.state ==
                            ReviewState.REVIEW
                        ) {
                            stepPolicy.decide(
                                phase =
                                    ReviewState.RELEARNING,
                                currentStep = null,
                                rating = rating,
                            )
                        } else {
                            currentStepDecision
                        }

                    shortTermDue(
                        reviewedAt = reviewedAt,
                        decision =
                            requireSchedule(decision),
                    )
                }

                ReviewState.REVIEW ->
                    longTermDue(
                        reviewedAt = reviewedAt,
                        intervalDays =
                            memory.intervalDays,
                    )

                ReviewState.NEW ->
                    error(
                        "A completed review cannot return to NEW"
                    )
            }

        val progress =
            current.copy(
                state = transition.nextState,

                step = due.step,

                stability = memory.stability,
                difficulty = memory.difficulty,

                reviewCount =
                    current.reviewCount + 1,

                lapseCount =
                    current.lapseCount +
                            if (transition.isLapse) {
                                1
                            } else {
                                0
                            },

                scheduledDays =
                    due.scheduledDays,

                lastReviewedAt =
                    reviewedAt,

                nextReviewAt =
                    due.nextReviewAt,
            )

        return ReviewSchedule(
            progress = progress,
            elapsedDays = elapsedDays,
            scheduledDays = due.scheduledDays,
        )
    }

    private fun shortTermDue(
        reviewedAt: Instant,
        decision: LearningStepDecision.Schedule,
    ): DueSchedule =
        DueSchedule(
            step = decision.step,
            nextReviewAt =
                reviewedAt.plus(decision.delay),
            scheduledDays =
                durationToDays(decision.delay),
        )

    private fun longTermDue(
        reviewedAt: Instant,
        intervalDays: Int,
    ): DueSchedule =
        DueSchedule(
            step = null,
            nextReviewAt =
                reviewedAt.plus(
                    Duration.ofDays(
                        intervalDays.toLong()
                    )
                ),
            scheduledDays = intervalDays,
        )

    private fun requireSchedule(
        decision: LearningStepDecision?,
    ): LearningStepDecision.Schedule =
        requireNotNull(
            decision as? LearningStepDecision.Schedule
        ) {
            "Expected a learning step schedule"
        }

    private fun validateCurrent(
        current: LearningProgress,
        reviewedAt: Instant,
    ) {
        current.lastReviewedAt?.let { last ->
            require(!reviewedAt.isBefore(last)) {
                "reviewedAt cannot be before lastReviewedAt"
            }
        }

        if (
            current.state == ReviewState.NEW ||
            current.state == ReviewState.REVIEW
        ) {
            require(current.step == null) {
                "${current.state} must not have a learning step"
            }
        }
    }

    private fun calculateElapsedDays(
        lastReviewedAt: Instant?,
        reviewedAt: Instant,
    ): Int {

        if (lastReviewedAt == null) {
            return 0
        }

        val days =
            Duration
                .between(
                    lastReviewedAt,
                    reviewedAt,
                )
                .toDays()

        require(days <= Int.MAX_VALUE) {
            "elapsedDays exceeds Int range"
        }

        return days.toInt()
    }

    private fun durationToDays(
        duration: Duration,
    ): Int {

        val days =
            duration.toDays()

        require(days <= Int.MAX_VALUE) {
            "scheduledDays exceeds Int range"
        }

        return days.toInt()
    }
}

private data class DueSchedule(
    val step: Int?,
    val nextReviewAt: Instant,
    val scheduledDays: Int,
)