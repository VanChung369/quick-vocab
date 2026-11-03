package com.chung.lexis.core.database.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chung.lexis.core.database.LexisDatabase
import com.chung.lexis.core.database.entity.DeckEntity
import com.chung.lexis.core.database.entity.DeckItemEntity
import com.chung.lexis.core.database.entity.EnrichmentStatusColumn
import com.chung.lexis.core.database.entity.LexemeEntity
import com.chung.lexis.core.database.entity.MeaningEntity
import com.chung.lexis.core.database.entity.MeaningKindColumn
import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.ReviewLog
import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.model.ReviewState
import com.chung.lexis.domain.repository.SaveReviewResult
import com.chung.lexis.domain.repository.UndoReviewResult
import com.chung.lexis.domain.scheduler.DefaultReviewScheduler
import com.chung.lexis.domain.scheduler.Fsrs6MemoryPolicy
import com.chung.lexis.domain.scheduler.LearningStepPolicy
import com.chung.lexis.domain.scheduler.ReviewStateMachine
import com.chung.lexis.domain.usecase.SubmitReviewUseCase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

/*
 * saveReview / undoReview / countNewCardsStartedSince
 * trên Room thật, với scheduler như production.
 */
@RunWith(AndroidJUnit4::class)
class RoomReviewSaveUndoTest {

    private lateinit var database: LexisDatabase
    private lateinit var repository: RoomReviewRepository
    private lateinit var submitReview: SubmitReviewUseCase

    private val now =
        Instant.parse("2026-10-21T12:00:00Z")

    private var deckId = 0L

    @Before
    fun setUp() {

        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        database =
            Room.inMemoryDatabaseBuilder(
                context,
                LexisDatabase::class.java,
            )
                .build()

        repository =
            RoomReviewRepository(database)

        submitReview =
            SubmitReviewUseCase(
                reviewRepository = repository,
                reviewScheduler =
                    DefaultReviewScheduler(
                        stateMachine = ReviewStateMachine(),
                        stepPolicy =
                            LearningStepPolicy(
                                learningSteps = listOf(
                                    Duration.ofMinutes(1),
                                    Duration.ofMinutes(10),
                                ),
                                relearningSteps = listOf(
                                    Duration.ofMinutes(10),
                                ),
                            ),
                        memoryPolicy = Fsrs6MemoryPolicy(),
                    ),
            )

        deckId =
            runBlocking {
                database
                    .deckDao()
                    .insertDeck(
                        DeckEntity(
                            name = "English",
                            learningLanguageTag = LANGUAGE_TAG,
                            translationLanguageTag = "vi",
                            createdAtEpochMillis = now.toEpochMilli(),
                            updatedAtEpochMillis = now.toEpochMilli(),
                        )
                    )
            }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveThenUndoRestoresProgressAndDeletesLog() =
        runBlocking {

            val lexemeId =
                insertStudyableLexeme("apple")

            val previous =
                requireNotNull(
                    repository.getProgress(lexemeId)
                )

            val saved =
                submitReview(
                    progress = previous,
                    rating = ReviewRating.GOOD,
                    reviewedAt = now,
                ) as SaveReviewResult.Saved

            assertEquals(
                ReviewState.LEARNING,
                saved.progress.state,
            )

            assertEquals(
                1,
                repository.countNewCardsStartedSince(
                    now.minus(Duration.ofHours(1))
                ),
            )

            val undo =
                repository.undoReview(
                    previousProgress = previous,
                    savedProgress = saved.progress,
                    reviewLog = saved.reviewLog,
                )

            assertEquals(
                UndoReviewResult.Undone(previous),
                undo,
            )

            assertEquals(
                previous,
                repository.getProgress(lexemeId),
            )

            assertTrue(
                repository
                    .getReviewHistory(lexemeId)
                    .isEmpty()
            )

            /*
             * Suất từ mới của hôm nay được trả lại,
             * và từ lại vào queue như từ mới.
             */
            assertEquals(
                0,
                repository.countNewCardsStartedSince(
                    now.minus(Duration.ofHours(1))
                ),
            )

            assertEquals(
                listOf(lexemeId),
                repository
                    .getReviewQueue(
                        now = now,
                        dueLimit = 10,
                        newLimit = 10,
                    )
                    .map { it.lexemeId },
            )
        }

    @Test
    fun undoAfterAnotherReviewIsStale() =
        runBlocking {

            val lexemeId =
                insertStudyableLexeme("apple")

            val previous =
                requireNotNull(
                    repository.getProgress(lexemeId)
                )

            val first =
                submitReview(
                    progress = previous,
                    rating = ReviewRating.GOOD,
                    reviewedAt = now,
                ) as SaveReviewResult.Saved

            val second =
                submitReview(
                    progress = first.progress,
                    rating = ReviewRating.GOOD,
                    reviewedAt = now.plus(Duration.ofMinutes(10)),
                ) as SaveReviewResult.Saved

            val undo =
                repository.undoReview(
                    previousProgress = previous,
                    savedProgress = first.progress,
                    reviewLog = first.reviewLog,
                )

            assertEquals(
                UndoReviewResult.StaleProgress(
                    current = second.progress,
                ),
                undo,
            )

            assertEquals(
                second.progress,
                repository.getProgress(lexemeId),
            )

            assertEquals(
                2,
                repository
                    .getReviewHistory(lexemeId)
                    .size,
            )
        }

    @Test
    fun undoingTheSameReviewTwiceFindsNoReview() =
        runBlocking {

            val lexemeId =
                insertStudyableLexeme("apple")

            val previous =
                requireNotNull(
                    repository.getProgress(lexemeId)
                )

            val saved =
                submitReview(
                    progress = previous,
                    rating = ReviewRating.AGAIN,
                    reviewedAt = now,
                ) as SaveReviewResult.Saved

            repository.undoReview(
                previousProgress = previous,
                savedProgress = saved.progress,
                reviewLog = saved.reviewLog,
            )

            assertEquals(
                UndoReviewResult.ReviewNotFound,
                repository.undoReview(
                    previousProgress = previous,
                    savedProgress = saved.progress,
                    reviewLog = saved.reviewLog,
                ),
            )
        }

    @Test
    fun countNewCardsStartedSinceCountsOnlyFirstReviews() =
        runBlocking {

            val appleId =
                insertStudyableLexeme("apple")

            val bananaId =
                insertStudyableLexeme("banana")

            val appleFirst =
                submitReview(
                    progress =
                        requireNotNull(repository.getProgress(appleId)),
                    rating = ReviewRating.GOOD,
                    reviewedAt = now,
                ) as SaveReviewResult.Saved

            /*
             * LEARNING → REVIEW: không phải từ mới.
             */
            submitReview(
                progress = appleFirst.progress,
                rating = ReviewRating.GOOD,
                reviewedAt = now.plus(Duration.ofMinutes(10)),
            )

            submitReview(
                progress =
                    requireNotNull(repository.getProgress(bananaId)),
                rating = ReviewRating.GOOD,
                reviewedAt = now.minus(Duration.ofDays(1)),
            )

            assertEquals(
                1,
                repository.countNewCardsStartedSince(
                    now.minus(Duration.ofHours(1))
                ),
            )

            assertEquals(
                2,
                repository.countNewCardsStartedSince(
                    now.minus(Duration.ofDays(2))
                ),
            )

            assertEquals(
                0,
                repository.countNewCardsStartedSince(
                    now.plus(Duration.ofHours(1))
                ),
            )
        }

    @Test
    fun saveReviewRejectsProgressNoDueQueryCouldFind() =
        runBlocking {

            val lexemeId =
                insertStudyableLexeme("apple")

            val previous =
                requireNotNull(
                    repository.getProgress(lexemeId)
                )

            /*
             * LEARNING không có nextReviewAt:
             * lưu vào là card biến mất khỏi mọi queue.
             */
            val invalidNext =
                previous.copy(
                    state = ReviewState.LEARNING,
                    step = 0,
                    stability = 2.3,
                    difficulty = 5.0,
                    reviewCount = 1,
                    lastReviewedAt = now,
                    nextReviewAt = null,
                )

            val error =
                runCatching {
                    repository.saveReview(
                        previousProgress = previous,
                        nextProgress = invalidNext,
                        reviewLog =
                            ReviewLog(
                                lexemeId = lexemeId,
                                rating = ReviewRating.GOOD,
                                stateBefore = ReviewState.NEW,
                                stateAfter = ReviewState.LEARNING,
                                reviewedAt = now,
                                elapsedDays = 0,
                                scheduledDays = 0,
                            ),
                    )
                }.exceptionOrNull()

            assertTrue(
                "Expected IllegalArgumentException but was $error",
                error is IllegalArgumentException
            )

            assertEquals(
                LearningProgress(lexemeId = lexemeId),
                repository.getProgress(lexemeId),
            )

            assertTrue(
                repository
                    .getReviewHistory(lexemeId)
                    .isEmpty()
            )
        }

    /*
     * Lexeme có meaning và thuộc deck → học được.
     */
    private suspend fun insertStudyableLexeme(
        text: String,
    ): Long {

        val lexemeDao =
            database.lexemeDao()

        val lexemeId =
            lexemeDao.insertLexeme(
                LexemeEntity(
                    text = text,
                    normalizedText = text.lowercase(),
                    languageTag = LANGUAGE_TAG,
                    enrichmentStatus = EnrichmentStatusColumn.READY,
                )
            )

        lexemeDao.insertMeanings(
            listOf(
                MeaningEntity(
                    lexemeId = lexemeId,
                    kind = MeaningKindColumn.TRANSLATION,
                    languageTag = "vi",
                    text = "nghĩa của $text",
                )
            )
        )

        database
            .deckDao()
            .insertDeckItem(
                DeckItemEntity(
                    deckId = deckId,
                    lexemeId = lexemeId,
                    languageTag = LANGUAGE_TAG,
                    addedAtEpochMillis = now.toEpochMilli(),
                )
            )

        return lexemeId
    }

    companion object {
        private const val LANGUAGE_TAG =
            "en"
    }
}
