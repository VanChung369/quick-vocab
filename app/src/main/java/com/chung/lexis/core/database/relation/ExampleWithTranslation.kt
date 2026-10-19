package com.chung.lexis.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.chung.lexis.core.database.entity.ExampleEntity
import com.chung.lexis.core.database.entity.ExampleTranslationEntity

data class ExampleWithTranslation(
    @Embedded
    val example: ExampleEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "exampleId",
    )
    val translation: ExampleTranslationEntity?,
)