package com.nikii321.fieldpilot.model.response

data class Recommendation(
    val wellId: String,
    val wellName: String,
    val action: String,
    val explanation: String,
    val mandatory: Boolean,
    val expectedEffect: Double,
    val rules: List<RuleHit>,
)
