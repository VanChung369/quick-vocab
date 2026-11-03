package com.chung.lexis.domain.model

import java.time.Duration

data class ReviewSessionLaunchPolicy(
    val dueLimit: Int,
    val newLimit: Int,

    /*
     * Hết card khác trong session → short-term repeat
     * tới hạn trong khoảng này được học luôn,
     * thay vì Waiting (learn ahead limit của Anki).
     *
     * ZERO = luôn chờ tới đúng hạn.
     */
    val learnAheadLimit: Duration = Duration.ZERO,
) {

    init {

        require(dueLimit >= 0) {
            "dueLimit must be >= 0"
        }

        require(newLimit >= 0) {
            "newLimit must be >= 0"
        }

        require(!learnAheadLimit.isNegative) {
            "learnAheadLimit must be >= 0"
        }
    }
}
