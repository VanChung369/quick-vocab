package com.chung.lexis.domain.model

import java.time.Instant

data class LearningProgress(
    val lexemeId: Long,

    val state: ReviewState = ReviewState.NEW,

    // Index of the current learning step; only set while LEARNING or RELEARNING
    val step: Int? = null,

    val stability: Double? = null,
    val difficulty: Double? = null,

    val reviewCount: Int = 0,
    val lapseCount: Int = 0,

    val scheduledDays: Int = 0,

    val lastReviewedAt: Instant? = null,
    val nextReviewAt: Instant? = null,
)

/**
 * Invariant của progress sau ít nhất một review.
 *
 * Vi phạm → card mất khỏi mọi due query
 * (nextReviewAt null) hoặc scheduler từ chối
 * ở lần review sau (memory state hỏng).
 *
 * @throws IllegalArgumentException nếu vi phạm.
 */
fun LearningProgress.requireReviewed() {

    require(state != ReviewState.NEW) {
        "Reviewed progress $lexemeId cannot be NEW"
    }

    require(reviewCount > 0) {
        "Reviewed progress $lexemeId must have reviewCount > 0"
    }

    require(lapseCount >= 0) {
        "lapseCount of $lexemeId must be >= 0"
    }

    require(scheduledDays >= 0) {
        "scheduledDays of $lexemeId must be >= 0"
    }

    val reviewedAt =
        requireNotNull(lastReviewedAt) {
            "Reviewed progress $lexemeId must have lastReviewedAt"
        }

    val dueAt =
        requireNotNull(nextReviewAt) {
            "$state progress $lexemeId must have nextReviewAt"
        }

    require(!dueAt.isBefore(reviewedAt)) {
        "nextReviewAt of $lexemeId cannot be before lastReviewedAt"
    }

    when (state) {

        ReviewState.LEARNING,
        ReviewState.RELEARNING ->
            require(step != null && step >= 0) {
                "$state progress $lexemeId must have step >= 0"
            }

        ReviewState.REVIEW ->
            require(step == null) {
                "REVIEW progress $lexemeId must not have a step"
            }

        ReviewState.NEW ->
            Unit
    }

    require(
        stability != null &&
                stability.isFinite() &&
                stability > 0.0
    ) {
        "stability of $lexemeId must be finite and > 0"
    }

    require(
        difficulty != null &&
                difficulty.isFinite()
    ) {
        "difficulty of $lexemeId must be finite"
    }
}
