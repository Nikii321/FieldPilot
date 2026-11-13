package com.nikii321.fieldpilot.model.request

import com.nikii321.fieldpilot.config.Defaults
import com.nikii321.fieldpilot.domain.Scenario

data class AnalysisRequest(
    val budget: Double = 3.5,
    val crewDays: Int = 12,
    val matrix: List<List<Double>> = Defaults.matrix,
    val scenarios: List<Scenario> = Defaults.scenarios,
) {
    fun validate() {
        require(budget.isFinite() && budget in 0.0..100.0 && crewDays in 0..120) {
            "Бюджет 0–100 млн руб.; ресурс 0–120 бригадо-дней"
        }
        require(scenarios.size == 3)
        scenarios.forEach { it.validate() }
        require(kotlin.math.abs(scenarios.sumOf { it.probability } - 1.0) < 1e-8) {
            "Сумма вероятностей сценариев должна равняться 1"
        }
    }
}
