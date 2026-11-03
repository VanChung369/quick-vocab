package com.chung.lexis.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.chung.lexis.core.database.entity.LearningProgressEntity
import com.chung.lexis.core.database.entity.ReviewLogEntity
import com.chung.lexis.core.database.entity.ReviewQuarantineEntity
import com.chung.lexis.core.database.entity.ReviewStateColumn

/*
 * Mọi query queue / summary chỉ lấy lexeme học được:
 *
 * - thuộc ít nhất một deck
 * - có ít nhất một meaning (flashcard phải có đáp án)
 * - không bị cách ly vì data hỏng
 */
@Dao
interface ReviewDao {

    @Upsert
    suspend fun upsertProgress(
        progress: LearningProgressEntity,
    )

    @Query(
        """
        SELECT *
        FROM learning_progress
        WHERE lexemeId = :lexemeId
        LIMIT 1
        """
    )
    suspend fun getProgress(
        lexemeId: Long,
    ): LearningProgressEntity?

    @Insert
    suspend fun insertReviewLog(
        reviewLog: ReviewLogEntity,
    ): Long

    @Query(
        """
        SELECT *
        FROM review_logs
        WHERE lexemeId = :lexemeId
        ORDER BY reviewedAtEpochMillis DESC, id DESC
        """
    )
    suspend fun getReviewHistory(
        lexemeId: Long,
    ): List<ReviewLogEntity>

    /*
     * Số lexeme có review đầu tiên (rời NEW)
     * từ thời điểm cho trước.
     */
    @Query(
        """
        SELECT COUNT(DISTINCT lexemeId)
        FROM review_logs
        WHERE stateBefore = :newState
          AND reviewedAtEpochMillis >= :sinceEpochMillis
        """
    )
    suspend fun countFirstReviewsSince(
        sinceEpochMillis: Long,
        newState: ReviewStateColumn,
    ): Int

    @Upsert
    suspend fun upsertQuarantine(
        quarantine: ReviewQuarantineEntity,
    )


    @Query(
        """
    SELECT p.*
    FROM learning_progress AS p
    WHERE p.state IN (
        :learningState,
        :relearningState
    )
      AND p.nextReviewAtEpochMillis IS NOT NULL
      AND p.nextReviewAtEpochMillis <= :nowEpochMillis
      AND EXISTS (
          SELECT 1
          FROM deck_items AS di
          WHERE di.lexemeId = p.lexemeId
      )
      AND EXISTS (
          SELECT 1
          FROM meanings AS m
          WHERE m.lexemeId = p.lexemeId
      )
      AND NOT EXISTS (
          SELECT 1
          FROM review_quarantine AS q
          WHERE q.lexemeId = p.lexemeId
      )
    ORDER BY
        p.nextReviewAtEpochMillis ASC,
        p.lexemeId ASC
    LIMIT :limit
    """
    )
    suspend fun getDueShortTerm(
        nowEpochMillis: Long,
        learningState: ReviewStateColumn,
        relearningState: ReviewStateColumn,
        limit: Int,
    ): List<LearningProgressEntity>

    @Query(
        """
    SELECT p.*
    FROM learning_progress AS p
    WHERE p.state IN (
        :learningState,
        :relearningState
    )
      AND p.nextReviewAtEpochMillis IS NOT NULL
      AND p.nextReviewAtEpochMillis > :nowEpochMillis
      AND EXISTS (
          SELECT 1
          FROM deck_items AS di
          WHERE di.lexemeId = p.lexemeId
      )
      AND EXISTS (
          SELECT 1
          FROM meanings AS m
          WHERE m.lexemeId = p.lexemeId
      )
      AND NOT EXISTS (
          SELECT 1
          FROM review_quarantine AS q
          WHERE q.lexemeId = p.lexemeId
      )
    ORDER BY
        p.nextReviewAtEpochMillis ASC,
        p.lexemeId ASC
    """
    )
    suspend fun getUpcomingShortTerm(
        nowEpochMillis: Long,
        learningState: ReviewStateColumn,
        relearningState: ReviewStateColumn,
    ): List<LearningProgressEntity>


    @Query(
        """
    SELECT p.*
    FROM learning_progress AS p
    WHERE p.state = :reviewState
      AND p.nextReviewAtEpochMillis IS NOT NULL
      AND p.nextReviewAtEpochMillis <= :nowEpochMillis
      AND EXISTS (
          SELECT 1
          FROM deck_items AS di
          WHERE di.lexemeId = p.lexemeId
      )
      AND EXISTS (
          SELECT 1
          FROM meanings AS m
          WHERE m.lexemeId = p.lexemeId
      )
      AND NOT EXISTS (
          SELECT 1
          FROM review_quarantine AS q
          WHERE q.lexemeId = p.lexemeId
      )
    ORDER BY
        p.nextReviewAtEpochMillis ASC,
        p.lexemeId ASC
    LIMIT :limit
    """
    )
    suspend fun getDueReview(
        nowEpochMillis: Long,
        reviewState: ReviewStateColumn,
        limit: Int,
    ): List<LearningProgressEntity>

    /*
     * Học được = có meaning, không phụ thuộc
     * enrichmentStatus: từ user tự nhập nghĩa
     * học được ngay khi enrichment còn chạy.
     */
    @Query(
        """
    SELECT l.id
    FROM lexemes AS l

    INNER JOIN deck_items AS di
        ON di.lexemeId = l.id

    LEFT JOIN learning_progress AS p
        ON p.lexemeId = l.id

    WHERE (
          p.lexemeId IS NULL
          OR p.state = :newState
      )
      AND EXISTS (
          SELECT 1
          FROM meanings AS m
          WHERE m.lexemeId = l.id
      )
      AND NOT EXISTS (
          SELECT 1
          FROM review_quarantine AS q
          WHERE q.lexemeId = l.id
      )

    GROUP BY l.id

    ORDER BY
        MIN(di.addedAtEpochMillis) ASC,
        l.id ASC

    LIMIT :limit
    """
    )
    suspend fun getNewLexemeIds(
        newState: ReviewStateColumn,
        limit: Int,
    ): List<Long>

    @Query(
        """
    SELECT COUNT(*)
    FROM learning_progress AS p
    WHERE p.state IN (
        :learningState,
        :relearningState
    )
      AND p.nextReviewAtEpochMillis IS NOT NULL
      AND p.nextReviewAtEpochMillis <= :nowEpochMillis
      AND EXISTS (
          SELECT 1
          FROM deck_items AS di
          WHERE di.lexemeId = p.lexemeId
      )
      AND EXISTS (
          SELECT 1
          FROM meanings AS m
          WHERE m.lexemeId = p.lexemeId
      )
      AND NOT EXISTS (
          SELECT 1
          FROM review_quarantine AS q
          WHERE q.lexemeId = p.lexemeId
      )
    """
    )
    suspend fun countDueShortTerm(
        nowEpochMillis: Long,
        learningState: ReviewStateColumn,
        relearningState: ReviewStateColumn,
    ): Int

    @Query(
        """
    SELECT COUNT(*)
    FROM learning_progress AS p
    WHERE p.state = :reviewState
      AND p.nextReviewAtEpochMillis IS NOT NULL
      AND p.nextReviewAtEpochMillis <= :nowEpochMillis
      AND EXISTS (
          SELECT 1
          FROM deck_items AS di
          WHERE di.lexemeId = p.lexemeId
      )
      AND EXISTS (
          SELECT 1
          FROM meanings AS m
          WHERE m.lexemeId = p.lexemeId
      )
      AND NOT EXISTS (
          SELECT 1
          FROM review_quarantine AS q
          WHERE q.lexemeId = p.lexemeId
      )
    """
    )
    suspend fun countDueReview(
        nowEpochMillis: Long,
        reviewState: ReviewStateColumn,
    ): Int

    @Query(
        """
    SELECT COUNT(DISTINCT l.id)
    FROM lexemes AS l

    INNER JOIN deck_items AS di
        ON di.lexemeId = l.id

    LEFT JOIN learning_progress AS p
        ON p.lexemeId = l.id

    WHERE (
          p.lexemeId IS NULL
          OR p.state = :newState
      )
      AND EXISTS (
          SELECT 1
          FROM meanings AS m
          WHERE m.lexemeId = l.id
      )
      AND NOT EXISTS (
          SELECT 1
          FROM review_quarantine AS q
          WHERE q.lexemeId = l.id
      )
    """
    )
    suspend fun countNewAvailable(
        newState: ReviewStateColumn,
    ): Int

    @Query(
        """
    SELECT MIN(
        p.nextReviewAtEpochMillis
    )
    FROM learning_progress AS p

    WHERE p.state IN (
        :learningState,
        :relearningState,
        :reviewState
    )

      AND p.nextReviewAtEpochMillis
          IS NOT NULL

      AND p.nextReviewAtEpochMillis
          > :nowEpochMillis

      AND EXISTS (
          SELECT 1
          FROM deck_items AS di
          WHERE di.lexemeId =
                p.lexemeId
      )

      AND EXISTS (
          SELECT 1
          FROM meanings AS m
          WHERE m.lexemeId =
                p.lexemeId
      )

      AND NOT EXISTS (
          SELECT 1
          FROM review_quarantine AS q
          WHERE q.lexemeId =
                p.lexemeId
      )
    """
    )
    suspend fun getNextDueAtAfter(
        nowEpochMillis: Long,
        learningState:
        ReviewStateColumn,
        relearningState:
        ReviewStateColumn,
        reviewState:
        ReviewStateColumn,
    ): Long?
}
