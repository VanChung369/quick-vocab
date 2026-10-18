package com.chung.lexis.domain.model

import java.time.Instant

data class DeckItem(
    val deckId: Long,

    val lexemeId: Long,

    val addedAt: Instant,

    val sortOrder: Int = 0,
)