package com.nikii321.fieldpilot.ml.model

data class RegressionMetrics(
    val count: Int,
    val mae: Double,
    val rmse: Double,
    val r2: Double?,
    val baselineMae: Double,
)
