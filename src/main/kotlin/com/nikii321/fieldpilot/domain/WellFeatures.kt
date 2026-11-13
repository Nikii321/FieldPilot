package com.nikii321.fieldpilot.domain

data class WellFeatures(
    val q7: Double = 30.0,
    val waterCut: Double = .35,
    val pressure: Double = 8.0,
    val ageMonths: Double = 12.0,
    val trend: Double = -1.0,
) {
    fun vector() = doubleArrayOf(q7, waterCut, pressure, ageMonths, trend)

    fun validate() {
        require(vector().all { it.isFinite() }) { "Признаки должны быть конечными числами" }
        require(
            q7 in 0.0..1000.0 &&
                waterCut in 0.0..1.0 &&
                pressure in 0.0..100.0 &&
                ageMonths in 0.0..600.0 &&
                trend in -1000.0..1000.0,
        ) {
            "Признаки вне допустимых границ MVP"
        }
    }
}
