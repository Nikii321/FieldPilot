package com.nikii321.fieldpilot.model.response

data class PlanResult(
    val actions: List<ActionOption>,
    val cost: Double,
    val crewDays: Int,
    val gainTonnes: Double,
    val incrementalCost: Double,
    val riskReserve: Double,
    val expectedEffect: Double,
    val baselineProduction: Double,
    val centralProduction: Double,
    val baselineCost: Double,
    val exploredNodes: Int,
    val optimal: Boolean = true,
)
