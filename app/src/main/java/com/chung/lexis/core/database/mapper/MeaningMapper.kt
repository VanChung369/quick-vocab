package com.chung.lexis.core.database.mapper

import com.chung.lexis.core.database.entity.MeaningEntity
import com.chung.lexis.domain.model.Meaning

fun MeaningEntity.toDomain(): Meaning =
    Meaning(
        id = id,
        lexemeId = lexemeId,
        kind = kind.toDomain(),
        languageTag = languageTag,
        text = text,
        partOfSpeech = partOfSpeech,
        sortOrder = sortOrder,
    )

fun Meaning.toEntity(): MeaningEntity =
    MeaningEntity(
        id = id,
        lexemeId = lexemeId,
        kind = kind.toColumn(),
        languageTag = languageTag,
        text = text,
        partOfSpeech = partOfSpeech,
        sortOrder = sortOrder,
    )