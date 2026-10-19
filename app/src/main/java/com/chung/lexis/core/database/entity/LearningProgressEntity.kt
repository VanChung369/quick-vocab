package com.chung.lexis.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "learning_progress",

    foreignKeys = [
        ForeignKey(
            entity = LexemeEntity::class,
            parentColumns = ["id"],
            childColumns = ["lexemeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],

    indices = [

        /*
         * Main index for:
         *
         * WHERE nextReviewAtEpochMillis <= now
         */
        Index(
            value = ["nextReviewAtEpochMillis"],
        ),

        Index(
            value = [
                "state",
                "nextReviewAtEpochMillis",
            ],
        ),
    ],
)
data class LearningProgressEntity(

    @PrimaryKey
    val lexemeId: Long,

    val state: ReviewStateColumn = ReviewStateColumn.NEW,

    val step: Int? = null,

    val stability: Double? = null,

    val difficulty: Double? = null,

    val reviewCount: Int = 0,

    val lapseCount: Int = 0,

    val scheduledDays: Int = 0,

    val lastReviewedAtEpochMillis: Long? = null,

    val nextReviewAtEpochMillis: Long? = null,
)