package com.chung.lexis.feature.review

import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.Lexeme
import com.chung.lexis.domain.model.ReviewCard
import com.chung.lexis.domain.model.ReviewDueSummary
import com.chung.lexis.domain.model.ReviewLog
import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.model.ReviewSessionLaunchPolicy
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
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewSessionViewModelTest {

    private val now =
        Instant.parse("2026-10-21T12:00:00Z")

    private lateinit var fakeRepository: FakeReviewRepository

    private val testDispatcher =
        StandardTestDispatcher()

    /*
     * Clock chạy theo virtual time của test scheduler,
     * để advanceTimeBy() / delay() khớp với clock.instant().
     */
    private val clock =
        SchedulerClock(
            scheduler = testDispatcher.scheduler,
            base = now,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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

            viewModel.startSession()

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

    @Test
    fun dueShortTermRepeatComesBeforePendingNewCard() =
        runTest {

            val viewModel =
                createViewModel(
                    queue =
                        listOf(
                            progress(A),
                            progress(B),
                            progress(C),
                            progress(D),
                        ),
                    cards =
                        listOf(A, B, C, D)
                            .associateWith { card(it) },
                    failCardIds =
                        emptySet(),
                )

            viewModel.startSession()

            advanceUntilIdle()

            assertActiveCard(A, viewModel)

            /*
             * A → GOOD → LEARNING, due +1m.
             */
            rateGood(viewModel)

            assertActiveCard(B, viewModel)

            /*
             * User ở card B/C hơn 1 phút.
             */
            advanceTimeBy(30_000L)

            rateGood(viewModel)

            assertActiveCard(C, viewModel)

            advanceTimeBy(31_000L)

            rateGood(viewModel)

            /*
             * A đã due (+1m) → SHORT_TERM
             * phải đứng trước D (NEW).
             */
            assertActiveCard(A, viewModel)

            assertEquals(
                1,
                fakeRepository
                    .getReviewQueueCallCount,
            )
        }

    @Test
    fun shortTermRequeueNeverRequeriesGlobalQueue() =
        runTest {

            /*
             * Repository đã trả đúng newLimit = 2.
             * NEW #3 tồn tại nhưng nằm ngoài budget.
             */
            val viewModel =
                createViewModel(
                    queue =
                        listOf(
                            progress(A),
                            progress(B),
                        ),
                    cards =
                        listOf(A, B, C)
                            .associateWith { card(it) },
                    failCardIds =
                        emptySet(),
                    launchPolicy =
                        ReviewSessionLaunchPolicy(
                            dueLimit = 10,
                            newLimit = 2,
                        ),
                )

            viewModel.startSession()

            advanceUntilIdle()

            assertActiveCard(A, viewModel)

            rateGood(viewModel)

            assertActiveCard(B, viewModel)

            rateGood(viewModel)

            /*
             * Không còn card ready,
             * nhưng A/B còn learning step.
             */
            assertEquals(
                ReviewSessionUiState.Waiting(
                    nextDueAt =
                        now.plus(
                            Duration.ofMinutes(1)
                        ),
                    reviewedCount = 2,
                    remainingCount = 2,
                ),
                viewModel.uiState.value,
            )

            /*
             * Timer tới hạn → A short-term requeue.
             */
            advanceUntilIdle()

            assertActiveCard(A, viewModel)

            assertEquals(
                1,
                fakeRepository
                    .getReviewQueueCallCount,
            )

            /*
             * LEARNING → REVIEW: A rời session.
             */
            rateGood(viewModel)

            assertActiveCard(B, viewModel)

            rateGood(viewModel)

            assertEquals(
                ReviewSessionUiState.Completed(
                    reviewedCount = 4,
                ),
                viewModel.uiState.value,
            )

            assertEquals(
                1,
                fakeRepository
                    .getReviewQueueCallCount,
            )

            /*
             * NEW #3 không bao giờ chui vào.
             */
            assertTrue(
                C !in
                        fakeRepository
                            .requestedCardIds
            )
        }

    @Test
    fun retryAfterInitialLoadFailureRequeriesGlobalQueue() =
        runTest {

            val viewModel =
                createViewModel(
                    queue =
                        emptyList(),

                    cards =
                        emptyMap(),

                    failCardIds =
                        emptySet(),

                    queueFailuresRemaining =
                        1,
                )

            viewModel.startSession()

            advanceUntilIdle()

            assertEquals(
                ReviewSessionUiState.Failed(
                    ReviewSessionError.LOAD_FAILED
                ),
                viewModel.uiState.value,
            )

            assertEquals(
                1,
                fakeRepository
                    .getReviewQueueCallCount,
            )

            viewModel.retry()

            advanceUntilIdle()

            /*
             * Initial load failure:
             * retry buộc phải query Room lại.
             */
            assertEquals(
                2,
                fakeRepository
                    .getReviewQueueCallCount,
            )

            assertEquals(
                ReviewSessionUiState.Completed(
                    reviewedCount = 0,
                ),
                viewModel.uiState.value,
            )
        }

    @Test
    fun retryAfterMidSessionLoadFailureResumesWithoutRequery() =
        runTest {

            val viewModel =
                createViewModel(
                    queue =
                        listOf(
                            progress(A),
                            progress(B),
                        ),

                    cards =
                        listOf(
                            A,
                            B,
                        ).associateWith {
                            card(it)
                        },

                    failCardIds =
                        emptySet(),

                    /*
                     * B fail đúng lần hydrate đầu.
                     */
                    cardFailuresRemaining =
                        mapOf(
                            B to 1,
                        ),
                )

            viewModel.startSession()

            runCurrent()

            assertActiveCard(
                A,
                viewModel,
            )

            /*
             * Save A thành công.
             *
             * Sau đó ViewModel load B,
             * nhưng B fail lần đầu.
             */
            viewModel.revealAnswer()

            viewModel.submitRating(
                ReviewRating.GOOD
            )

            runCurrent()

            assertEquals(
                ReviewSessionUiState.Failed(
                    ReviewSessionError.LOAD_FAILED
                ),
                viewModel.uiState.value,
            )

            /*
             * A đã được save đúng một lần.
             */
            assertEquals(
                1,
                fakeRepository
                    .saveReviewCallCount,
            )

            /*
             * Initial queue chỉ query một lần.
             */
            assertEquals(
                1,
                fakeRepository
                    .getReviewQueueCallCount,
            )

            /*
             * Retry phải tiếp tục session hiện tại,
             * KHÔNG start session mới.
             */
            viewModel.retry()

            runCurrent()

            val state =
                viewModel
                    .uiState
                    .value

            assertTrue(
                "Expected Active(B) but was $state",
                state is
                        ReviewSessionUiState.Active &&
                        state.card.lexeme.id == B,
            )

            state as
                    ReviewSessionUiState.Active

            /*
             * reviewedCount của A phải được giữ.
             */
            assertEquals(
                1,
                state.reviewedCount,
            )

            /*
             * Đây là invariant quan trọng nhất:
             *
             * Retry giữa session không được
             * lấy global queue lần nữa.
             */
            assertEquals(
                1,
                fakeRepository
                    .getReviewQueueCallCount,
            )

            /*
             * Retry load B không được submit A lần nữa.
             */
            assertEquals(
                1,
                fakeRepository
                    .saveReviewCallCount,
            )
        }

    @Test
    fun startSessionUsesPolicyAndDoesNotRestartAfterCompletion() =
        runTest {

            val policy =
                ReviewSessionLaunchPolicy(
                    dueLimit = 123,
                    newLimit = 7,
                )

            val viewModel =
                createViewModel(
                    queue =
                        emptyList(),

                    cards =
                        emptyMap(),

                    failCardIds =
                        emptySet(),

                    launchPolicy =
                        policy,
                )

            /*
             * Lần đầu route được mở.
             */
            viewModel.startSession()

            advanceUntilIdle()

            assertEquals(
                ReviewSessionUiState.Completed(
                    reviewedCount = 0,
                ),
                viewModel.uiState.value,
            )

            assertEquals(
                1,
                fakeRepository
                    .getReviewQueueCallCount,
            )

            assertEquals(
                123,
                fakeRepository.lastDueLimit,
            )

            assertEquals(
                7,
                fakeRepository.lastNewLimit,
            )

            /*
             * Giả lập rời tab rồi quay lại.
             *
             * LaunchedEffect sẽ gọi startSession()
             * một lần nữa.
             */
            viewModel.startSession()

            advanceUntilIdle()

            /*
             * Completed không được tự mở
             * session mới.
             */
            assertEquals(
                1,
                fakeRepository
                    .getReviewQueueCallCount,
            )
        }

    private fun TestScope.rateGood(
        viewModel: ReviewSessionViewModel,
    ) {

        viewModel.revealAnswer()

        viewModel.submitRating(
            ReviewRating.GOOD
        )

        /*
         * Không advanceUntilIdle(): nó sẽ nhảy
         * virtual time qua luôn timer của Waiting.
         */
        runCurrent()
    }

    private fun assertActiveCard(
        expectedLexemeId: Long,
        viewModel: ReviewSessionViewModel,
    ) {

        val state =
            viewModel.uiState.value

        assertTrue(
            "Expected Active($expectedLexemeId) but was $state",
            state is ReviewSessionUiState.Active &&
                    state.card.lexeme.id ==
                    expectedLexemeId
        )
    }

    private fun createViewModel(
        queue: List<LearningProgress>,
        cards: Map<Long, ReviewCard>,
        failCardIds: Set<Long>,
        launchPolicy:
        ReviewSessionLaunchPolicy =
            ReviewSessionLaunchPolicy(
                dueLimit = 10,
                newLimit = 10,
            ),
        queueFailuresRemaining: Int = 0,
        cardFailuresRemaining:
        Map<Long, Int> =
            emptyMap(),
    ): ReviewSessionViewModel {

        fakeRepository =
            FakeReviewRepository(
                queue = queue,
                cards = cards,
                failCardIds = failCardIds,
                queueFailuresRemaining =
                    queueFailuresRemaining,

                cardFailuresRemaining =
                    cardFailuresRemaining,
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

            launchPolicy =
                launchPolicy,

            clock =
                clock,
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
        queueFailuresRemaining: Int = 0,
        cardFailuresRemaining:
        Map<Long, Int> =
            emptyMap(),
    ) : ReviewRepository {

        private var queueFailuresRemaining =
            queueFailuresRemaining

        private val cardFailuresRemaining =
            cardFailuresRemaining
                .toMutableMap()

        private val progressById =
            queue
                .associateBy { it.lexemeId }
                .toMutableMap()

        var saveReviewCallCount = 0
            private set

        var getReviewQueueCallCount =
            0
            private set

        var lastDueLimit: Int? =
            null
            private set

        var lastNewLimit: Int? =
            null
            private set

        val requestedCardIds =
            mutableListOf<Long>()

        override suspend fun getProgress(
            lexemeId: Long,
        ): LearningProgress? =
            progressById[lexemeId]

        override suspend fun getReviewQueue(
            now: Instant,
            dueLimit: Int,
            newLimit: Int,
        ): List<LearningProgress> {

            getReviewQueueCallCount += 1

            lastDueLimit =
                dueLimit

            lastNewLimit =
                newLimit

            if (queueFailuresRemaining > 0) {

                queueFailuresRemaining -= 1

                throw IllegalStateException(
                    "Cannot load initial review queue"
                )
            }

            return queue
        }

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

            requestedCardIds += lexemeId

            if (lexemeId in failCardIds) {
                throw IllegalStateException(
                    "Cannot hydrate $lexemeId"
                )
            }

            val remainingFailures =
                cardFailuresRemaining[
                    lexemeId
                ] ?: 0

            if (remainingFailures > 0) {

                cardFailuresRemaining[
                    lexemeId
                ] =
                    remainingFailures - 1

                throw IllegalStateException(
                    "Cannot hydrate $lexemeId"
                )
            }

            return cards[lexemeId]
        }
    }

    /*
     * NEW      → LEARNING step 0, +1m
     * LEARNING → REVIEW, +1d
     */
    private class FakeReviewScheduler : ReviewScheduler {

        override fun schedule(
            current: LearningProgress,
            rating: ReviewRating,
            reviewedAt: Instant,
        ): ReviewSchedule =
            when (current.state) {

                ReviewState.NEW ->
                    ReviewSchedule(
                        progress =
                            current.copy(
                                state = ReviewState.LEARNING,
                                step = 0,
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

                ReviewState.LEARNING,
                ReviewState.RELEARNING,
                ReviewState.REVIEW ->
                    ReviewSchedule(
                        progress =
                            current.copy(
                                state = ReviewState.REVIEW,
                                step = null,
                                reviewCount = current.reviewCount + 1,
                                scheduledDays = 1,
                                lastReviewedAt = reviewedAt,
                                nextReviewAt =
                                    reviewedAt.plus(
                                        Duration.ofDays(1)
                                    ),
                            ),
                        elapsedDays = 0,
                        scheduledDays = 1,
                    )
            }
    }

    private class SchedulerClock(
        private val scheduler: TestCoroutineScheduler,
        private val base: Instant,
    ) : Clock() {

        override fun getZone(): ZoneId =
            ZoneOffset.UTC

        override fun withZone(
            zone: ZoneId?,
        ): Clock =
            this

        override fun instant(): Instant =
            base.plusMillis(
                scheduler.currentTime
            )
    }

    companion object {
        private const val A = 1L
        private const val B = 2L
        private const val C = 3L
        private const val D = 4L
    }
}
