package com.nikii321.fieldpilot.model.response

data class ActionOption(
    val wellId: String,
    val wellName: String,
    val action: com.nikii321.fieldpilot.domain.WellAction,
    val gainTonnes: Double,
    val incrementalCost: Double,
    val expectedEffect: Double,
)
