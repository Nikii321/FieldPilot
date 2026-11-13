package com.nikii321.fieldpilot.model.response

data class DecisionSummary(
    val id: String,
    val title: String,
    val createdAt: String,
    val cost: Double,
    val expectedEffect: Double,
    val modelId: String,
)
