package com.chung.lexis.domain.scheduler

import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.model.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Test


class Fsrs6MemoryPolicyTest {
    @Test
    fun `new good uses fsrs6 initial memory state`() {

        val policy =
            Fsrs6MemoryPolicy()

        val result =
            policy.evaluate(
                current =
                    LearningProgress(
                        lexemeId = 1L,
                    ),
                rating =
                    ReviewRating.GOOD,
                elapsedDays = 0,
            )

        assertEquals(
            2.3065,
            result.stability,
            1e-9,
        )

        assertEquals(
            2.118103970459015,
            result.difficulty,
            1e-9,
        )

        assertEquals(
            2,
            result.intervalDays,
        )
    }


    @Test
    fun `good review updates long term fsrs memory`() {

        val policy =
            Fsrs6MemoryPolicy()

        val result =
            policy.evaluate(
                current =
                    LearningProgress(
                        lexemeId = 1L,
                        state = ReviewState.REVIEW,
                        stability = 10.0,
                        difficulty = 5.0,
                    ),
                rating =
                    ReviewRating.GOOD,
                elapsedDays = 10,
            )

        assertEquals(
            32.02672948198673,
            result.stability,
            1e-9,
        )

        assertEquals(
            4.990228369296839,
            result.difficulty,
            1e-9,
        )

        assertEquals(
            32,
            result.intervalDays,
        )
    }


    @Test
    fun `again uses fsrs forgetting formula`() {

        val policy =
            Fsrs6MemoryPolicy()

        val result =
            policy.evaluate(
                current =
                    LearningProgress(
                        lexemeId = 1L,
                        state = ReviewState.REVIEW,
                        stability = 10.0,
                        difficulty = 5.0,
                    ),
                rating =
                    ReviewRating.AGAIN,
                elapsedDays = 10,
            )

        assertEquals(
            1.3919869729546932,
            result.stability,
            1e-9,
        )

        assertEquals(
            8.341762369296838,
            result.difficulty,
            1e-9,
        )

        assertEquals(
            1,
            result.intervalDays,
        )
    }

    @Test
    fun `short term hard can decrease stability`() {

        val result =
            Fsrs6MemoryPolicy()
                .evaluate(
                    current = afterFirstGood(),
                    rating = ReviewRating.HARD,
                    elapsedDays = 0,
                )

        assertEquals(
            1.3333787168039835,
            result.stability,
            1e-9,
        )

        assertEquals(
            4.752858488532556,
            result.difficulty,
            1e-9,
        )

        assertEquals(
            1,
            result.intervalDays,
        )
    }

    @Test
    fun `short term good does not decrease stability`() {

        val result =
            Fsrs6MemoryPolicy()
                .evaluate(
                    current = afterFirstGood(),
                    rating = ReviewRating.GOOD,
                    elapsedDays = 0,
                )

        assertEquals(
            2.3065,
            result.stability,
            1e-9,
        )

        assertEquals(
            2.1112142357853942,
            result.difficulty,
            1e-9,
        )
    }

    @Test
    fun `long term hard applies hard penalty`() {

        val result =
            Fsrs6MemoryPolicy()
                .evaluate(
                    current = reviewState(),
                    rating = ReviewRating.HARD,
                    elapsedDays = 10,
                )

        assertEquals(
            23.246875110466817,
            result.stability,
            1e-9,
        )

        assertEquals(
            6.665995369296838,
            result.difficulty,
            1e-9,
        )

        assertEquals(
            23,
            result.intervalDays,
        )
    }

    @Test
    fun `long term easy applies easy bonus`() {

        val result =
            Fsrs6MemoryPolicy()
                .evaluate(
                    current = reviewState(),
                    rating = ReviewRating.EASY,
                    elapsedDays = 10,
                )

        assertEquals(
            51.25386164681294,
            result.stability,
            1e-9,
        )

        assertEquals(
            3.3144613692968385,
            result.difficulty,
            1e-9,
        )

        assertEquals(
            51,
            result.intervalDays,
        )
    }

    @Test
    fun `lower desired retention gives longer interval`() {

        val result =
            Fsrs6MemoryPolicy(
                config = Fsrs6Config(
                    desiredRetention = 0.8,
                ),
            ).evaluate(
                current = LearningProgress(lexemeId = 1L),
                rating = ReviewRating.GOOD,
                elapsedDays = 0,
            )

        assertEquals(
            2.3065,
            result.stability,
            1e-9,
        )

        // raw interval = 7.648... days
        assertEquals(
            8,
            result.intervalDays,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `stability and difficulty must exist together`() {

        Fsrs6MemoryPolicy()
            .evaluate(
                current =
                    LearningProgress(
                        lexemeId = 1L,
                        stability = 2.0,
                        difficulty = null,
                    ),
                rating =
                    ReviewRating.GOOD,
                elapsedDays = 1,
            )
    }

    /*
     * Memory state right after NEW + GOOD.
     */
    private fun afterFirstGood(): LearningProgress =
        LearningProgress(
            lexemeId = 1L,
            state = ReviewState.LEARNING,
            step = 0,
            stability = 2.3065,
            difficulty = 2.118103970459015,
        )

    private fun reviewState(): LearningProgress =
        LearningProgress(
            lexemeId = 1L,
            state = ReviewState.REVIEW,
            stability = 10.0,
            difficulty = 5.0,
        )
}

