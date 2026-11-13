package com.nikii321.fieldpilot.ml.model

data class TrainingRow(
    val wellId: String,
    val period: Int,
    val features: com.nikii321.fieldpilot.domain.WellFeatures,
    val target: Double,
)
