package com.nikii321.fieldpilot

import com.nikii321.fieldpilot.algorithm.*
import com.nikii321.fieldpilot.config.*
import com.nikii321.fieldpilot.domain.*
import com.nikii321.fieldpilot.exception.*
import com.nikii321.fieldpilot.model.request.*
import com.nikii321.fieldpilot.repository.*
import com.nikii321.fieldpilot.service.*
import com.nikii321.fieldpilot.service.ml.*
import kotlin.test.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(
    properties =
        [
            "spring.datasource.url=jdbc:h2:mem:flow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "fieldpilot.ml.enabled=false",
            "fieldpilot.ml.source-path=",
        ],
)
@Transactional
class DecisionFlowTest {
    @Autowired lateinit var analysis: AnalysisService
    @Autowired lateinit var wells: WellService
    @Autowired lateinit var decisions: DecisionService
    @Autowired lateinit var models: ModelService
    @Autowired lateinit var repository: ModelRepository

    @Test
    fun `exact portfolio matches exhaustive search and educational example`() {
        val a = analysis.analyze(AnalysisRequest())
        val p = a.plan
        assertEquals(3.5, p.cost, 1e-9)
        assertEquals(12, p.crewDays)
        assertEquals(550.0, p.gainTonnes, 1e-7)
        assertEquals(3030.0, p.baselineProduction, 1e-6)
        assertEquals(3580.0, p.centralProduction, 1e-6)
        assertEquals(2.98225, p.expectedEffect, 1e-7)
        assertEquals(
            listOf(
                ActionKind.GTM,
                ActionKind.GTM,
                ActionKind.REPAIR,
                ActionKind.REGIME,
                ActionKind.CONTINUE,
                ActionKind.STOP,
            ),
            p.actions.map { it.action.kind },
        )
        var best = Double.NEGATIVE_INFINITY
        fun brute(i: Int, cost: Double, days: Int, effect: Double) {
            if (cost > 3.5 + 1e-9 || days > 12) return
            if (i == a.wells.size) {
                best = maxOf(best, effect)
                return
            }
            for (o in a.wells[i].options) brute(
                i + 1,
                cost + o.action.cost,
                days + o.action.crewDays,
                effect + o.expectedEffect,
            )
        }
        brute(0, 0.0, 0, 0.0)
        assertEquals(best, p.expectedEffect, 1e-9)
        assertEquals(.003838112, a.ahp.consistencyRatio, 1e-8)
        assertEquals("W03", a.wells.maxBy { it.priority ?: -1.0 }.well.id)
        val outcomes = a.uncertainty.strategies.last().outcomes
        assertEquals(.145, outcomes[0], 1e-7)
        assertEquals(3.03, outcomes[1], 1e-7)
        assertEquals(5.724, outcomes[2], 1e-7)
    }

    @Test
    fun `production gate cannot be bought and mandatory stop may make plan infeasible`() {
        val a = analysis.analyze(AnalysisRequest(budget = 100.0, crewDays = 120))
        val w = a.wells.single { it.well.id == "W06" }
        assertNull(w.forecast)
        assertNull(w.priority)
        assertEquals(listOf(ActionKind.STOP), w.options.map { it.action.kind })
        assertTrue(a.recommendations.single { it.wellId == "W06" }.mandatory)
        assertFailsWith<InfeasiblePlanException> { analysis.analyze(AnalysisRequest(budget = .19)) }
        assertFailsWith<InfeasiblePlanException> { analysis.analyze(AnalysisRequest(crewDays = 0)) }
    }

    @Test
    fun `quality flag blocks analysis and optimistic editing protects concurrent updates`() {
        val w = wells.get("W01")
        fun request(valid: Boolean) =
            WellRequest(
                w.name,
                w.owner,
                w.features,
                w.productionPermit,
                valid,
                w.repairNeeded,
                w.riskScore,
                w.urgency,
                w.version,
            )
        val next = wells.update(w.id, request(false))
        assertEquals(w.version + 1, next.version)
        assertFailsWith<ConflictException> { wells.update(w.id, request(true)) }
        assertFailsWith<InfeasiblePlanException> { analysis.analyze(AnalysisRequest()) }
    }

    @Test
    fun `action editing is atomic and snapshot survives changes`() {
        val saved = decisions.save(SaveDecisionRequest("Исходный план"))
        val w = wells.get("W01")
        val actions = wells.actions(w.id)
        wells.saveActions(
            w.id,
            ActionSetRequest(
                w.version,
                actions.map { if (it.kind == ActionKind.GTM) it.copy(cost = 99.0) else it },
            ),
        )
        assertEquals(2.98225, decisions.get(saved.id).analysis.plan.expectedEffect, 1e-7)
        val later = decisions.save(SaveDecisionRequest("После изменения"))
        assertTrue(decisions.compare(saved.id, later.id).effectDifference < 0)
        assertFailsWith<ConflictException> {
            wells.saveActions(w.id, ActionSetRequest(w.version, actions))
        }
        assertFailsWith<IllegalArgumentException> {
            wells.saveActions(w.id, ActionSetRequest(w.version + 1, actions.dropLast(1)))
        }
    }

    @Test
    fun `persisted snapshot restores inference and carries validation metrics`() {
        val active = models.snapshot()
        val persisted = repository.active()!!
        assertEquals(active.artifact, persisted.artifact)
        val restored = ModelService(repository)
        val features = wells.get("W01").features
        assertEquals(models.forecast(features), restored.forecast(features))
        assertEquals(28.0, restored.forecast(features).dailyOil, 1e-7)
        assertTrue(active.artifact.report.metrics.mae < active.artifact.report.metrics.baselineMae)
    }
}
