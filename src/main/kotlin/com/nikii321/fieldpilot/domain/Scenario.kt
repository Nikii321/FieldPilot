package com.nikii321.fieldpilot.domain

data class Scenario(
    val name: String,
    val probability: Double,
    val gainFactor: Double,
    val margin: Double,
    val riskFactor: Double,
) {
    fun validate() {
        require(name.isNotBlank() && name.length <= 60)
        require(listOf(probability, gainFactor, margin, riskFactor).all { it.isFinite() })
        require(
            probability in 0.0..1.0 &&
                gainFactor in 0.0..3.0 &&
                margin in 0.0..1.0 &&
                riskFactor in 0.0..5.0,
        ) {
            "Недопустимые параметры сценария"
        }
    }
}
