package com.nikii321.fieldpilot.model.response

data class DecisionComparison(
    val left: SavedDecision,
    val right: SavedDecision,
    val costDifference: Double,
    val effectDifference: Double,
    val added: List<String>,
    val removed: List<String>,
)
