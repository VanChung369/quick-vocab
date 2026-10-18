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
