package com.chung.lexis.domain.scheduler

import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.model.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewStateMachineTest {

    private val machine =
        ReviewStateMachine()

    @Test
    fun `new again enters learning`() {
        val result =
            machine.transition(
                currentState = ReviewState.NEW,
                rating = ReviewRating.AGAIN,
            )

        assertEquals(
            ReviewState.LEARNING,
            result.nextState,
        )
    }

    @Test
    fun `new easy graduates directly to review`() {
        val result =
            machine.transition(
                currentState = ReviewState.NEW,
                rating = ReviewRating.EASY,
            )

        assertEquals(
            ReviewState.REVIEW,
            result.nextState,
        )
    }

    @Test
    fun `learning good stays learning when steps remain`() {
        val result =
            machine.transition(
                currentState = ReviewState.LEARNING,
                rating = ReviewRating.GOOD,
                learningStepCompleted = false,
            )

        assertEquals(
            ReviewState.LEARNING,
            result.nextState,
        )
    }

    @Test
    fun `learning good graduates when steps completed`() {
        val result =
            machine.transition(
                currentState = ReviewState.LEARNING,
                rating = ReviewRating.GOOD,
                learningStepCompleted = true,
            )

        assertEquals(
            ReviewState.REVIEW,
            result.nextState,
        )
    }

    @Test
    fun `review again enters relearning and increments lapse`() {
        val result =
            machine.transition(
                currentState = ReviewState.REVIEW,
                rating = ReviewRating.AGAIN,
            )

        assertEquals(
            ReviewState.RELEARNING,
            result.nextState,
        )

        assertTrue(result.isLapse)
    }

    @Test
    fun `relearning again does not increment lapse again`() {
        val result =
            machine.transition(
                currentState = ReviewState.RELEARNING,
                rating = ReviewRating.AGAIN,
            )

        assertEquals(
            ReviewState.RELEARNING,
            result.nextState,
        )

        assertFalse(result.isLapse)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `learningStepCompleted is invalid for easy`() {
        machine.transition(
            currentState = ReviewState.LEARNING,
            rating = ReviewRating.EASY,
            learningStepCompleted = true,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `learningStepCompleted is invalid in review`() {
        machine.transition(
            currentState = ReviewState.REVIEW,
            rating = ReviewRating.GOOD,
            learningStepCompleted = true,
        )
    }
}