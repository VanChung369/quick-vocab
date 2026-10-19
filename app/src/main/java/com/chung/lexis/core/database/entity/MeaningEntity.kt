package com.chung.lexis.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meanings",

    foreignKeys = [
        ForeignKey(
            entity = LexemeEntity::class,
            parentColumns = ["id"],
            childColumns = ["lexemeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],

    indices = [
        Index(
            value = ["lexemeId"],
        ),

        Index(
            value = [
                "lexemeId",
                "sortOrder",
            ],
        ),

        /*
         * Required so Pronunciation / Example can reference
         * (meaningId, lexemeId) together.
         */
        Index(
            value = [
                "id",
                "lexemeId",
            ],
            unique = true,
        ),
    ],
)
data class MeaningEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val lexemeId: Long,

    val kind: MeaningKindColumn,

    val languageTag: String,

    val text: String,

    val partOfSpeech: String? = null,

    val sortOrder: Int = 0,
)