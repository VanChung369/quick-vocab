package com.chung.lexis.domain.model

import org.junit.Test
import java.time.Duration
import java.time.Instant

class LearningProgressTest {

    private val now =
        Instant.parse("2026-01-10T10:00:00Z")

    private val learning =
        LearningProgress(
            lexemeId = 1L,
            state = ReviewState.LEARNING,
            step = 1,
            stability = 2.3,
            difficulty = 5.0,
            reviewCount = 1,
            lastReviewedAt = now,
            nextReviewAt = now.plus(Duration.ofMinutes(10)),
        )

    private val review =
        learning.copy(
            state = ReviewState.REVIEW,
            step = null,
            scheduledDays = 3,
            nextReviewAt = now.plus(Duration.ofDays(3)),
        )

    @Test
    fun `valid reviewed progress passes`() {

        learning.requireReviewed()

        review.requireReviewed()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `new progress is rejected`() {

        LearningProgress(lexemeId = 1L).requireReviewed()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing nextReviewAt is rejected`() {

        learning.copy(nextReviewAt = null).requireReviewed()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `nextReviewAt before lastReviewedAt is rejected`() {

        learning
            .copy(nextReviewAt = now.minusSeconds(1))
            .requireReviewed()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `learning without step is rejected`() {

        learning.copy(step = null).requireReviewed()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `review with step is rejected`() {

        review.copy(step = 0).requireReviewed()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing stability is rejected`() {

        learning.copy(stability = null).requireReviewed()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non finite stability is rejected`() {

        learning.copy(stability = Double.NaN).requireReviewed()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non positive stability is rejected`() {

        learning.copy(stability = 0.0).requireReviewed()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non finite difficulty is rejected`() {

        learning
            .copy(difficulty = Double.POSITIVE_INFINITY)
            .requireReviewed()
    }
}
