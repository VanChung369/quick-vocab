package com.chung.lexis.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/*
 * Lexeme có review data hỏng.
 *
 * Queue và due summary bỏ qua lexeme có row ở đây,
 * thay vì mỗi session load lại rồi skip.
 */
@Entity(
    tableName = "review_quarantine",

    foreignKeys = [
        ForeignKey(
            entity = LexemeEntity::class,
            parentColumns = ["id"],
            childColumns = ["lexemeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ReviewQuarantineEntity(

    @PrimaryKey
    val lexemeId: Long,

    val reason: String,

    val detectedAtEpochMillis: Long,
)
