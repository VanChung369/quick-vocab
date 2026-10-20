package com.chung.lexis.domain.usecase

import com.chung.lexis.domain.model.ReviewLog
import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.repository.ReviewRepository
import com.chung.lexis.domain.repository.SaveReviewResult
import com.chung.lexis.domain.scheduler.ReviewScheduler
import java.time.Instant

class SubmitReviewUseCase(
    private val reviewRepository: ReviewRepository,
    private val reviewScheduler: ReviewScheduler,
) {

    suspend operator fun invoke(
        lexemeId: Long,
        rating: ReviewRating,
        reviewedAt: Instant,
    ): SaveReviewResult {

        val current =
            reviewRepository.getProgress(lexemeId)
                ?: return SaveReviewResult.LexemeNotFound

        val schedule =
            reviewScheduler.schedule(
                current = current,
                rating = rating,
                reviewedAt = reviewedAt,
            )

        require(
            schedule.progress.lexemeId ==
                    current.lexemeId
        ) {
            "Scheduler changed lexemeId"
        }

        require(
            schedule.progress.lastReviewedAt
                ?.toEpochMilli() ==
                    reviewedAt.toEpochMilli()
        ) {
            "Scheduler must set lastReviewedAt"
        }

        require(
            schedule.progress.scheduledDays ==
                    schedule.scheduledDays
        ) {
            "scheduledDays must match progress"
        }

        val reviewLog =
            ReviewLog(
                lexemeId = lexemeId,
                rating = rating,

                stateBefore =
                    current.state,

                stateAfter =
                    schedule.progress.state,

                reviewedAt =
                    reviewedAt,

                elapsedDays =
                    schedule.elapsedDays,

                scheduledDays =
                    schedule.scheduledDays,
            )

        return reviewRepository.saveReview(
            previousProgress = current,
            nextProgress = schedule.progress,
            reviewLog = reviewLog,
        )
    }
}