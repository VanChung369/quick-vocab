package com.chung.lexis.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_logs",

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
            value = [
                "lexemeId",
                "reviewedAtEpochMillis",
            ],
        ),

        Index(
            value = ["reviewedAtEpochMillis"],
        ),
    ],
)
data class ReviewLogEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val lexemeId: Long,

    val rating: ReviewRatingColumn,

    val stateBefore: ReviewStateColumn,

    val stateAfter: ReviewStateColumn,

    val reviewedAtEpochMillis: Long,

    val elapsedDays: Int,

    val scheduledDays: Int,
)