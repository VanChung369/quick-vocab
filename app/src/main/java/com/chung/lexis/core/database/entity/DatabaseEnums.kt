package com.chung.lexis.core.database.entity


enum class EnrichmentStatusColumn {
    PENDING,
    ENRICHING,
    READY,
    FAILED,
}

enum class MeaningKindColumn {
    TRANSLATION,
    DEFINITION,
}

enum class PronunciationTypeColumn {
    IPA,
    PINYIN,
    KANA,
    ROMAJI,
    OTHER,
}

enum class ReviewStateColumn {
    NEW,
    LEARNING,
    REVIEW,
    RELEARNING,
}

enum class ReviewRatingColumn {
    AGAIN,
    HARD,
    GOOD,
    EASY,
}