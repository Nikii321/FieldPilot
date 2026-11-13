package com.nikii321.fieldpilot.model.response

data class ScenarioResult(
    val strategies: List<StrategyResult>,
    val bayes: String,
    val wald: String,
    val savage: String,
    val hurwicz: String,
    val alpha: Double = .6,
)
