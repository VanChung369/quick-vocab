package com.chung.lexis.domain.model

enum class ReviewState {
    NEW,
    LEARNING,
    REVIEW,
    RELEARNING,
}

enum class PronunciationType {
    IPA,
    PINYIN,
    KANA,
    ROMAJI,
    OTHER,
}

enum class MeaningKind {
    TRANSLATION,
    DEFINITION,
}

enum class ReviewRating {
    AGAIN,
    HARD,
    GOOD,
    EASY,
}
