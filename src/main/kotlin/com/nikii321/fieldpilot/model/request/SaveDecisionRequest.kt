package com.nikii321.fieldpilot.model.request

data class SaveDecisionRequest(val title: String, val request: AnalysisRequest = AnalysisRequest())
