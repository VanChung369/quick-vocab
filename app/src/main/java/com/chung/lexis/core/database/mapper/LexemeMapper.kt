package com.chung.lexis.core.database.mapper

import com.chung.lexis.core.database.entity.EnrichmentStatusColumn
import com.chung.lexis.core.database.entity.LexemeEntity
import com.chung.lexis.domain.model.EnrichmentFailure
import com.chung.lexis.domain.model.EnrichmentState
import com.chung.lexis.domain.model.Lexeme
import java.time.Instant

fun LexemeEntity.toDomain(): Result<Lexeme> =
    runCatching {
        Lexeme(
            id = id,
            text = text,
            normalizedText = normalizedText,
            languageTag = languageTag,
            enrichmentState = toEnrichmentState(),
        )
    }

private fun LexemeEntity.toEnrichmentState(): EnrichmentState =
    when (enrichmentStatus) {
        EnrichmentStatusColumn.PENDING ->
            EnrichmentState.Pending

        EnrichmentStatusColumn.ENRICHING ->
            EnrichmentState.Enriching

        EnrichmentStatusColumn.READY ->
            EnrichmentState.Ready

        EnrichmentStatusColumn.FAILED -> {
            val message = failureMessage
                ?: error(
                    "FAILED lexeme $id has no failureMessage"
                )

            val failedAtMillis = failedAtEpochMillis
                ?: error(
                    "FAILED lexeme $id has no failedAtEpochMillis"
                )

            EnrichmentState.Failed(
                failure = EnrichmentFailure(
                    code = failureCode,
                    message = message,
                    failedAt = Instant.ofEpochMilli(
                        failedAtMillis
                    ),
                    retryAfter = retryAfterEpochMillis
                        ?.let(Instant::ofEpochMilli),
                )
            )
        }
    }


fun Lexeme.toEntity(): LexemeEntity {
    return when (val state = enrichmentState) {

        EnrichmentState.Pending ->
            baseEntity(
                status = EnrichmentStatusColumn.PENDING,
            )

        EnrichmentState.Enriching ->
            baseEntity(
                status = EnrichmentStatusColumn.ENRICHING,
            )

        EnrichmentState.Ready ->
            baseEntity(
                status = EnrichmentStatusColumn.READY,
            )

        is EnrichmentState.Failed ->
            LexemeEntity(
                id = id,
                text = text,
                normalizedText = normalizedText,
                languageTag = languageTag,
                enrichmentStatus =
                    EnrichmentStatusColumn.FAILED,
                failureCode = state.failure.code,
                failureMessage = state.failure.message,
                failedAtEpochMillis =
                    state.failure.failedAt.toEpochMilli(),
                retryAfterEpochMillis =
                    state.failure.retryAfter?.toEpochMilli(),
            )
    }
}

private fun Lexeme.baseEntity(
    status: EnrichmentStatusColumn,
): LexemeEntity =
    LexemeEntity(
        id = id,
        text = text,
        normalizedText = normalizedText,
        languageTag = languageTag,
        enrichmentStatus = status,

        // Important: clear stale failure state.
        failureCode = null,
        failureMessage = null,
        failedAtEpochMillis = null,
        retryAfterEpochMillis = null,
    )