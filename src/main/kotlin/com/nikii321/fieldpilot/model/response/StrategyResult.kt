package com.nikii321.fieldpilot.model.response

data class StrategyResult(
    val name: String,
    val outcomes: List<Double>,
    val expectedEffect: Double,
    val worstEffect: Double,
    val maxRegret: Double,
    val hurwicz: Double,
)
