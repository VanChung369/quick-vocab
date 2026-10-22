package com.chung.lexis.core.database.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chung.lexis.core.database.LexisDatabase
import com.chung.lexis.core.database.entity.DeckEntity
import com.chung.lexis.core.database.entity.DeckItemEntity
import com.chung.lexis.core.database.entity.EnrichmentStatusColumn
import com.chung.lexis.core.database.entity.LearningProgressEntity
import com.chung.lexis.core.database.entity.LexemeEntity
import com.chung.lexis.core.database.entity.ReviewStateColumn
import com.chung.lexis.domain.model.ReviewState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class RoomReviewQueueTest {

    private lateinit var database: LexisDatabase
    private lateinit var repository: RoomReviewRepository

    private val now =
        Instant.parse("2026-10-21T12:00:00Z")

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
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sameLexemeInTwoDecksAppearsOnlyOnce() =
        runBlocking {

            val deck1 =
                insertDeck("English Basic")

            val deck2 =
                insertDeck("IELTS")

            val lexemeId =
                insertLexeme(
                    text = "apple",
                )

            addToDeck(
                deckId = deck1,
                lexemeId = lexemeId,
            )

            addToDeck(
                deckId = deck2,
                lexemeId = lexemeId,
            )

            insertProgress(
                lexemeId = lexemeId,
                state = ReviewStateColumn.REVIEW,
                dueAt =
                    now.minus(
                        Duration.ofDays(1),
                    ),
            )

            val queue =
                repository.getReviewQueue(
                    now = now,
                    dueLimit = 10,
                    newLimit = 0,
                )

            assertEquals(
                1,
                queue.size,
            )

            assertEquals(
                lexemeId,
                queue.single().lexemeId,
            )
        }

    @Test
    fun shortTermCardsArePrioritizedBeforeReviewCards() =
        runBlocking {

            val deckId =
                insertDeck("English")

            val reviewLexemeId =
                insertLexeme("book")

            val learningLexemeId =
                insertLexeme("apple")

            addToDeck(
                deckId = deckId,
                lexemeId = reviewLexemeId,
            )

            addToDeck(
                deckId = deckId,
                lexemeId = learningLexemeId,
            )

            /*
             * REVIEW này overdue lâu hơn.
             *
             * Nhưng LEARNING vẫn phải đứng trước
             * vì short-term có priority cao hơn.
             */
            insertProgress(
                lexemeId = reviewLexemeId,
                state = ReviewStateColumn.REVIEW,
                dueAt =
                    now.minus(
                        Duration.ofDays(3),
                    ),
            )

            insertProgress(
                lexemeId = learningLexemeId,
                state = ReviewStateColumn.LEARNING,
                dueAt =
                    now.minus(
                        Duration.ofMinutes(1),
                    ),
                step = 0,
            )

            val queue =
                repository.getReviewQueue(
                    now = now,
                    dueLimit = 2,
                    newLimit = 0,
                )

            assertEquals(
                listOf(
                    learningLexemeId,
                    reviewLexemeId,
                ),
                queue.map { it.lexemeId },
            )

            assertEquals(
                ReviewState.LEARNING,
                queue[0].state,
            )

            assertEquals(
                ReviewState.REVIEW,
                queue[1].state,
            )
        }

    @Test
    fun relearningCardIsPrioritizedBeforeReviewCard() =
        runBlocking {

            val deckId =
                insertDeck("English")

            val reviewLexemeId =
                insertLexeme("dog")

            val relearningLexemeId =
                insertLexeme("cat")

            addToDeck(
                deckId = deckId,
                lexemeId = reviewLexemeId,
            )

            addToDeck(
                deckId = deckId,
                lexemeId = relearningLexemeId,
            )

            insertProgress(
                lexemeId = reviewLexemeId,
                state = ReviewStateColumn.REVIEW,
                dueAt =
                    now.minus(
                        Duration.ofDays(5),
                    ),
            )

            insertProgress(
                lexemeId = relearningLexemeId,
                state = ReviewStateColumn.RELEARNING,
                dueAt =
                    now.minus(
                        Duration.ofMinutes(5),
                    ),
                step = 0,
            )

            val queue =
                repository.getReviewQueue(
                    now = now,
                    dueLimit = 2,
                    newLimit = 0,
                )

            assertEquals(
                relearningLexemeId,
                queue[0].lexemeId,
            )

            assertEquals(
                ReviewState.RELEARNING,
                queue[0].state,
            )

            assertEquals(
                reviewLexemeId,
                queue[1].lexemeId,
            )
        }

    @Test
    fun futureReviewIsNotReturned() =
        runBlocking {

            val deckId =
                insertDeck("English")

            val dueLexemeId =
                insertLexeme("apple")

            val futureLexemeId =
                insertLexeme("banana")

            addToDeck(
                deckId = deckId,
                lexemeId = dueLexemeId,
            )

            addToDeck(
                deckId = deckId,
                lexemeId = futureLexemeId,
            )

            insertProgress(
                lexemeId = dueLexemeId,
                state = ReviewStateColumn.REVIEW,
                dueAt =
                    now.minus(
                        Duration.ofMinutes(1),
                    ),
            )

            insertProgress(
                lexemeId = futureLexemeId,
                state = ReviewStateColumn.REVIEW,
                dueAt =
                    now.plus(
                        Duration.ofDays(1),
                    ),
            )

            val queue =
                repository.getReviewQueue(
                    now = now,
                    dueLimit = 10,
                    newLimit = 0,
                )

            assertTrue(
                queue.any {
                    it.lexemeId ==
                            dueLexemeId
                }
            )

            assertFalse(
                queue.any {
                    it.lexemeId ==
                            futureLexemeId
                }
            )
        }

    @Test
    fun lexemeWithoutDeckMembershipIsNotReturned() =
        runBlocking {

            val lexemeId =
                insertLexeme("orphan")

            /*
             * Có progress nhưng không có DeckItem.
             */
            insertProgress(
                lexemeId = lexemeId,
                state = ReviewStateColumn.REVIEW,
                dueAt =
                    now.minus(
                        Duration.ofDays(1),
                    ),
            )

            val queue =
                repository.getReviewQueue(
                    now = now,
                    dueLimit = 10,
                    newLimit = 0,
                )

            assertTrue(
                queue.none {
                    it.lexemeId ==
                            lexemeId
                }
            )
        }

    @Test
    fun newReadyLexemeWithoutProgressIsReturned() =
        runBlocking {

            val deckId =
                insertDeck("English")

            val lexemeId =
                insertLexeme(
                    text = "apple",
                    enrichmentStatus =
                        EnrichmentStatusColumn.READY,
                )

            addToDeck(
                deckId = deckId,
                lexemeId = lexemeId,
            )

            /*
             * Không insert LearningProgress.
             *
             * Đây phải được xem như logical NEW.
             */
            val queue =
                repository.getReviewQueue(
                    now = now,
                    dueLimit = 0,
                    newLimit = 10,
                )

            assertEquals(
                1,
                queue.size,
            )

            val progress =
                queue.single()

            assertEquals(
                lexemeId,
                progress.lexemeId,
            )

            assertEquals(
                ReviewState.NEW,
                progress.state,
            )

            assertEquals(
                0,
                progress.reviewCount,
            )
        }

    @Test
    fun pendingNewLexemeIsNotReturned() =
        runBlocking {

            val deckId =
                insertDeck("English")

            val readyLexemeId =
                insertLexeme(
                    text = "apple",
                    enrichmentStatus =
                        EnrichmentStatusColumn.READY,
                )

            val pendingLexemeId =
                insertLexeme(
                    text = "banana",
                    enrichmentStatus =
                        EnrichmentStatusColumn.PENDING,
                )

            addToDeck(
                deckId = deckId,
                lexemeId = readyLexemeId,
            )

            addToDeck(
                deckId = deckId,
                lexemeId = pendingLexemeId,
            )

            val queue =
                repository.getReviewQueue(
                    now = now,
                    dueLimit = 0,
                    newLimit = 10,
                )

            assertTrue(
                queue.any {
                    it.lexemeId ==
                            readyLexemeId
                }
            )

            assertFalse(
                queue.any {
                    it.lexemeId ==
                            pendingLexemeId
                }
            )
        }

    @Test
    fun dueLimitAndNewLimitAreIndependent() =
        runBlocking {

            val deckId =
                insertDeck("English")

            val review1 =
                insertLexeme("review-one")

            val review2 =
                insertLexeme("review-two")

            val new1 =
                insertLexeme("new-one")

            val new2 =
                insertLexeme("new-two")

            listOf(
                review1,
                review2,
                new1,
                new2,
            ).forEach { lexemeId ->
                addToDeck(
                    deckId = deckId,
                    lexemeId = lexemeId,
                )
            }

            insertProgress(
                lexemeId = review1,
                state = ReviewStateColumn.REVIEW,
                dueAt =
                    now.minus(
                        Duration.ofDays(2),
                    ),
            )

            insertProgress(
                lexemeId = review2,
                state = ReviewStateColumn.REVIEW,
                dueAt =
                    now.minus(
                        Duration.ofDays(1),
                    ),
            )

            val queue =
                repository.getReviewQueue(
                    now = now,
                    dueLimit = 1,
                    newLimit = 1,
                )

            /*
             * 1 due + 1 new.
             */
            assertEquals(
                2,
                queue.size,
            )

            assertEquals(
                ReviewState.REVIEW,
                queue[0].state,
            )

            assertEquals(
                ReviewState.NEW,
                queue[1].state,
            )
        }

    @Test
    fun olderNewLexemeIsReturnedFirst() =
        runBlocking {

            val deckId =
                insertDeck("English")

            val olderLexemeId =
                insertLexeme("older")

            val newerLexemeId =
                insertLexeme("newer")

            addToDeck(
                deckId = deckId,
                lexemeId = newerLexemeId,
                addedAt =
                    now.minus(
                        Duration.ofDays(1),
                    ),
            )

            addToDeck(
                deckId = deckId,
                lexemeId = olderLexemeId,
                addedAt =
                    now.minus(
                        Duration.ofDays(5),
                    ),
            )

            val queue =
                repository.getReviewQueue(
                    now = now,
                    dueLimit = 0,
                    newLimit = 2,
                )

            assertEquals(
                listOf(
                    olderLexemeId,
                    newerLexemeId,
                ),
                queue.map { it.lexemeId },
            )
        }

    private suspend fun insertDeck(
        name: String,
    ): Long =
        database
            .deckDao()
            .insertDeck(
                DeckEntity(
                    name = name,

                    learningLanguageTag =
                        LANGUAGE_TAG,

                    translationLanguageTag =
                        "vi",

                    createdAtEpochMillis =
                        now.toEpochMilli(),

                    updatedAtEpochMillis =
                        now.toEpochMilli(),
                )
            )

    private suspend fun insertLexeme(
        text: String,
        enrichmentStatus:
        EnrichmentStatusColumn =
            EnrichmentStatusColumn.READY,
    ): Long =
        database
            .lexemeDao()
            .insertLexeme(
                LexemeEntity(
                    text = text,

                    normalizedText =
                        text.lowercase(),

                    languageTag =
                        LANGUAGE_TAG,

                    enrichmentStatus =
                        enrichmentStatus,
                )
            )

    private suspend fun addToDeck(
        deckId: Long,
        lexemeId: Long,
        addedAt: Instant = now,
    ) {
        val result =
            database
                .deckDao()
                .insertDeckItem(
                    DeckItemEntity(
                        deckId = deckId,
                        lexemeId = lexemeId,

                        languageTag =
                            LANGUAGE_TAG,

                        addedAtEpochMillis =
                            addedAt.toEpochMilli(),

                        sortOrder = 0,
                    )
                )

        check(result != -1L) {
            "Failed to insert DeckItem"
        }
    }

    private suspend fun insertProgress(
        lexemeId: Long,
        state: ReviewStateColumn,
        dueAt: Instant,
        step: Int? = null,
    ) {
        database
            .reviewDao()
            .upsertProgress(
                LearningProgressEntity(
                    lexemeId = lexemeId,

                    state = state,

                    step =
                        when (state) {
                            ReviewStateColumn.LEARNING,
                            ReviewStateColumn.RELEARNING ->
                                step ?: 0

                            ReviewStateColumn.NEW,
                            ReviewStateColumn.REVIEW ->
                                null
                        },

                    /*
                     * Non-NEW states trong scheduler
                     * phải có FSRS memory state.
                     */
                    stability = 2.5,
                    difficulty = 5.0,

                    reviewCount = 1,

                    scheduledDays = 1,

                    lastReviewedAtEpochMillis =
                        now
                            .minus(
                                Duration.ofDays(1)
                            )
                            .toEpochMilli(),

                    nextReviewAtEpochMillis =
                        dueAt.toEpochMilli(),
                )
            )
    }

    @Test
    fun reviewDueSummaryCountsRealDueCardsWithoutSessionLimits() =
        runBlocking {

            val deck1 =
                insertDeck("English")

            val deck2 =
                insertDeck("IELTS")

            val learning =
                insertLexeme("learning")

            val review =
                insertLexeme("review")

            val future =
                insertLexeme("future")

            val newReady =
                insertLexeme("new-ready")

            val newPending =
                insertLexeme(
                    text = "new-pending",
                    enrichmentStatus =
                        EnrichmentStatusColumn.PENDING,
                )

            val orphan =
                insertLexeme("orphan")

            /*
             * Same LEARNING lexeme exists in two decks.
             * Summary must still count it once.
             */
            addToDeck(
                deckId = deck1,
                lexemeId = learning,
            )

            addToDeck(
                deckId = deck2,
                lexemeId = learning,
            )

            addToDeck(
                deckId = deck1,
                lexemeId = review,
            )

            addToDeck(
                deckId = deck1,
                lexemeId = future,
            )

            addToDeck(
                deckId = deck1,
                lexemeId = newReady,
            )

            addToDeck(
                deckId = deck1,
                lexemeId = newPending,
            )

            insertProgress(
                lexemeId = learning,
                state =
                    ReviewStateColumn.LEARNING,
                step = 0,
                dueAt =
                    now.minus(
                        Duration.ofMinutes(1)
                    ),
            )

            insertProgress(
                lexemeId = review,
                state =
                    ReviewStateColumn.REVIEW,
                dueAt =
                    now.minus(
                        Duration.ofDays(1)
                    ),
            )

            insertProgress(
                lexemeId = future,
                state =
                    ReviewStateColumn.REVIEW,
                dueAt =
                    now.plus(
                        Duration.ofDays(1)
                    ),
            )

            /*
             * Has progress but no DeckItem.
             */
            insertProgress(
                lexemeId = orphan,
                state =
                    ReviewStateColumn.REVIEW,
                dueAt =
                    now.minus(
                        Duration.ofDays(3)
                    ),
            )

            val summary =
                repository
                    .getReviewDueSummary(now)

            assertEquals(
                1,
                summary.shortTermDue,
            )

            assertEquals(
                1,
                summary.reviewDue,
            )

            assertEquals(
                1,
                summary.newAvailable,
            )

            assertEquals(
                2,
                summary.dueCount,
            )

            assertEquals(
                3,
                summary.totalAvailable,
            )
        }

    @Test
    fun reactiveSummaryUpdatesAfterRoomInvalidation() =
        runBlocking {

            val deckId =
                insertDeck("English")

            val lexemeId =
                insertLexeme("apple")

            addToDeck(
                deckId = deckId,
                lexemeId = lexemeId,
            )

            val clock =
                Clock.fixed(
                    now,
                    ZoneOffset.UTC,
                )

            /*
             * Đảm bảo initial emission đã xảy ra
             * trước khi mình thay đổi DB.
             */
            val initialReceived =
                CompletableDeferred<Unit>()

            val collection =
                async {

                    repository
                        .observeReviewDueSummary(
                            clock = clock,
                        )
                        .onEach {
                            if (
                                !initialReceived
                                    .isCompleted
                            ) {
                                initialReceived
                                    .complete(Unit)
                            }
                        }
                        .map { summary ->
                            summary.dueCount
                        }
                        .distinctUntilChanged()
                        .take(2)
                        .toList()
                }

            withTimeout(2_000L) {
                initialReceived.await()
            }

            /*
             * Không gọi refresh().
             *
             * learning_progress thay đổi
             * phải khiến Room invalidation
             * tự phát summary mới.
             */
            insertProgress(
                lexemeId = lexemeId,
                state =
                    ReviewStateColumn.REVIEW,
                dueAt =
                    now.minus(
                        Duration.ofMinutes(1)
                    ),
            )

            val counts =
                withTimeout(2_000L) {
                    collection.await()
                }

            assertEquals(
                listOf(
                    0,
                    1,
                ),
                counts,
            )
        }


    @Test
    fun reactiveSummaryUpdatesWhenTimePassesWithoutDatabaseWrite() =
        runBlocking {

            val deckId =
                insertDeck("English")

            val lexemeId =
                insertLexeme("apple")

            addToDeck(
                deckId = deckId,
                lexemeId = lexemeId,
            )

            /*
             * Dùng cùng system clock mà production dùng.
             */
            val dueAt =
                Instant
                    .now()
                    .plusSeconds(2)

            insertProgress(
                lexemeId = lexemeId,
                state =
                    ReviewStateColumn.REVIEW,
                dueAt = dueAt,
            )

            /*
             * Sau đoạn này tuyệt đối không write DB nữa.
             *
             * Emission thứ hai chỉ có thể tới từ
             * timer getNextDueAtAfter().
             */
            val counts =
                withTimeout(6_000L) {

                    repository
                        .observeReviewDueSummary(
                            clock =
                                Clock.systemUTC(),
                        )
                        .map { summary ->
                            summary.dueCount
                        }
                        .distinctUntilChanged()
                        .take(2)
                        .toList()
                }

            assertEquals(
                listOf(
                    0,
                    1,
                ),
                counts,
            )
        }

    companion object {
        private const val LANGUAGE_TAG =
            "en"
    }
}