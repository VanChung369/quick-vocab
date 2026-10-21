package com.chung.lexis.domain.model

data class ReviewDueSummary(
    val shortTermDue: Int,
    val reviewDue: Int,
    val newAvailable: Int,
) {

    init {
        require(shortTermDue >= 0) {
            "shortTermDue must be >= 0"
        }

        require(reviewDue >= 0) {
            "reviewDue must be >= 0"
        }

        require(newAvailable >= 0) {
            "newAvailable must be >= 0"
        }
    }

    val dueCount: Int
        get() =
            shortTermDue +
                    reviewDue

    val totalAvailable: Int
        get() =
            dueCount +
                    newAvailable
}