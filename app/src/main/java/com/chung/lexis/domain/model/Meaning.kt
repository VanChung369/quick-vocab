package com.chung.lexis.domain.model

data class Meaning(
    val id: Long = 0L,

    val lexemeId: Long,

    val kind: MeaningKind,

    val languageTag: String,

    val text: String,

    val partOfSpeech: String? = null,

    val sortOrder: Int = 0,
)