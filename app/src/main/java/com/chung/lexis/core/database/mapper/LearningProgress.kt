package com.chung.lexis.core.database.mapper

import com.chung.lexis.core.database.entity.LearningProgressEntity
import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.ReviewState
import java.time.Instant

fun LearningProgressEntity.toDomain(): LearningProgress {
    val domainState = state.toDomain()

    return LearningProgress(
        lexemeId = lexemeId,
        state = domainState,

        step = when (domainState) {
            ReviewState.LEARNING,
            ReviewState.RELEARNING -> step

            ReviewState.NEW,
            ReviewState.REVIEW -> null
        },

        stability = stability,
        difficulty = difficulty,
        reviewCount = reviewCount,
        lapseCount = lapseCount,
        scheduledDays = scheduledDays,

        lastReviewedAt = lastReviewedAtEpochMillis
            ?.let(Instant::ofEpochMilli),

        nextReviewAt = nextReviewAtEpochMillis
            ?.let(Instant::ofEpochMilli),
    )

}

fun LearningProgress.toEntity(): LearningProgressEntity =
    LearningProgressEntity(
        lexemeId = lexemeId,

        state = state.toColumn(),

        step = when (state) {
            ReviewState.LEARNING,
            ReviewState.RELEARNING -> step

            ReviewState.NEW,
            ReviewState.REVIEW -> null
        },

        stability = stability,
        difficulty = difficulty,
        reviewCount = reviewCount,
        lapseCount = lapseCount,
        scheduledDays = scheduledDays,

        lastReviewedAtEpochMillis =
            lastReviewedAt?.toEpochMilli(),

        nextReviewAtEpochMillis =
            nextReviewAt?.toEpochMilli(),
    )