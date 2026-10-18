package com.chung.lexis.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "deck_items",

    primaryKeys = [
        "deckId",
        "lexemeId",
    ],

    foreignKeys = [

        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = [
                "id",
                "learningLanguageTag",
            ],
            childColumns = [
                "deckId",
                "languageTag",
            ],
            onDelete = ForeignKey.CASCADE,
        ),

        ForeignKey(
            entity = LexemeEntity::class,
            parentColumns = [
                "id",
                "languageTag",
            ],
            childColumns = [
                "lexemeId",
                "languageTag",
            ],
            onDelete = ForeignKey.CASCADE,
        ),
    ],

    indices = [

        Index(
            value = [
                "deckId",
                "languageTag",
            ],
        ),

        Index(
            value = [
                "lexemeId",
                "languageTag",
            ],
        ),

        Index(
            value = [
                "deckId",
                "sortOrder",
            ],
        ),
    ],
)
data class DeckItemEntity(
    val deckId: Long,

    val lexemeId: Long,

    val languageTag: String,

    val addedAtEpochMillis: Long,

    val sortOrder: Int = 0,
)