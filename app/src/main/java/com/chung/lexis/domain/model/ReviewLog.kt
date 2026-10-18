package com.chung.lexis.domain.model

import java.time.Instant

data class ReviewLog(
    val id: Long = 0L,

    val lexemeId: Long,

    val rating: ReviewRating,

    val stateBefore: ReviewState,

    val stateAfter: ReviewState,

    val reviewedAt: Instant,

    val elapsedDays: Int,

    val scheduledDays: Int,
)
