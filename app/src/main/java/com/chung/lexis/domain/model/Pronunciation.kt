package com.chung.lexis.domain.model

data class Pronunciation(
    val id: Long = 0L,

    val lexemeId: Long,

    val meaningId: Long? = null,

    val type: PronunciationType,

    val value: String,

    val languageTag: String,
)