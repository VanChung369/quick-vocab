package com.chung.lexis.domain.usecase

import com.chung.lexis.domain.model.CorruptReviewDataException
import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.ReviewCard
import com.chung.lexis.domain.model.ReviewDueSummary
import com.chung.lexis.domain.model.ReviewLog
import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.model.ReviewState
import com.chung.lexis.domain.repository.ReviewRepository
import com.chung.lexis.domain.repository.SaveReviewResult
import com.chung.lexis.domain.scheduler.ReviewSchedule
import com.chung.lexis.domain.scheduler.ReviewScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant

class SubmitReviewUseCaseTest {

    private val now =
        Instant.parse("2026-01-10T10:00:00Z")

    @Test
    fun invalidScheduledProgressIsCorruptAndNotSaved() =
        runTest {

            val repository =
                RecordingReviewRepository()

            val useCase =
                SubmitReviewUseCase(
                    reviewRepository = repository,

                    /*
                     * LEARNING không có stability:
                     * lần review sau scheduler sẽ từ chối.
                     */
                    reviewScheduler =
                        FakeScheduler(stability = null),
                )

            val error =
                runCatching {
                    useCase(
                        progress =
                            LearningProgress(lexemeId = 1L),
                        rating = ReviewRating.GOOD,
                        reviewedAt = now,
                    )
                }.exceptionOrNull()

            assertTrue(
                "Expected CorruptReviewDataException but was $error",
                error is CorruptReviewDataException &&
                        error.cause is IllegalArgumentException
            )

            assertEquals(
                0,
                repository.saveReviewCallCount,
            )
        }

    @Test
    fun validScheduledProgressIsSaved() =
        runTest {

            val repository =
                RecordingReviewRepository()

            val useCase =
                SubmitReviewUseCase(
                    reviewRepository = repository,
                    reviewScheduler =
                        FakeScheduler(stability = 2.3),
                )

            val result =
                useCase(
                    progress =
                        LearningProgress(lexemeId = 1L),
                    rating = ReviewRating.GOOD,
                    reviewedAt = now,
                )

            assertTrue(result is SaveReviewResult.Saved)

            assertEquals(
                1,
                repository.saveReviewCallCount,
            )
        }

    /*
     * NEW → LEARNING step 1, +10m.
     */
    private class FakeScheduler(
        private val stability: Double?,
    ) : ReviewScheduler {

        override fun schedule(
            current: LearningProgress,
            rating: ReviewRating,
            reviewedAt: Instant,
        ): ReviewSchedule =
            ReviewSchedule(
                progress =
                    current.copy(
                        state = ReviewState.LEARNING,
                        step = 1,
                        stability = stability,
                        difficulty = 5.0,
                        reviewCount = current.reviewCount + 1,
                        scheduledDays = 0,
                        lastReviewedAt = reviewedAt,
                        nextReviewAt =
                            reviewedAt.plus(
                                Duration.ofMinutes(10)
                            ),
                    ),
                elapsedDays = 0,
                scheduledDays = 0,
            )
    }

    private class RecordingReviewRepository : ReviewRepository {

        var saveReviewCallCount = 0
            private set

        override suspend fun saveReview(
            previousProgress: LearningProgress,
            nextProgress: LearningProgress,
            reviewLog: ReviewLog,
        ): SaveReviewResult {

            saveReviewCallCount += 1

            return SaveReviewResult.Saved(
                progress = nextProgress,
                reviewLog = reviewLog,
            )
        }

        override suspend fun getProgress(
            lexemeId: Long,
        ): LearningProgress? =
            error("Not used")

        override suspend fun getReviewQueue(
            now: Instant,
            dueLimit: Int,
            newLimit: Int,
        ): List<LearningProgress> =
            error("Not used")

        override suspend fun getUpcomingShortTerm(
            now: Instant,
        ): List<LearningProgress> =
            error("Not used")

        override suspend fun getReviewDueSummary(
            now: Instant,
        ): ReviewDueSummary =
            error("Not used")

        override fun observeReviewDueSummary(
            clock: Clock,
        ): Flow<ReviewDueSummary> =
            emptyFlow()

        override suspend fun getReviewHistory(
            lexemeId: Long,
        ): List<ReviewLog> =
            error("Not used")

        override suspend fun getReviewCard(
            lexemeId: Long,
        ): ReviewCard? =
            error("Not used")

        override suspend fun quarantineReviewCard(
            lexemeId: Long,
            reason: String,
            detectedAt: Instant,
        ) =
            error("Not used")
    }
}
