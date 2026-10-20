package com.chung.lexis.domain.scheduler

import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.model.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class LearningStepPolicyTest {

    private val policy =
        LearningStepPolicy(
            learningSteps = listOf(
                Duration.ofMinutes(1),
                Duration.ofMinutes(10),
            ),
            relearningSteps = listOf(
                Duration.ofMinutes(10),
            ),
        )

    @Test
    fun `new good schedules first learning step`() {

        val result =
            policy.decide(
                phase = ReviewState.NEW,
                currentStep = null,
                rating = ReviewRating.GOOD,
            )

        assertEquals(
            LearningStepDecision.Schedule(
                step = 0,
                delay = Duration.ofMinutes(1),
            ),
            result,
        )
    }

    @Test
    fun `good advances to next learning step`() {

        val result =
            policy.decide(
                phase = ReviewState.LEARNING,
                currentStep = 0,
                rating = ReviewRating.GOOD,
            )

        assertEquals(
            LearningStepDecision.Schedule(
                step = 1,
                delay = Duration.ofMinutes(10),
            ),
            result,
        )
    }

    @Test
    fun `good on last learning step graduates`() {

        val result =
            policy.decide(
                phase = ReviewState.LEARNING,
                currentStep = 1,
                rating = ReviewRating.GOOD,
            )

        assertTrue(
            result is LearningStepDecision.Graduate
        )
    }

    @Test
    fun `again resets learning to first step`() {

        val result =
            policy.decide(
                phase = ReviewState.LEARNING,
                currentStep = 1,
                rating = ReviewRating.AGAIN,
            )

        assertEquals(
            LearningStepDecision.Schedule(
                step = 0,
                delay = Duration.ofMinutes(1),
            ),
            result,
        )
    }

    @Test
    fun `hard stays on current learning step`() {

        val result =
            policy.decide(
                phase = ReviewState.LEARNING,
                currentStep = 1,
                rating = ReviewRating.HARD,
            )

        assertEquals(
            LearningStepDecision.Schedule(
                step = 1,
                delay = Duration.ofMinutes(10),
            ),
            result,
        )
    }

    @Test
    fun `easy graduates immediately`() {

        val result =
            policy.decide(
                phase = ReviewState.NEW,
                currentStep = null,
                rating = ReviewRating.EASY,
            )

        assertTrue(
            result is LearningStepDecision.Graduate
        )
    }

    @Test
    fun `relearning uses relearning steps`() {

        val result =
            policy.decide(
                phase = ReviewState.RELEARNING,
                currentStep = null,
                rating = ReviewRating.AGAIN,
            )

        assertEquals(
            LearningStepDecision.Schedule(
                step = 0,
                delay = Duration.ofMinutes(10),
            ),
            result,
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `review state cannot use learning steps`() {

        policy.decide(
            phase = ReviewState.REVIEW,
            currentStep = null,
            rating = ReviewRating.GOOD,
        )
    }
}