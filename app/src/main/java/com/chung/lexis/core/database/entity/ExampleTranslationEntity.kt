package com.chung.lexis.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "example_translations",

    foreignKeys = [
        ForeignKey(
            entity = ExampleEntity::class,
            parentColumns = ["id"],
            childColumns = ["exampleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExampleTranslationEntity(

    @androidx.room.PrimaryKey
    val exampleId: Long,

    val text: String,

    val languageTag: String,
)