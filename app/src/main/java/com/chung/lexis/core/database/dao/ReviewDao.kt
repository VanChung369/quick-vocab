package com.chung.lexis.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.chung.lexis.core.database.entity.LearningProgressEntity
import com.chung.lexis.core.database.entity.ReviewLogEntity

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
}