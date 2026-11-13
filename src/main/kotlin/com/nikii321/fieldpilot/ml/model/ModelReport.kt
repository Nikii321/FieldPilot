package com.nikii321.fieldpilot.ml.model

data class ModelReport(
    val engine: String,
    val lambda: Double,
    val coefficients: List<Double>,
    val intercept: Double,
    val means: List<Double>,
    val featureNames: List<String>,
    val trainCount: Int,
    val validationCount: Int,
    val testCount: Int,
    val trainPeriods: List<Int>,
    val validationPeriods: List<Int>,
    val testPeriods: List<Int>,
    val validationMetrics: RegressionMetrics,
    val metrics: RegressionMetrics,
    val provenance: String,
)
