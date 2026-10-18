package com.chung.lexis.domain.model

import java.time.Instant

sealed interface EnrichmentState {
    data object Pending : EnrichmentState

    data object Enriching : EnrichmentState

    data object Ready : EnrichmentState

    data class Failed(
        val failure: EnrichmentFailure,
    ) : EnrichmentState
}

data class EnrichmentFailure(
    val code: String? = null,
    val message: String,
    val failedAt: Instant,
    val retryAfter: Instant? = null,
)
