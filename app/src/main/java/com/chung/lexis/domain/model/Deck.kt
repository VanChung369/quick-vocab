package com.chung.lexis.domain.model

import java.time.Instant

data class Deck(
    val id: Long = 0L,

    val name: String,

    val description: String? = null,

    val learningLanguageTag: String,

    val translationLanguageTag: String,

    val createdAt: Instant,

    val updatedAt: Instant,
)