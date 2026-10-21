package com.chung.lexis.domain.scheduler

class Fsrs6Config(
    parameters: List<Double> = DEFAULT_PARAMETERS,
    val desiredRetention: Double = 0.9,
    val maximumIntervalDays: Int = 36_500
) {
    val parameters: List<Double> =
        parameters.toList()


    init {
        require(this.parameters.size == 21) {
            "FSRS-6 requires exactly 21 parameters"
        }

        require(
            this.parameters.all { it.isFinite() }
        ) {
            "FSRS parameters must be finite"
        }

        require(
            desiredRetention > 0.0 &&
                    desiredRetention < 1.0
        ) {
            "desiredRetention must be between 0 and 1"
        }

        require(maximumIntervalDays >= 1) {
            "maximumIntervalDays must be >= 1"
        }

        require(this.parameters[20] > 0.0) {
            "FSRS decay parameter must be > 0"
        }

        require(parameters.size == LOWER_BOUNDS.size)

        parameters.forEachIndexed { index, value ->
            require(
                value in LOWER_BOUNDS[index]..UPPER_BOUNDS[index]
            ) {
                "parameters[$index]=$value is outside FSRS-6 bounds " +
                        "[${LOWER_BOUNDS[index]}, ${UPPER_BOUNDS[index]}]"
            }
        }

    }

    companion object {

        val LOWER_BOUNDS =
            listOf(
                0.001, 0.001, 0.001, 0.001,
                1.0, 0.001, 0.001, 0.001,
                0.0, 0.0, 0.001, 0.001,
                0.001, 0.001, 0.0, 0.0,
                1.0, 0.0, 0.0, 0.0,
                0.1,
            )

        val UPPER_BOUNDS =
            listOf(
                100.0, 100.0, 100.0, 100.0,
                10.0, 4.0, 4.0, 0.75,
                4.5, 0.8, 3.5, 5.0,
                0.25, 0.9, 4.0, 1.0,
                6.0, 2.0, 2.0, 0.8,
                0.8,
            )
        val DEFAULT_PARAMETERS =
            listOf(
                0.212,
                1.2931,
                2.3065,
                8.2956,
                6.4133,
                0.8334,
                3.0194,
                0.001,
                1.8722,
                0.1666,
                0.796,
                1.4835,
                0.0614,
                0.2629,
                1.6483,
                0.6014,
                1.8729,
                0.5425,
                0.0912,
                0.0658,
                0.1542,
            )
    }
}


