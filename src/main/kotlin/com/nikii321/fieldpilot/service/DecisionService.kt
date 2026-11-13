package com.nikii321.fieldpilot.service

import com.nikii321.fieldpilot.model.request.SaveDecisionRequest
import com.nikii321.fieldpilot.model.response.*
import com.nikii321.fieldpilot.repository.DecisionRepository
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class DecisionService(
    private val repository: DecisionRepository,
    private val analysis: AnalysisService,
) {
    fun all() =
        repository.all().map {
            DecisionSummary(
                it.id,
                it.title,
                it.createdAt,
                it.analysis.plan.cost,
                it.analysis.plan.expectedEffect,
                it.analysis.model.id,
            )
        }

    fun get(id: String) = repository.get(id)

    fun save(r: SaveDecisionRequest): SavedDecision {
        require(r.title.trim().length in 1..100) { "Название: 1–100 символов" }
        val d =
            SavedDecision(
                UUID.randomUUID().toString(),
                r.title.trim(),
                Instant.now().toString(),
                analysis.analyze(r.request),
            )
        repository.save(d)
        return d
    }

    fun delete(id: String) = repository.delete(id)

    fun compare(left: String, right: String): DecisionComparison {
        val a = get(left)
        val b = get(right)
        fun keys(d: SavedDecision) =
            d.analysis.plan.actions.map { it.wellId + ":" + it.action.kind }.toSet()
        return DecisionComparison(
            a,
            b,
            b.analysis.plan.cost - a.analysis.plan.cost,
            b.analysis.plan.expectedEffect - a.analysis.plan.expectedEffect,
            (keys(b) - keys(a)).toList(),
            (keys(a) - keys(b)).toList(),
        )
    }
}
