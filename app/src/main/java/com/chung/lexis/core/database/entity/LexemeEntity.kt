package com.chung.lexis.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lexemes",
    indices = [
        Index(
            value = [
                "normalizedText",
                "languageTag",
            ],
            unique = true,
        ),

        Index(
            value = [
                "id",
                "languageTag",
            ],
            unique = true,
        ),
    ],
)
data class LexemeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val text: String,

    val normalizedText: String,

    val languageTag: String,

    // Mapper converts this to the domain EnrichmentState
    val enrichmentStatus: EnrichmentStatusColumn = EnrichmentStatusColumn.PENDING,

    val failureCode: String? = null,

    val failureMessage: String? = null,

    val failedAtEpochMillis: Long? = null,

    val retryAfterEpochMillis: Long? = null,
)