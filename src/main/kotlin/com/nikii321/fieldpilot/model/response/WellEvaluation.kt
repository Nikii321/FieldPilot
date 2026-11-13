package com.nikii321.fieldpilot.model.response

data class WellEvaluation(
    val well: com.nikii321.fieldpilot.domain.Well,
    val forecast: Double?,
    val priority: Double?,
    val options: List<ActionOption>,
    val rules: List<RuleHit>,
)
