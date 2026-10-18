package com.chung.lexis.domain.model

// (normalizedText, languageTag) must be unique: learning progress is keyed by lexemeId
data class Lexeme(
    val id: Long = 0L,

    val text: String,

    val normalizedText: String,

    val languageTag: String,
    
    val enrichmentState: EnrichmentState = EnrichmentState.Pending,
)


data class LexemeUserState(
    val lexemeId: Long,
    val isFavorite: Boolean = false,
)