package com.chung.lexis.core.database.mapper

import com.chung.lexis.core.database.entity.EnrichmentStatusColumn
import com.chung.lexis.core.database.entity.LexemeEntity
import com.chung.lexis.domain.model.EnrichmentFailure
import com.chung.lexis.domain.model.EnrichmentState
import com.chung.lexis.domain.model.Lexeme
import java.time.Instant

fun LexemeEntity.toDomain(): Lexeme {
    return Lexeme(
        id = id,
        text = text,
        normalizedText = normalizedText,
        languageTag = languageTag,
        enrichmentState = toEnrichmentState(),
    )
}


private fun LexemeEntity.toEnrichmentState(): EnrichmentState {
    return when (enrichmentStatus) {

        EnrichmentStatusColumn.FAILED -> {
            EnrichmentState.Failed(
                failure = EnrichmentFailure(
                    code = failureCode,
                    message = requireNotNull(failureMessage),
                    failedAt = Instant.ofEpochMilli(
                        requireNotNull(failedAtEpochMillis)
                    ),
                    retryAfter = retryAfterEpochMillis
                        ?.let(Instant::ofEpochMilli),
                )
            )
        }

        EnrichmentStatusColumn.PENDING ->
            EnrichmentState.Pending

        EnrichmentStatusColumn.ENRICHING ->
            EnrichmentState.Enriching

        EnrichmentStatusColumn.READY ->
            EnrichmentState.Ready
    }
}