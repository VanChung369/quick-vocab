package com.chung.lexis.core.database.mapper

import com.chung.lexis.core.database.entity.DeckEntity
import com.chung.lexis.domain.model.Deck
import java.time.Instant

fun DeckEntity.toDomain(): Deck =
    Deck(
        id = id,
        name = name,
        description = description,
        learningLanguageTag = learningLanguageTag,
        translationLanguageTag = translationLanguageTag,
        createdAt = Instant.ofEpochMilli(
            createdAtEpochMillis
        ),
        updatedAt = Instant.ofEpochMilli(
            updatedAtEpochMillis
        ),
    )

fun Deck.toEntity(): DeckEntity =
    DeckEntity(
        id = id,
        name = name,
        description = description,
        learningLanguageTag = learningLanguageTag,
        translationLanguageTag = translationLanguageTag,
        createdAtEpochMillis = createdAt.toEpochMilli(),
        updatedAtEpochMillis = updatedAt.toEpochMilli(),
    )