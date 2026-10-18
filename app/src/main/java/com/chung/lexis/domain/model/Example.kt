package com.chung.lexis.domain.model

data class Example(
    val id: Long = 0L,

    val lexemeId: Long,

    val meaningId: Long? = null,

    val sentence: LocalizedText,

    val translation: LocalizedText? = null,
)
