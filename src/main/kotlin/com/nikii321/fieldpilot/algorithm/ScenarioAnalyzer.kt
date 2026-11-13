package com.nikii321.fieldpilot.algorithm

import com.nikii321.fieldpilot.domain.Scenario
import com.nikii321.fieldpilot.model.response.*

object ScenarioAnalyzer {
    fun outcomes(actions: List<ActionOption>, scenarios: List<Scenario>) = scenarios.map { s ->
        actions.sumOf {
            it.gainTonnes * s.gainFactor * s.margin -
                it.incrementalCost -
                it.action.riskReserve * s.riskFactor
        }
    }

    fun compare(
        portfolios: List<Pair<String, List<ActionOption>>>,
        scenarios: List<Scenario>,
    ): ScenarioResult {
        val values = portfolios.map { outcomes(it.second, scenarios) }
        val best = List(3) { j -> values.maxOf { it[j] } }
        val rows = portfolios.mapIndexed { i, p ->
            val v = values[i]
            StrategyResult(
                p.first,
                v,
                v.indices.sumOf { v[it] * scenarios[it].probability },
                v.min(),
                v.indices.maxOf { best[it] - v[it] },
                .6 * v.max() + .4 * v.min(),
            )
        }
        return ScenarioResult(
            rows,
            rows.maxBy { it.expectedEffect }.name,
            rows.maxBy { it.worstEffect }.name,
            rows.minBy { it.maxRegret }.name,
            rows.maxBy { it.hurwicz }.name,
        )
    }
}
