package com.chung.lexis.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "decks",
    indices = [
        Index(
            value = [
                "id",
                "learningLanguageTag",
            ],
            unique = true,
        ),
    ],
)
data class DeckEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val name: String,

    val description: String? = null,

    val learningLanguageTag: String,

    val translationLanguageTag: String,

    val createdAtEpochMillis: Long,

    val updatedAtEpochMillis: Long,
)