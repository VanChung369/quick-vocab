package com.chung.lexis.domain.scheduler

import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.ReviewRating
import kotlin.math.E
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

class Fsrs6MemoryPolicy(
    private val config: Fsrs6Config =
        Fsrs6Config(),
) : ReviewMemoryPolicy {

    private val w =
        config.parameters

    /*
     * Official implementation uses:
     *
     * decay = -w[20]
     * factor = 0.9^(1 / decay) - 1
     */
    private val decay =
        -w[20]

    private val factor =
        0.9.pow(1.0 / decay) - 1.0

    override fun evaluate(
        current: LearningProgress,
        rating: ReviewRating,
        elapsedDays: Int,
    ): ReviewMemoryDecision {

        require(elapsedDays >= 0) {
            "elapsedDays must be >= 0"
        }

        require(
            (current.stability == null) ==
                    (current.difficulty == null)
        ) {
            "stability and difficulty must both be null or both be set"
        }

        val grade =
            rating.toFsrsGrade()

        val currentStability =
            current.stability

        val currentDifficulty =
            current.difficulty

        val nextState =
            when {

                /*
                 * First ever review.
                 */
                currentStability == null &&
                        currentDifficulty == null -> {

                    MemoryState(
                        stability =
                            initialStability(grade),

                        difficulty =
                            initialDifficulty(
                                grade = grade,
                                clamp = true,
                            ),
                    )
                }

                /*
                 * FSRS-6 treats reviews with
                 * elapsedDays < 1 separately.
                 *
                 * Because elapsedDays is Int here,
                 * this means elapsedDays == 0.
                 */
                elapsedDays == 0 -> {

                    MemoryState(
                        stability =
                            shortTermStability(
                                stability =
                                    currentStability!!,
                                grade = grade,
                            ),

                        difficulty =
                            nextDifficulty(
                                difficulty =
                                    currentDifficulty!!,
                                grade = grade,
                            ),
                    )
                }

                else -> {

                    val stability =
                        currentStability!!

                    val difficulty =
                        currentDifficulty!!

                    val retrievability =
                        retrievability(
                            elapsedDays =
                                elapsedDays,
                            stability =
                                stability,
                        )

                    val nextStability =
                        when (rating) {

                            ReviewRating.AGAIN ->
                                forgetStability(
                                    difficulty =
                                        difficulty,
                                    stability =
                                        stability,
                                    retrievability =
                                        retrievability,
                                )

                            ReviewRating.HARD,
                            ReviewRating.GOOD,
                            ReviewRating.EASY ->
                                recallStability(
                                    difficulty =
                                        difficulty,
                                    stability =
                                        stability,
                                    retrievability =
                                        retrievability,
                                    rating =
                                        rating,
                                )
                        }

                    MemoryState(
                        stability =
                            clampStability(
                                nextStability
                            ),

                        difficulty =
                            nextDifficulty(
                                difficulty =
                                    difficulty,
                                grade =
                                    grade,
                            ),
                    )
                }
            }

        return ReviewMemoryDecision(
            stability =
                nextState.stability,

            difficulty =
                nextState.difficulty,

            intervalDays =
                nextInterval(
                    stability =
                        nextState.stability,
                ),
        )
    }

    private fun initialStability(
        grade: Int,
    ): Double =
        clampStability(
            w[grade - 1]
        )

    private fun initialDifficulty(
        grade: Int,
        clamp: Boolean,
    ): Double {

        val difficulty =
            w[4] -
                    exp(
                        w[5] *
                                (grade - 1)
                    ) +
                    1.0

        return if (clamp) {
            clampDifficulty(difficulty)
        } else {
            difficulty
        }
    }

    private fun nextDifficulty(
        difficulty: Double,
        grade: Int,
    ): Double {

        val deltaDifficulty =
            -w[6] *
                    (grade - 3)

        val dampedDelta =
            (10.0 - difficulty) *
                    deltaDifficulty /
                    9.0

        val candidate =
            difficulty +
                    dampedDelta

        /*
         * FSRS-5/6 mean reversion target:
         * initial difficulty for EASY.
         */
        val easyDifficulty =
            initialDifficulty(
                grade = 4,
                clamp = false,
            )

        val reverted =
            w[7] *
                    easyDifficulty +
                    (1.0 - w[7]) *
                    candidate

        return clampDifficulty(
            reverted
        )
    }

    private fun retrievability(
        elapsedDays: Int,
        stability: Double,
    ): Double =
        (
                1.0 +
                        factor *
                        elapsedDays.toDouble() /
                        stability
                ).pow(decay)

    private fun shortTermStability(
        stability: Double,
        grade: Int,
    ): Double {

        var increase =
            E.pow(
                w[17] *
                        (
                                grade -
                                        3 +
                                        w[18]
                                )
            ) *
                    stability.pow(
                        -w[19]
                    )

        /*
         * Hard / Good / Easy must not
         * decrease stability on short-term review.
         */
        if (grade >= 2) {
            increase =
                max(
                    increase,
                    1.0,
                )
        }

        return clampStability(
            stability *
                    increase
        )
    }

    private fun recallStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        rating: ReviewRating,
    ): Double {

        val hardPenalty =
            if (
                rating ==
                ReviewRating.HARD
            ) {
                w[15]
            } else {
                1.0
            }

        val easyBonus =
            if (
                rating ==
                ReviewRating.EASY
            ) {
                w[16]
            } else {
                1.0
            }

        return stability *
                (
                        1.0 +
                                exp(w[8]) *
                                (11.0 - difficulty) *
                                stability.pow(-w[9]) *
                                (
                                        exp(
                                            (1.0 - retrievability) *
                                                    w[10]
                                        ) -
                                                1.0
                                        ) *
                                hardPenalty *
                                easyBonus
                        )
    }

    private fun forgetStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
    ): Double {

        val longTerm =
            w[11] *
                    difficulty.pow(-w[12]) *
                    (
                            (stability + 1.0)
                                .pow(w[13]) -
                                    1.0
                            ) *
                    exp(
                        (1.0 - retrievability) *
                                w[14]
                    )

        /*
         * FSRS-6 also places a short-term
         * upper bound on post-lapse stability.
         */
        val shortTerm =
            stability /
                    exp(
                        w[17] *
                                w[18]
                    )

        return min(
            longTerm,
            shortTerm,
        )
    }

    private fun nextInterval(
        stability: Double,
    ): Int {

        val rawInterval =
            (
                    stability /
                            factor
                    ) *
                    (
                            config.desiredRetention
                                .pow(
                                    1.0 /
                                            decay
                                ) -
                                    1.0
                            )

        require(rawInterval.isFinite()) {
            "FSRS produced a non-finite interval"
        }

        return round(rawInterval)
            .coerceIn(
                1.0,
                config.maximumIntervalDays
                    .toDouble(),
            )
            .toInt()
    }

    private fun clampStability(
        stability: Double,
    ): Double =
        max(
            stability,
            STABILITY_MIN,
        )

    private fun clampDifficulty(
        difficulty: Double,
    ): Double =
        difficulty.coerceIn(
            1.0,
            10.0,
        )

    private fun ReviewRating.toFsrsGrade():
            Int =
        when (this) {
            ReviewRating.AGAIN -> 1
            ReviewRating.HARD -> 2
            ReviewRating.GOOD -> 3
            ReviewRating.EASY -> 4
        }

    private data class MemoryState(
        val stability: Double,
        val difficulty: Double,
    )

    companion object {
        private const val STABILITY_MIN =
            0.001
    }
}