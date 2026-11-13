package com.nikii321.fieldpilot.model.response

data class AhpResult(
    val criteria: List<String>,
    val matrix: List<List<Double>>,
    val weights: List<Double>,
    val lambdaMax: Double,
    val consistencyIndex: Double,
    val consistencyRatio: Double,
    val acceptable: Boolean,
)
