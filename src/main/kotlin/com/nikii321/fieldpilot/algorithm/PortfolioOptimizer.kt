package com.nikii321.fieldpilot.algorithm

import com.nikii321.fieldpilot.exception.InfeasiblePlanException
import com.nikii321.fieldpilot.model.response.*

object PortfolioOptimizer {
    fun optimize(
        groups: List<List<ActionOption>>,
        budget: Double,
        crewDays: Int,
    ): Pair<List<ActionOption>, Int> {
        require(groups.isNotEmpty() && groups.size <= 12)
        if (groups.any { it.isEmpty() })
            throw InfeasiblePlanException("Для одной из скважин нет допустимых действий")
        val sorted = groups.map {
            it.sortedWith(
                compareByDescending<ActionOption> { it.expectedEffect }.thenBy { it.action.cost },
            )
        }
        val suffix = DoubleArray(groups.size + 1)
        for (i in groups.indices.reversed()) suffix[i] =
            suffix[i + 1] + sorted[i].maxOf { it.expectedEffect }
        var best: List<ActionOption>? = null
        var bestValue = Double.NEGATIVE_INFINITY
        var bestCost = Double.POSITIVE_INFINITY
        var nodes = 0
        val current = mutableListOf<ActionOption>()
        fun visit(i: Int, cost: Double, days: Int, value: Double) {
            if (++nodes > 500_000)
                throw InfeasiblePlanException(
                    "Превышен лимит точного поиска; сократите фонд или варианты",
                )
            if (cost > budget + 1e-9 || days > crewDays || value + suffix[i] < bestValue - 1e-10)
                return
            if (i == groups.size) {
                if (
                    value > bestValue + 1e-10 ||
                        (kotlin.math.abs(value - bestValue) < 1e-10 && cost < bestCost)
                ) {
                    best = current.toList()
                    bestValue = value
                    bestCost = cost
                }
                return
            }
            for (o in sorted[i]) {
                current.add(o)
                visit(
                    i + 1,
                    cost + o.action.cost,
                    days + o.action.crewDays,
                    value + o.expectedEffect,
                )
                current.removeAt(current.lastIndex)
            }
        }
        visit(0, 0.0, 0, 0.0)
        return (best
            ?: throw InfeasiblePlanException(
                "Ресурсов недостаточно даже для допустимого базового плана и обязательных действий",
            )) to nodes
    }
}
