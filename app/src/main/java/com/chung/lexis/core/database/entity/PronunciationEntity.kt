package com.chung.lexis.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pronunciations",

    foreignKeys = [

        ForeignKey(
            entity = LexemeEntity::class,
            parentColumns = ["id"],
            childColumns = ["lexemeId"],
            onDelete = ForeignKey.CASCADE,
        ),

        ForeignKey(
            entity = MeaningEntity::class,
            parentColumns = [
                "id",
                "lexemeId",
            ],
            childColumns = [
                "meaningId",
                "lexemeId",
            ],
            onDelete = ForeignKey.CASCADE,
        ),
    ],

    indices = [

        Index(
            value = ["lexemeId"],
        ),

        Index(
            value = [
                "meaningId",
                "lexemeId",
            ],
        ),
    ],
)
data class PronunciationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val lexemeId: Long,

    val meaningId: Long? = null,

    val type: PronunciationTypeColumn,

    val value: String,

    val languageTag: String,
)