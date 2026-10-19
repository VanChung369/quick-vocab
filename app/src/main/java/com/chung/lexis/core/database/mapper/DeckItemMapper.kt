package com.chung.lexis.core.database.mapper

import com.chung.lexis.core.database.entity.DeckItemEntity
import com.chung.lexis.domain.model.DeckItem
import java.time.Instant

fun DeckItemEntity.toDomain(): DeckItem =
    DeckItem(
        deckId = deckId,
        lexemeId = lexemeId,
        addedAt = Instant.ofEpochMilli(
            addedAtEpochMillis
        ),
        sortOrder = sortOrder,
    )

fun DeckItem.toEntity(
    languageTag: String,
): DeckItemEntity =
    DeckItemEntity(
        deckId = deckId,
        lexemeId = lexemeId,
        languageTag = languageTag,
        addedAtEpochMillis = addedAt.toEpochMilli(),
        sortOrder = sortOrder,
    )