package com.chung.lexis.domain.scheduler

import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.ReviewRating
import java.time.Instant

interface ReviewScheduler {

    fun schedule(
        current: LearningProgress,
        rating: ReviewRating,
        reviewedAt: Instant,
    ): ReviewSchedule
}

data class ReviewSchedule(
    val progress: LearningProgress,
    val elapsedDays: Int,
    val scheduledDays: Int,
)