package com.chung.lexis.feature.review

import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.Lexeme
import com.chung.lexis.domain.model.ReviewCard
import com.chung.lexis.domain.model.ReviewDueSummary
import com.chung.lexis.domain.model.ReviewLog
import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.model.ReviewState
import com.chung.lexis.domain.repository.ReviewRepository
import com.chung.lexis.domain.repository.SaveReviewResult
import com.chung.lexis.domain.scheduler.ReviewSchedule
import com.chung.lexis.domain.scheduler.ReviewScheduler
import com.chung.lexis.domain.usecase.SubmitReviewUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewSessionViewModelTest {

    private val now =
        Instant.parse("2026-10-21T12:00:00Z")

    private lateinit var fakeRepository: FakeReviewRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun savedCardIsNotResubmittableWhenNextCardFailsToLoad() =
        runTest {

            val viewModel =
                createViewModel(
                    queue =
                        listOf(
                            progress(1L),
                            progress(2L),
                        ),
                    cards =
                        mapOf(
                            1L to card(1L),
                        ),
                    failCardIds =
                        setOf(2L),
                )

            viewModel.startSession(
                dueLimit = 10,
                newLimit = 10,
            )

            advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value is
                        ReviewSessionUiState.Active
            )

            viewModel.revealAnswer()

            viewModel.submitRating(
                ReviewRating.GOOD
            )

            advanceUntilIdle()

            assertEquals(
                ReviewSessionUiState.Failed(
                    ReviewSessionError.LOAD_FAILED
                ),
                viewModel.uiState.value,
            )

            /*
             * Quan trọng nhất:
             * A đã save đúng 1 lần.
             */
            assertEquals(
                1,
                fakeRepository.saveReviewCallCount,
            )

            /*
             * Thử bấm rating lại khi Failed.
             * Không được tạo review thứ hai.
             */
            viewModel.submitRating(
                ReviewRating.GOOD
            )

            advanceUntilIdle()

            assertEquals(
                1,
                fakeRepository.saveReviewCallCount,
            )
        }

    private fun createViewModel(
        queue: List<LearningProgress>,
        cards: Map<Long, ReviewCard>,
        failCardIds: Set<Long>,
    ): ReviewSessionViewModel {

        fakeRepository =
            FakeReviewRepository(
                queue = queue,
                cards = cards,
                failCardIds = failCardIds,
            )

        return ReviewSessionViewModel(
            reviewRepository =
                fakeRepository,

            submitReviewUseCase =
                SubmitReviewUseCase(
                    reviewRepository =
                        fakeRepository,

                    reviewScheduler =
                        FakeReviewScheduler(),
                ),

            clock =
                Clock.fixed(
                    now,
                    ZoneOffset.UTC,
                ),
        )
    }

    private fun progress(
        lexemeId: Long,
    ): LearningProgress =
        LearningProgress(
            lexemeId = lexemeId,
        )

    private fun card(
        lexemeId: Long,
    ): ReviewCard =
        ReviewCard(
            lexeme =
                Lexeme(
                    id = lexemeId,
                    text = "word-$lexemeId",
                    normalizedText = "word-$lexemeId",
                    languageTag = "en",
                ),
            meanings = emptyList(),
            pronunciations = emptyList(),
            examples = emptyList(),
            progress = progress(lexemeId),
        )

    private class FakeReviewRepository(
        private val queue: List<LearningProgress>,
        private val cards: Map<Long, ReviewCard>,
        private val failCardIds: Set<Long>,
    ) : ReviewRepository {

        private val progressById =
            queue
                .associateBy { it.lexemeId }
                .toMutableMap()

        var saveReviewCallCount = 0
            private set

        override suspend fun getProgress(
            lexemeId: Long,
        ): LearningProgress? =
            progressById[lexemeId]

        override suspend fun getReviewQueue(
            now: Instant,
            dueLimit: Int,
            newLimit: Int,
        ): List<LearningProgress> =
            queue

        override suspend fun getReviewDueSummary(
            now: Instant,
        ): ReviewDueSummary =
            error("Not used")

        override fun observeReviewDueSummary(
            clock: Clock,
        ): Flow<ReviewDueSummary> =
            emptyFlow()

        override suspend fun saveReview(
            previousProgress: LearningProgress,
            nextProgress: LearningProgress,
            reviewLog: ReviewLog,
        ): SaveReviewResult {

            saveReviewCallCount += 1

            progressById[nextProgress.lexemeId] =
                nextProgress

            return SaveReviewResult.Saved(
                progress = nextProgress,
                reviewLog = reviewLog,
            )
        }

        override suspend fun getReviewHistory(
            lexemeId: Long,
        ): List<ReviewLog> =
            emptyList()

        override suspend fun getReviewCard(
            lexemeId: Long,
        ): ReviewCard? {

            if (lexemeId in failCardIds) {
                throw IllegalStateException(
                    "Cannot hydrate $lexemeId"
                )
            }

            return cards[lexemeId]
        }
    }

    private class FakeReviewScheduler : ReviewScheduler {

        override fun schedule(
            current: LearningProgress,
            rating: ReviewRating,
            reviewedAt: Instant,
        ): ReviewSchedule =
            ReviewSchedule(
                progress =
                    current.copy(
                        state = ReviewState.LEARNING,
                        step = current.step?.plus(1) ?: 0,
                        reviewCount = current.reviewCount + 1,
                        scheduledDays = 0,
                        lastReviewedAt = reviewedAt,
                        nextReviewAt =
                            reviewedAt.plus(
                                Duration.ofMinutes(1)
                            ),
                    ),
                elapsedDays = 0,
                scheduledDays = 0,
            )
    }
}
