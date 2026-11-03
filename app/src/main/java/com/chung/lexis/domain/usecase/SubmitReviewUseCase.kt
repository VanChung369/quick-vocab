package com.chung.lexis.domain.usecase

import com.chung.lexis.domain.model.CorruptReviewDataException
import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.ReviewLog
import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.model.requireReviewed
import com.chung.lexis.domain.repository.ReviewRepository
import com.chung.lexis.domain.repository.SaveReviewResult
import com.chung.lexis.domain.scheduler.ReviewSchedule
import com.chung.lexis.domain.scheduler.ReviewScheduler
import java.time.Instant

class SubmitReviewUseCase(
    private val reviewRepository: ReviewRepository,
    private val reviewScheduler: ReviewScheduler,
) {

    /**
     * @param progress snapshot user đã thấy khi rating
     * (ReviewCard.progress). Repository dùng nó làm mốc
     * compare-and-set: write khác xen vào → StaleProgress.
     */
    suspend operator fun invoke(
        progress: LearningProgress,
        rating: ReviewRating,
        reviewedAt: Instant,
    ): SaveReviewResult {

        /*
         * Clock máy có thể bị lùi (user chỉnh giờ, NTP sync).
         * Không throw: kẹp về lastReviewedAt để
         * thời điểm review không bao giờ đi lùi.
         */
        val effectiveReviewedAt =
            progress.lastReviewedAt
                ?.let { lastReviewedAt ->
                    maxOf(
                        reviewedAt,
                        lastReviewedAt,
                    )
                }
                ?: reviewedAt

        /*
         * Scheduler là pure function:
         * require/check fail ở đây là data hỏng,
         * retry không giúp gì.
         */
        val schedule =
            try {
                scheduleChecked(
                    current = progress,
                    rating = rating,
                    reviewedAt = effectiveReviewedAt,
                )
            } catch (
                invalid: IllegalArgumentException,
            ) {
                throw CorruptReviewDataException(
                    lexemeId = progress.lexemeId,
                    cause = invalid,
                )
            } catch (
                invalid: IllegalStateException,
            ) {
                throw CorruptReviewDataException(
                    lexemeId = progress.lexemeId,
                    cause = invalid,
                )
            }

        val reviewLog =
            ReviewLog(
                lexemeId = progress.lexemeId,
                rating = rating,

                stateBefore =
                    progress.state,

                stateAfter =
                    schedule.progress.state,

                reviewedAt =
                    effectiveReviewedAt,

                elapsedDays =
                    schedule.elapsedDays,

                scheduledDays =
                    schedule.scheduledDays,
            )

        return reviewRepository.saveReview(
            previousProgress = progress,
            nextProgress = schedule.progress,
            reviewLog = reviewLog,
        )
    }

    private fun scheduleChecked(
        current: LearningProgress,
        rating: ReviewRating,
        reviewedAt: Instant,
    ): ReviewSchedule {

        val schedule =
            reviewScheduler.schedule(
                current = current,
                rating = rating,
                reviewedAt = reviewedAt,
            )

        val nextReviewAt =
            requireNotNull(
                schedule.progress.nextReviewAt
            ) {
                "Scheduler must set nextReviewAt"
            }

        require(
            nextReviewAt.toEpochMilli() >=
                    reviewedAt.toEpochMilli()
        ) {
            "nextReviewAt cannot be before reviewedAt"
        }

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

        require(schedule.elapsedDays >= 0) {
            "elapsedDays must be >= 0"
        }

        require(schedule.scheduledDays >= 0) {
            "scheduledDays must be >= 0"
        }

        require(
            schedule.progress.reviewCount ==
                    current.reviewCount + 1
        ) {
            "Scheduler must increment reviewCount exactly once"
        }

        /*
         * Check trước khi save để vi phạm
         * thành CorruptReviewDataException (skip card),
         * thay vì saveReview fail → SAVE_FAILED retry mãi.
         */
        schedule.progress.requireReviewed()

        return schedule
    }
}
