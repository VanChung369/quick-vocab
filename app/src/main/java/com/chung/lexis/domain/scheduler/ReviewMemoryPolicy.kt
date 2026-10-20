package com.chung.lexis.domain.scheduler

import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.ReviewRating

interface ReviewMemoryPolicy {

    fun evaluate(
        current: LearningProgress,
        rating: ReviewRating,
        elapsedDays: Int,
    ): ReviewMemoryDecision
}

data class ReviewMemoryDecision(
    val stability: Double,
    val difficulty: Double,
    val intervalDays: Int,
) {
    init {
        require(stability.isFinite() && stability > 0.0) {
            "stability must be finite and > 0"
        }

        require(difficulty.isFinite() && difficulty > 0.0) {
            "difficulty must be finite and > 0"
        }

        require(intervalDays >= 1) {
            "intervalDays must be >= 1"
        }
    }
}