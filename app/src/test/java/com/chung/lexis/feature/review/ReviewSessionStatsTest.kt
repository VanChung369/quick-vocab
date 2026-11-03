package com.chung.lexis.feature.review

import com.chung.lexis.domain.model.ReviewRating
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewSessionStatsTest {

    @Test
    fun `record counts each rating`() {

        val stats =
            ReviewSessionStats()
                .record(lexemeId = 1L, rating = ReviewRating.AGAIN)
                .record(lexemeId = 2L, rating = ReviewRating.HARD)
                .record(lexemeId = 3L, rating = ReviewRating.GOOD)
                .record(lexemeId = 4L, rating = ReviewRating.EASY)
                .record(lexemeId = 5L, rating = ReviewRating.GOOD)

        assertEquals(1, stats.againCount)
        assertEquals(1, stats.hardCount)
        assertEquals(2, stats.goodCount)
        assertEquals(1, stats.easyCount)
        assertEquals(5, stats.reviewedCount)
        assertEquals(3, stats.rememberedCount)
    }

    @Test
    fun `difficult words are listed once in order of first difficulty`() {

        val stats =
            ReviewSessionStats()
                .record(lexemeId = 2L, rating = ReviewRating.HARD)
                .record(lexemeId = 1L, rating = ReviewRating.GOOD)
                .record(lexemeId = 1L, rating = ReviewRating.AGAIN)
                .record(lexemeId = 2L, rating = ReviewRating.AGAIN)
                .record(lexemeId = 1L, rating = ReviewRating.HARD)

        assertEquals(
            listOf(2L, 1L),
            stats.difficultLexemeIds,
        )
    }

    @Test
    fun `good and easy do not mark a word difficult`() {

        val stats =
            ReviewSessionStats()
                .record(lexemeId = 1L, rating = ReviewRating.GOOD)
                .record(lexemeId = 2L, rating = ReviewRating.EASY)

        assertEquals(
            emptyList<Long>(),
            stats.difficultLexemeIds,
        )
    }
}
