package com.chung.lexis.core.database.mapper

import com.chung.lexis.core.database.entity.MeaningKindColumn
import com.chung.lexis.core.database.entity.PronunciationTypeColumn
import com.chung.lexis.core.database.entity.ReviewRatingColumn
import com.chung.lexis.core.database.entity.ReviewStateColumn
import com.chung.lexis.domain.model.MeaningKind
import com.chung.lexis.domain.model.PronunciationType
import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.model.ReviewState

fun MeaningKindColumn.toDomain(): MeaningKind =
    when (this) {
        MeaningKindColumn.TRANSLATION -> MeaningKind.TRANSLATION
        MeaningKindColumn.DEFINITION -> MeaningKind.DEFINITION
    }

fun MeaningKind.toColumn(): MeaningKindColumn =
    when (this) {
        MeaningKind.TRANSLATION -> MeaningKindColumn.TRANSLATION
        MeaningKind.DEFINITION -> MeaningKindColumn.DEFINITION
    }

fun PronunciationTypeColumn.toDomain(): PronunciationType =
    when (this) {
        PronunciationTypeColumn.IPA -> PronunciationType.IPA
        PronunciationTypeColumn.PINYIN -> PronunciationType.PINYIN
        PronunciationTypeColumn.KANA -> PronunciationType.KANA
        PronunciationTypeColumn.ROMAJI -> PronunciationType.ROMAJI
        PronunciationTypeColumn.OTHER -> PronunciationType.OTHER
    }

fun PronunciationType.toColumn(): PronunciationTypeColumn =
    when (this) {
        PronunciationType.IPA -> PronunciationTypeColumn.IPA
        PronunciationType.PINYIN -> PronunciationTypeColumn.PINYIN
        PronunciationType.KANA -> PronunciationTypeColumn.KANA
        PronunciationType.ROMAJI -> PronunciationTypeColumn.ROMAJI
        PronunciationType.OTHER -> PronunciationTypeColumn.OTHER
    }

fun ReviewStateColumn.toDomain(): ReviewState =
    when (this) {
        ReviewStateColumn.NEW -> ReviewState.NEW
        ReviewStateColumn.LEARNING -> ReviewState.LEARNING
        ReviewStateColumn.REVIEW -> ReviewState.REVIEW
        ReviewStateColumn.RELEARNING -> ReviewState.RELEARNING
    }

fun ReviewState.toColumn(): ReviewStateColumn =
    when (this) {
        ReviewState.NEW -> ReviewStateColumn.NEW
        ReviewState.LEARNING -> ReviewStateColumn.LEARNING
        ReviewState.REVIEW -> ReviewStateColumn.REVIEW
        ReviewState.RELEARNING -> ReviewStateColumn.RELEARNING
    }

fun ReviewRatingColumn.toDomain(): ReviewRating =
    when (this) {
        ReviewRatingColumn.AGAIN -> ReviewRating.AGAIN
        ReviewRatingColumn.HARD -> ReviewRating.HARD
        ReviewRatingColumn.GOOD -> ReviewRating.GOOD
        ReviewRatingColumn.EASY -> ReviewRating.EASY
    }


fun ReviewRating.toColumn(): ReviewRatingColumn =
    when (this) {
        ReviewRating.AGAIN -> ReviewRatingColumn.AGAIN
        ReviewRating.HARD -> ReviewRatingColumn.HARD
        ReviewRating.GOOD -> ReviewRatingColumn.GOOD
        ReviewRating.EASY -> ReviewRatingColumn.EASY
    }