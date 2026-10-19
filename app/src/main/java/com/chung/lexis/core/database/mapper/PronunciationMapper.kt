package com.chung.lexis.core.database.mapper

import com.chung.lexis.core.database.entity.PronunciationEntity
import com.chung.lexis.domain.model.Pronunciation

fun PronunciationEntity.toDomain(): Pronunciation =
    Pronunciation(
        id = id,
        lexemeId = lexemeId,
        meaningId = meaningId,
        type = type.toDomain(),
        value = value,
        languageTag = languageTag,
    )

fun Pronunciation.toEntity(): PronunciationEntity =
    PronunciationEntity(
        id = id,
        lexemeId = lexemeId,
        meaningId = meaningId,
        type = type.toColumn(),
        value = value,
        languageTag = languageTag,
    )