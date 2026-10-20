package com.chung.lexis.domain.scheduler

import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.model.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class DefaultReviewSchedulerTest {

    private val now =
        Instant.parse("2026-01-10T10:00:00Z")

    private val scheduler =
        DefaultReviewScheduler(
            stateMachine = ReviewStateMachine(),
            stepPolicy = LearningStepPolicy(
                learningSteps = listOf(
                    Duration.ofMinutes(1),
                    Duration.ofMinutes(10),
                ),
                relearningSteps = listOf(
                    Duration.ofMinutes(10),
                ),
            ),
            memoryPolicy = FakeMemoryPolicy(),
        )

    @Test
    fun `new good schedules first learning step`() {

        val result =
            scheduler.schedule(
                current = LearningProgress(lexemeId = 1L),
                rating = ReviewRating.GOOD,
                reviewedAt = now,
            )

        val progress = result.progress

        assertEquals(ReviewState.LEARNING, progress.state)
        assertEquals(0, progress.step)
        assertEquals(now.plus(Duration.ofMinutes(1)), progress.nextReviewAt)
        assertEquals(now, progress.lastReviewedAt)
        assertEquals(1, progress.reviewCount)
        assertEquals(0, progress.scheduledDays)
        assertEquals(0, result.elapsedDays)
        assertEquals(2.5, progress.stability!!, 0.0)
        assertEquals(5.0, progress.difficulty!!, 0.0)
    }

    @Test
    fun `learning again resets to first step`() {

        val result =
            scheduler.schedule(
                current = learning(step = 1),
                rating = ReviewRating.AGAIN,
                reviewedAt = now,
            )

        assertEquals(ReviewState.LEARNING, result.progress.state)
        assertEquals(0, result.progress.step)
        assertEquals(
            now.plus(Duration.ofMinutes(1)),
            result.progress.nextReviewAt,
        )
    }

    @Test
    fun `good on last learning step graduates to review`() {

        val result =
            scheduler.schedule(
                current = learning(step = 1),
                rating = ReviewRating.GOOD,
                reviewedAt = now,
            )

        val progress = result.progress

        assertEquals(ReviewState.REVIEW, progress.state)
        assertNull(progress.step)
        assertEquals(now.plus(Duration.ofDays(4)), progress.nextReviewAt)
        assertEquals(4, progress.scheduledDays)
        assertEquals(4, result.scheduledDays)
    }

    @Test
    fun `new easy graduates directly to review`() {

        val result =
            scheduler.schedule(
                current = LearningProgress(lexemeId = 1L),
                rating = ReviewRating.EASY,
                reviewedAt = now,
            )

        assertEquals(ReviewState.REVIEW, result.progress.state)
        assertNull(result.progress.step)
        assertEquals(
            now.plus(Duration.ofDays(4)),
            result.progress.nextReviewAt,
        )
    }

    @Test
    fun `review good stays in review and computes elapsed days`() {

        val result =
            scheduler.schedule(
                current = review(lastReviewedAt = now.minus(Duration.ofDays(3))),
                rating = ReviewRating.GOOD,
                reviewedAt = now,
            )

        assertEquals(ReviewState.REVIEW, result.progress.state)
        assertEquals(3, result.elapsedDays)
        assertEquals(0, result.progress.lapseCount)
        assertEquals(
            now.plus(Duration.ofDays(4)),
            result.progress.nextReviewAt,
        )
    }

    @Test
    fun `review again enters first relearning step and counts lapse`() {

        val result =
            scheduler.schedule(
                current = review(lastReviewedAt = now.minus(Duration.ofDays(3))),
                rating = ReviewRating.AGAIN,
                reviewedAt = now,
            )

        val progress = result.progress

        assertEquals(ReviewState.RELEARNING, progress.state)
        assertEquals(0, progress.step)
        assertEquals(now.plus(Duration.ofMinutes(10)), progress.nextReviewAt)
        assertEquals(1, progress.lapseCount)
    }

    @Test
    fun `good on last relearning step graduates to review`() {

        val result =
            scheduler.schedule(
                current = LearningProgress(
                    lexemeId = 1L,
                    state = ReviewState.RELEARNING,
                    step = 0,
                    lapseCount = 1,
                    lastReviewedAt = now.minus(Duration.ofMinutes(10)),
                ),
                rating = ReviewRating.GOOD,
                reviewedAt = now,
            )

        assertEquals(ReviewState.REVIEW, result.progress.state)
        assertNull(result.progress.step)
        assertEquals(1, result.progress.lapseCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reviewedAt cannot be before lastReviewedAt`() {

        scheduler.schedule(
            current = review(lastReviewedAt = now),
            rating = ReviewRating.GOOD,
            reviewedAt = now.minusSeconds(1),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `review state must not have a step`() {

        scheduler.schedule(
            current = review(lastReviewedAt = now.minus(Duration.ofDays(1)))
                .copy(step = 0),
            rating = ReviewRating.GOOD,
            reviewedAt = now,
        )
    }

    private fun learning(step: Int): LearningProgress =
        LearningProgress(
            lexemeId = 1L,
            state = ReviewState.LEARNING,
            step = step,
            reviewCount = 1,
            lastReviewedAt = now.minus(Duration.ofMinutes(1)),
        )

    private fun review(lastReviewedAt: Instant): LearningProgress =
        LearningProgress(
            lexemeId = 1L,
            state = ReviewState.REVIEW,
            stability = 2.5,
            difficulty = 5.0,
            reviewCount = 3,
            scheduledDays = 3,
            lastReviewedAt = lastReviewedAt,
        )
}

private class FakeMemoryPolicy(
    private val intervalDays: Int = 4,
) : ReviewMemoryPolicy {

    override fun evaluate(
        current: LearningProgress,
        rating: ReviewRating,
        elapsedDays: Int,
    ): ReviewMemoryDecision =
        ReviewMemoryDecision(
            stability = 2.5,
            difficulty = 5.0,
            intervalDays = intervalDays,
        )
}
