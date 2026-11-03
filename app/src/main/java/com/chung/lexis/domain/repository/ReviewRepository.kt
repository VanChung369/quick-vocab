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

    /*
     * deckId ở các hàm queue / summary:
     * null = mọi deck, khác null = chỉ card thuộc deck đó.
     */

    suspend fun getReviewQueue(
        now: Instant,
        dueLimit: Int,
        newLimit: Int,
        deckId: Long? = null,
    ): List<LearningProgress>

    /**
     * LEARNING / RELEARNING chưa tới hạn tại [now]
     * (chỉ card thuộc ít nhất một deck, hoặc [deckId]).
     */
    suspend fun getUpcomingShortTerm(
        now: Instant,
        deckId: Long? = null,
    ): List<LearningProgress>

    suspend fun getReviewDueSummary(
        now: Instant,
        deckId: Long? = null,
    ): ReviewDueSummary

    fun observeReviewDueSummary(
        clock: Clock,
        deckId: Long? = null,
    ): Flow<ReviewDueSummary>

    suspend fun saveReview(
        previousProgress: LearningProgress,
        nextProgress: LearningProgress,
        reviewLog: ReviewLog,
    ): SaveReviewResult

    /**
     * Hoàn tác một review đã [saveReview]: progress về
     * [previousProgress], xoá [reviewLog].
     *
     * Compare-and-set như saveReview: progress hiện tại
     * phải đúng [savedProgress], nếu không → StaleProgress.
     */
    suspend fun undoReview(
        previousProgress: LearningProgress,
        savedProgress: LearningProgress,
        reviewLog: ReviewLog,
    ): UndoReviewResult

    suspend fun getReviewHistory(
        lexemeId: Long,
    ): List<ReviewLog>

    /**
     * Số từ mới đã được học lần đầu từ [since]
     * (review log có stateBefore = NEW).
     */
    suspend fun countNewCardsStartedSince(
        since: Instant,
    ): Int

    /**
     * @throws com.chung.lexis.domain.model.CorruptReviewDataException
     * nếu data đã lưu của lexeme vi phạm invariant.
     */
    suspend fun getReviewCard(
        lexemeId: Long,
    ): ReviewCard?

    /**
     * Loại lexeme có review data hỏng khỏi queue và
     * due summary, để session sau không load rồi skip lại.
     */
    suspend fun quarantineReviewCard(
        lexemeId: Long,
        reason: String,
        detectedAt: Instant,
    )
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

sealed interface UndoReviewResult {

    data class Undone(
        val progress: LearningProgress,
    ) : UndoReviewResult

    /*
     * Có write khác sau review cần hoàn tác.
     */
    data class StaleProgress(
        val current: LearningProgress,
    ) : UndoReviewResult

    /*
     * Lexeme hoặc review log không còn.
     */
    data object ReviewNotFound : UndoReviewResult
}