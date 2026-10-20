package com.chung.lexis.core.database.repository

import androidx.room.withTransaction
import com.chung.lexis.core.database.LexisDatabase
import com.chung.lexis.core.database.mapper.toDomain
import com.chung.lexis.core.database.mapper.toEntity
import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.ReviewLog
import com.chung.lexis.domain.repository.ReviewRepository
import com.chung.lexis.domain.repository.SaveReviewResult

class RoomReviewRepository(
    private val database: LexisDatabase,
) : ReviewRepository {

    private val lexemeDao = database.lexemeDao()
    private val reviewDao = database.reviewDao()

    override suspend fun getProgress(
        lexemeId: Long,
    ): LearningProgress? =
        database.withTransaction {

            val lexeme = lexemeDao.getLexeme(
                lexemeId
            ) ?: return@withTransaction null

            reviewDao
                .getProgress(lexeme.id)
                ?.toDomain()
                ?: LearningProgress(
                    lexemeId = lexeme.id,
                )
        }

    override suspend fun saveReview(
        previousProgress: LearningProgress,
        nextProgress: LearningProgress,
        reviewLog: ReviewLog,
    ): SaveReviewResult =
        database.withTransaction {

            validateReview(
                previousProgress = previousProgress,
                nextProgress = nextProgress,
                reviewLog = reviewLog,
            )

            val lexemeId =
                previousProgress.lexemeId

            if (
                lexemeDao.getLexeme(lexemeId) == null
            ) {
                return@withTransaction SaveReviewResult.LexemeNotFound
            }

            val currentProgress =
                reviewDao
                    .getProgress(lexemeId)
                    ?.toDomain()
                    ?: LearningProgress(
                        lexemeId = lexemeId,
                    )

            /*
             * Canonicalize previousProgress through
             * database representation.
             *
             * This also truncates Instant to millis and
             * applies mapper invariants such as step=null
             * for NEW/REVIEW.
             */
            val expectedProgress =
                previousProgress
                    .toEntity()
                    .toDomain()

            if (
                currentProgress != expectedProgress
            ) {
                return@withTransaction SaveReviewResult.StaleProgress(
                    current = currentProgress,
                )
            }

            reviewDao.upsertProgress(
                nextProgress.toEntity()
            )

            val reviewLogEntity =
                reviewLog
                    .toEntity()
                    .copy(id = 0L)

            val reviewLogId =
                reviewDao.insertReviewLog(
                    reviewLogEntity
                )

            val persistedProgress =
                reviewDao
                    .getProgress(lexemeId)
                    ?.toDomain()
                    ?: error(
                        "LearningProgress $lexemeId " +
                                "was not found after upsert"
                    )

            val persistedLog =
                reviewLogEntity
                    .copy(id = reviewLogId)
                    .toDomain()

            SaveReviewResult.Saved(
                progress = persistedProgress,
                reviewLog = persistedLog,
            )
        }

    override suspend fun getReviewHistory(
        lexemeId: Long,
    ): List<ReviewLog> =
        reviewDao
            .getReviewHistory(lexemeId)
            .map { entity ->
                entity.toDomain()
            }

    private fun validateReview(
        previousProgress: LearningProgress,
        nextProgress: LearningProgress,
        reviewLog: ReviewLog,
    ) {
        require(
            previousProgress.lexemeId ==
                    nextProgress.lexemeId
        ) {
            "previousProgress and nextProgress " +
                    "must belong to the same lexeme"
        }

        require(
            previousProgress.lexemeId ==
                    reviewLog.lexemeId
        ) {
            "ReviewLog must belong to the same lexeme"
        }

        require(reviewLog.id == 0L) {
            "A new ReviewLog must not already have an id"
        }

        require(
            reviewLog.stateBefore ==
                    previousProgress.state
        ) {
            "ReviewLog.stateBefore must match " +
                    "previous progress state"
        }

        require(
            reviewLog.stateAfter ==
                    nextProgress.state
        ) {
            "ReviewLog.stateAfter must match " +
                    "next progress state"
        }

        require(
            nextProgress.lastReviewedAt
                ?.toEpochMilli() ==
                    reviewLog.reviewedAt.toEpochMilli()
        ) {
            "nextProgress.lastReviewedAt must match " +
                    "ReviewLog.reviewedAt"
        }
    }
}