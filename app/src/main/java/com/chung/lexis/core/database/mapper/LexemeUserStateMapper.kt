package com.chung.lexis.core.database.mapper

import com.chung.lexis.core.database.entity.LexemeUserStateEntity
import com.chung.lexis.domain.model.LexemeUserState

fun LexemeUserStateEntity.toDomain(): LexemeUserState =
    LexemeUserState(
        lexemeId = lexemeId,
        isFavorite = isFavorite,
    )

fun LexemeUserState.toEntity(): LexemeUserStateEntity =
    LexemeUserStateEntity(
        lexemeId = lexemeId,
        isFavorite = isFavorite,
    )