package com.chung.lexis.domain.repository

import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.ReviewCard
import com.chung.lexis.domain.model.ReviewDueSummary
import com.chung.lexis.domain.model.ReviewLog
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant

interface ReviewRepository {
    suspend fun getProgress(
        lexemeId: Long,
    ): LearningProgress?

    suspend fun getReviewQueue(
        now: Instant,
        dueLimit: Int,
        newLimit: Int,
    ): List<LearningProgress>

    suspend fun getReviewDueSummary(
        now: Instant,
    ): ReviewDueSummary

    fun observeReviewDueSummary(
        clock: Clock,
    ): Flow<ReviewDueSummary>

    suspend fun saveReview(
        previousProgress: LearningProgress,
        nextProgress: LearningProgress,
        reviewLog: ReviewLog,
    ): SaveReviewResult

    suspend fun getReviewHistory(
        lexemeId: Long,
    ): List<ReviewLog>

    suspend fun getReviewCard(
        lexemeId: Long,
    ): ReviewCard?
}


sealed interface SaveReviewResult {

    data class Saved(
        val progress: LearningProgress,
        val reviewLog: ReviewLog,
    ) : SaveReviewResult

    data class StaleProgress(
        val current: LearningProgress,
    ) : SaveReviewResult

    data object LexemeNotFound : SaveReviewResult
}