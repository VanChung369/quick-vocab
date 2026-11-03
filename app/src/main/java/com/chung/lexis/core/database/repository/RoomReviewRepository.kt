package com.chung.lexis.core.database.repository

import androidx.room.withTransaction
import com.chung.lexis.core.database.LexisDatabase
import com.chung.lexis.core.database.entity.ReviewQuarantineEntity
import com.chung.lexis.core.database.entity.ReviewStateColumn
import com.chung.lexis.core.database.mapper.toDomain
import com.chung.lexis.core.database.mapper.toDomainResult
import com.chung.lexis.core.database.mapper.toEntity
import com.chung.lexis.domain.model.CorruptReviewDataException
import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.ReviewCard
import com.chung.lexis.domain.model.ReviewDueSummary
import com.chung.lexis.domain.model.ReviewLog
import com.chung.lexis.domain.model.requireReviewed
import com.chung.lexis.domain.repository.ReviewRepository
import com.chung.lexis.domain.repository.SaveReviewResult
import com.chung.lexis.domain.repository.UndoReviewResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Instant

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

    override suspend fun undoReview(
        previousProgress: LearningProgress,
        savedProgress: LearningProgress,
        reviewLog: ReviewLog,
    ): UndoReviewResult =
        database.withTransaction {

            val lexemeId =
                reviewLog.lexemeId

            require(reviewLog.id != 0L) {
                "Only a saved ReviewLog can be undone"
            }

            require(
                previousProgress.lexemeId == lexemeId &&
                        savedProgress.lexemeId == lexemeId
            ) {
                "Progress and ReviewLog must belong to the same lexeme"
            }

            val logEntity =
                reviewDao.getReviewLog(
                    reviewLog.id
                )

            if (
                lexemeDao.getLexeme(lexemeId) == null ||
                logEntity == null ||
                logEntity.lexemeId != lexemeId
            ) {
                return@withTransaction UndoReviewResult.ReviewNotFound
            }

            val currentProgress =
                reviewDao
                    .getProgress(lexemeId)
                    ?.toDomain()
                    ?: LearningProgress(
                        lexemeId = lexemeId,
                    )

            /*
             * Canonicalize như saveReview.
             */
            if (
                currentProgress !=
                savedProgress
                    .toEntity()
                    .toDomain()
            ) {
                return@withTransaction UndoReviewResult.StaleProgress(
                    current = currentProgress,
                )
            }

            /*
             * Logical NEW (chưa có row) được lưu thành
             * row NEW: queue vẫn coi là từ mới.
             */
            reviewDao.upsertProgress(
                previousProgress.toEntity()
            )

            reviewDao.deleteReviewLog(
                reviewLog.id
            )

            UndoReviewResult.Undone(
                progress =
                    reviewDao
                        .getProgress(lexemeId)
                        ?.toDomain()
                        ?: error(
                            "LearningProgress $lexemeId " +
                                    "was not found after undo"
                        ),
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

    override suspend fun countNewCardsStartedSince(
        since: Instant,
    ): Int =
        reviewDao.countFirstReviewsSince(
            sinceEpochMillis =
                since.toEpochMilli(),

            newState =
                ReviewStateColumn.NEW,
        )

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

        /*
         * Không lưu progress mà lần đọc sau
         * coi là hỏng hoặc không due query nào thấy.
         */
        nextProgress.requireReviewed()
    }

    override suspend fun getReviewQueue(
        now: Instant,
        dueLimit: Int,
        newLimit: Int,
        deckId: Long?,
    ): List<LearningProgress> {

        require(dueLimit >= 0) {
            "dueLimit must be >= 0"
        }

        require(newLimit >= 0) {
            "newLimit must be >= 0"
        }

        return database.withTransaction {

            val nowEpochMillis =
                now.toEpochMilli()

            val shortTerm =
                if (dueLimit == 0) {
                    emptyList()
                } else {
                    reviewDao.getDueShortTerm(
                        nowEpochMillis =
                            nowEpochMillis,

                        learningState =
                            ReviewStateColumn.LEARNING,

                        relearningState =
                            ReviewStateColumn.RELEARNING,

                        limit =
                            dueLimit,

                        deckId =
                            deckId,
                    )
                }

            /*
             * Short-term cards get first priority.
             *
             * REVIEW only receives the remaining slots.
             */
            val reviewSlots =
                dueLimit -
                        shortTerm.size

            val review =
                if (reviewSlots == 0) {
                    emptyList()
                } else {
                    reviewDao.getDueReview(
                        nowEpochMillis =
                            nowEpochMillis,

                        reviewState =
                            ReviewStateColumn.REVIEW,

                        limit =
                            reviewSlots,

                        deckId =
                            deckId,
                    )
                }

            val newLexemeIds =
                if (newLimit == 0) {
                    emptyList()
                } else {
                    reviewDao.getNewLexemeIds(
                        newState =
                            ReviewStateColumn.NEW,

                        limit =
                            newLimit,

                        deckId =
                            deckId,
                    )
                }

            buildList {

                addAll(
                    shortTerm.map { entity ->
                        entity.toDomain()
                    }
                )

                addAll(
                    review.map { entity ->
                        entity.toDomain()
                    }
                )

                /*
                 * Missing LearningProgress row is
                 * intentionally represented as logical NEW.
                 */
                addAll(
                    newLexemeIds.map { lexemeId ->
                        LearningProgress(
                            lexemeId = lexemeId,
                        )
                    }
                )
            }
        }
    }

    override suspend fun getUpcomingShortTerm(
        now: Instant,
        deckId: Long?,
    ): List<LearningProgress> =
        reviewDao
            .getUpcomingShortTerm(
                nowEpochMillis =
                    now.toEpochMilli(),

                learningState =
                    ReviewStateColumn.LEARNING,

                relearningState =
                    ReviewStateColumn.RELEARNING,

                deckId =
                    deckId,
            )
            .map { entity ->
                entity.toDomain()
            }

    override suspend fun getReviewDueSummary(
        now: Instant,
        deckId: Long?,
    ): ReviewDueSummary =
        database.withTransaction {

            val nowEpochMillis =
                now.toEpochMilli()

            val shortTermDue =
                reviewDao.countDueShortTerm(
                    nowEpochMillis =
                        nowEpochMillis,

                    learningState =
                        ReviewStateColumn.LEARNING,

                    relearningState =
                        ReviewStateColumn.RELEARNING,

                    deckId =
                        deckId,
                )

            val reviewDue =
                reviewDao.countDueReview(
                    nowEpochMillis =
                        nowEpochMillis,

                    reviewState =
                        ReviewStateColumn.REVIEW,

                    deckId =
                        deckId,
                )

            val newAvailable =
                reviewDao.countNewAvailable(
                    newState =
                        ReviewStateColumn.NEW,

                    deckId =
                        deckId,
                )

            ReviewDueSummary(
                shortTermDue =
                    shortTermDue,

                reviewDue =
                    reviewDue,

                newAvailable =
                    newAvailable,
            )
        }

    @OptIn(
        ExperimentalCoroutinesApi::class
    )
    override fun observeReviewDueSummary(
        clock: Clock,
        deckId: Long?,
    ): Flow<ReviewDueSummary> =

        database
            .invalidationTracker
            .createFlow(
                "learning_progress",
                "deck_items",
                "lexemes",
                "meanings",
                "review_quarantine",
            )
            .flatMapLatest {
                flow {

                    while (true) {

                        val now =
                            clock.instant()

                        /*
                         * 1. Emit summary hiện tại.
                         */
                        emit(
                            getReviewDueSummary(
                                now = now,
                                deckId = deckId,
                            )
                        )

                        /*
                         * 2. Tìm thời điểm tương lai
                         * gần nhất mà badge có thể thay đổi.
                         */
                        val nextDueAtEpochMillis =
                            reviewDao
                                .getNextDueAtAfter(
                                    nowEpochMillis =
                                        now.toEpochMilli(),

                                    learningState =
                                        ReviewStateColumn.LEARNING,

                                    relearningState =
                                        ReviewStateColumn.RELEARNING,

                                    reviewState =
                                        ReviewStateColumn.REVIEW,

                                    deckId =
                                        deckId,
                                )

                        /*
                         * Không có card nào sắp due.
                         *
                         * Không polling.
                         *
                         * Flow ngủ vô thời hạn cho tới khi
                         * Room table bị thay đổi; flatMapLatest
                         * sẽ cancel flow này và tạo flow mới.
                         */
                        if (
                            nextDueAtEpochMillis ==
                            null
                        ) {
                            awaitCancellation()
                        }

                        /*
                         * Clock có thể đã chạy thêm vài ms
                         * trong lúc query DB.
                         */
                        val delayMillis =
                            (
                                    nextDueAtEpochMillis -
                                            clock
                                                .instant()
                                                .toEpochMilli()
                                    )
                                .coerceAtLeast(1L)

                        /*
                         * Ngủ chính xác tới card sắp due.
                         */
                        delay(
                            delayMillis
                        )
                    }
                }
            }
            .distinctUntilChanged()

    override suspend fun getReviewCard(
        lexemeId: Long,
    ): ReviewCard? =
        database.withTransaction {

            val lexemeEntity =
                lexemeDao.getLexeme(
                    lexemeId
                ) ?: return@withTransaction null

            val meaningEntities =
                lexemeDao.getMeanings(
                    lexemeId
                )

            val pronunciationEntities =
                lexemeDao.getPronunciations(
                    lexemeId
                )

            val exampleRelations =
                lexemeDao.getExamples(
                    lexemeId
                )

            val progressEntity =
                reviewDao.getProgress(
                    lexemeId
                )

            /*
             * Đọc DB xong mới map.
             *
             * Phần dưới không có DB call nên
             * require/check fail chắc chắn là data hỏng,
             * không phải lỗi IO tạm thời.
             */
            try {

                val lexeme =
                    lexemeEntity
                        .toDomainResult()
                        .getOrElse { cause ->

                            throw IllegalStateException(
                                "Cannot map lexeme $lexemeId",
                                cause,
                            )
                        }

                ReviewCard(
                    lexeme =
                        lexeme,

                    meanings =
                        meaningEntities.map { entity ->
                            entity.toDomain()
                        },

                    pronunciations =
                        pronunciationEntities.map { entity ->
                            entity.toDomain()
                        },

                    examples =
                        exampleRelations.map { relation ->

                            relation.example.toDomain(
                                translation =
                                    relation.translation,
                            )
                        },

                    progress =
                        progressEntity
                            ?.toDomain()
                            ?: LearningProgress(
                                lexemeId = lexemeId,
                            ),
                )

            } catch (
                invalid: IllegalArgumentException,
            ) {
                throw CorruptReviewDataException(
                    lexemeId = lexemeId,
                    cause = invalid,
                )
            } catch (
                invalid: IllegalStateException,
            ) {
                throw CorruptReviewDataException(
                    lexemeId = lexemeId,
                    cause = invalid,
                )
            }
        }

    override suspend fun quarantineReviewCard(
        lexemeId: Long,
        reason: String,
        detectedAt: Instant,
    ) {
        reviewDao.upsertQuarantine(
            ReviewQuarantineEntity(
                lexemeId = lexemeId,
                reason = reason,
                detectedAtEpochMillis =
                    detectedAt.toEpochMilli(),
            )
        )
    }
}