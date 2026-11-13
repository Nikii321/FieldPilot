package com.nikii321.fieldpilot.model.response

data class SavedDecision(
    val id: String,
    val title: String,
    val createdAt: String,
    val analysis: AnalysisResponse,
)
