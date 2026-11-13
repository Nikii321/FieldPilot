package com.nikii321.fieldpilot.service

import com.nikii321.fieldpilot.algorithm.*
import com.nikii321.fieldpilot.domain.*
import com.nikii321.fieldpilot.exception.InfeasiblePlanException
import com.nikii321.fieldpilot.ml.inference.ProductionPredictor
import com.nikii321.fieldpilot.model.request.AnalysisRequest
import com.nikii321.fieldpilot.model.response.*
import com.nikii321.fieldpilot.repository.*
import com.nikii321.fieldpilot.service.ml.ModelService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class AnalysisService(
    private val wells: WellRepository,
    private val actions: ActionRepository,
    private val models: ModelService,
) {
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun analyze(r: AnalysisRequest): AnalysisResponse {
        r.validate()
        val ahp = AhpCalculator.calculate(r.matrix)
        require(ahp.acceptable) { "Матрица AHP несогласована: CR должен быть ≤ 0,10" }
        val source = wells.all()
        require(source.isNotEmpty()) { "Добавьте скважины" }
        if (source.any { !it.dataValid })
            throw InfeasiblePlanException(
                "Есть неподтвержденные данные: исправьте карточки скважин перед расчетом",
            )
        val snapshot = models.snapshot()
        val predictor = ProductionPredictor(snapshot.artifact.report)
        val forecasts = source.associate {
            it.id to if (it.productionPermit) predictor.predict(it.features) else 0.0
        }
        val baseCosts = source.associate { w ->
            w.id to
                if (w.productionPermit) 0.0
                else
                    (actions.get(w.id).singleOrNull { it.kind == ActionKind.STOP }?.cost
                        ?: throw InfeasiblePlanException("Нет обязательной остановки"))
        }
        val groups = source.map { w ->
            actions
                .get(w.id)
                .filter { it.enabled && (w.productionPermit || it.kind == ActionKind.STOP) }
                .map { a ->
                    val gain =
                        when (a.kind) {
                            ActionKind.CONTINUE -> 0.0
                            ActionKind.STOP -> -(forecasts.getValue(w.id) * 30)
                            else -> a.gainTonnes
                        }
                    val extra = a.cost - baseCosts.getValue(w.id)
                    val effect =
                        r.scenarios.sumOf { s ->
                            s.probability *
                                (gain * s.gainFactor * s.margin -
                                    extra -
                                    a.riskReserve * s.riskFactor)
                        }
                    ActionOption(w.id, w.name, a, gain, extra, effect)
                }
        }
        if (groups.any { it.isEmpty() })
            throw InfeasiblePlanException("Нет допустимых действий для одной из скважин")
        val baseline = groups.map { g ->
            g.singleOrNull {
                it.action.kind ==
                    if (source.first { w -> w.id == it.wellId }.productionPermit)
                        ActionKind.CONTINUE
                    else ActionKind.STOP
            } ?: throw InfeasiblePlanException("Базовое действие должно быть включено")
        }
        val eligible = source.filter { it.productionPermit }
        val values = eligible.map { w ->
            val best = groups[source.indexOf(w)].maxBy { it.expectedEffect }
            listOf(
                best.gainTonnes.coerceAtLeast(0.0),
                best.expectedEffect.coerceAtLeast(0.0),
                w.riskScore.toDouble(),
                w.urgency.toDouble(),
            )
        }
        val priorities =
            if (values.isEmpty()) emptyList() else AhpCalculator.rank(values, ahp.weights)
        val priorityById = eligible.mapIndexed { i, w -> w.id to priorities[i] }.toMap()
        val matrices =
            (0..3).map { c ->
                values.map { a ->
                    values.map { b -> a[c].coerceAtLeast(1e-6) / b[c].coerceAtLeast(1e-6) }
                }
            }
        val (chosen, nodes) = PortfolioOptimizer.optimize(groups, r.budget, r.crewDays)
        val baseProduction = forecasts.values.sum() * 30
        val plan =
            PlanResult(
                chosen,
                chosen.sumOf { it.action.cost },
                chosen.sumOf { it.action.crewDays },
                chosen.sumOf { it.gainTonnes },
                chosen.sumOf { it.incrementalCost },
                chosen.sumOf { it.action.riskReserve },
                chosen.sumOf { it.expectedEffect },
                baseProduction,
                baseProduction + chosen.sumOf { it.gainTonnes } * r.scenarios[1].gainFactor,
                baseCosts.values.sum(),
                nodes,
            )
        val portfolios = mutableListOf("Базовая эксплуатация" to baseline)
        val regime = groups.map { g ->
            g.find { it.action.kind == ActionKind.REGIME }
                ?: g.single { it.action.kind == ActionKind.STOP }
        }
        if (
            regime.sumOf { it.action.cost } <= r.budget + 1e-9 &&
                regime.sumOf { it.action.crewDays } <= r.crewDays
        )
            portfolios.add("Изменение режимов" to regime)
        portfolios.add("Оптимальный портфель" to chosen)
        val evaluations = source.mapIndexed { i, w ->
            WellEvaluation(
                w,
                if (w.productionPermit) forecasts[w.id] else null,
                priorityById[w.id],
                groups[i],
                KnowledgeEngine.evaluate(w, forecasts[w.id]),
            )
        }
        val recommendations = chosen.map { a ->
            val e = evaluations.first { it.well.id == a.wellId }
            val mandatory = !e.well.productionPermit
            val rules = e.rules.toMutableList()
            if (
                e.options.any {
                    it.action.kind !in listOf(ActionKind.CONTINUE, ActionKind.STOP) &&
                        it.expectedEffect <= 0
                }
            )
                rules.add(
                    RuleHit(
                        "R5",
                        "Ожидаемый эффект мероприятия ≤ 0",
                        "Не выбирать по экономическому критерию",
                    ),
                )
            if (e.options.any { it.expectedEffect > a.expectedEffect + 1e-9 })
                rules.add(
                    RuleHit(
                        "R6",
                        "Есть более выгодное отдельное мероприятие, не вошедшее в портфель",
                        "Отложить с учетом общего бюджета и трудозатрат",
                    ),
                )
            Recommendation(
                a.wellId,
                a.wellName,
                a.action.title,
                if (mandatory)
                    "Нет разрешения на эксплуатацию: обязательная остановка включена в бюджет до экономического выбора"
                else
                    "Выбрано в совместном портфеле с максимальным ожидаемым эффектом. Прирост %.1f т, затраты %.2f млн ₽, трудозатраты %d бригадо-дней"
                        .format(a.gainTonnes, a.action.cost, a.action.crewDays),
                mandatory,
                a.expectedEffect,
                rules,
            )
        }
        return AnalysisResponse(
            "FieldPilot",
            "Дебит: т/сут; объем: т за 30 дней; деньги: млн ₽; ресурс: бригадо-дни",
            30,
            r,
            ahp,
            evaluations,
            plan,
            ScenarioAnalyzer.compare(portfolios, r.scenarios),
            recommendations,
            snapshot.artifact,
            KnowledgeEngine.rules,
            matrices,
            listOf(
                "Учебный MVP. Исходная история синтетическая; реальные решения требуют проверки специалистом.",
                "ML прогнозирует базовый дебит. Эффект вмешательств, простои и риск-резерв задаются экспертно.",
                "AHP задает приоритет внимания. Оптимизатор максимизирует ожидаемый дополнительный эффект с учетом ограничений.",
                "Газ, взаимодействие скважин, система сбора, закачка и календарь бригад не моделируются.",
                "Отрицательный технологический допуск нельзя компенсировать экономической выгодой.",
            ),
        )
    }
}
