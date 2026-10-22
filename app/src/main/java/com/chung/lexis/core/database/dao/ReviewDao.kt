package com.chung.lexis.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.chung.lexis.core.database.entity.EnrichmentStatusColumn
import com.chung.lexis.core.database.entity.LearningProgressEntity
import com.chung.lexis.core.database.entity.ReviewLogEntity
import com.chung.lexis.core.database.entity.ReviewStateColumn

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
    WHERE p.state = :reviewState
      AND p.nextReviewAtEpochMillis IS NOT NULL
      AND p.nextReviewAtEpochMillis <= :nowEpochMillis
      AND EXISTS (
          SELECT 1
          FROM deck_items AS di
          WHERE di.lexemeId = p.lexemeId
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

    @Query(
        """
    SELECT l.id
    FROM lexemes AS l

    INNER JOIN deck_items AS di
        ON di.lexemeId = l.id

    LEFT JOIN learning_progress AS p
        ON p.lexemeId = l.id

    WHERE l.enrichmentStatus = :readyStatus
      AND (
          p.lexemeId IS NULL
          OR p.state = :newState
      )

    GROUP BY l.id

    ORDER BY
        MIN(di.addedAtEpochMillis) ASC,
        l.id ASC

    LIMIT :limit
    """
    )
    suspend fun getNewLexemeIds(
        readyStatus: EnrichmentStatusColumn,
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

    WHERE l.enrichmentStatus = :readyStatus
      AND (
          p.lexemeId IS NULL
          OR p.state = :newState
      )
    """
    )
    suspend fun countNewAvailable(
        readyStatus: EnrichmentStatusColumn,
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