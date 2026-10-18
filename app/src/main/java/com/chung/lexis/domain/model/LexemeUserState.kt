package com.chung.lexis.domain.model

data class LexemeUserState(
    val lexemeId: Long,
    val isFavorite: Boolean = false,
)