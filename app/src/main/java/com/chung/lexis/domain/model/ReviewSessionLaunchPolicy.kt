package com.chung.lexis.domain.model

data class ReviewSessionLaunchPolicy(
    val dueLimit: Int,
    val newLimit: Int,
) {

    init {

        require(dueLimit >= 0) {
            "dueLimit must be >= 0"
        }

        require(newLimit >= 0) {
            "newLimit must be >= 0"
        }
    }
}
