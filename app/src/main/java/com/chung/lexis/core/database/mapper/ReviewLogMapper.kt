package com.chung.lexis.core.database.mapper

import com.chung.lexis.core.database.entity.ReviewLogEntity
import com.chung.lexis.domain.model.ReviewLog
import java.time.Instant

fun ReviewLogEntity.toDomain(): ReviewLog =
    ReviewLog(
        id = id,
        lexemeId = lexemeId,
        rating = rating.toDomain(),
        stateBefore = stateBefore.toDomain(),
        stateAfter = stateAfter.toDomain(),

        reviewedAt = Instant.ofEpochMilli(
            reviewedAtEpochMillis
        ),

        elapsedDays = elapsedDays,
        scheduledDays = scheduledDays,
    )

fun ReviewLog.toEntity(): ReviewLogEntity =
    ReviewLogEntity(
        id = id,
        lexemeId = lexemeId,
        rating = rating.toColumn(),
        stateBefore = stateBefore.toColumn(),
        stateAfter = stateAfter.toColumn(),
        reviewedAtEpochMillis =
            reviewedAt.toEpochMilli(),
        elapsedDays = elapsedDays,
        scheduledDays = scheduledDays,
    )