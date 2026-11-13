package com.nikii321.fieldpilot.model.response

data class PredictionResponse(
    val modelId: String,
    val dailyOil: Double,
    val oil30Days: Double,
    val referencePrediction: Double,
    val contributions: Map<String, Double>,
    val note: String,
)
