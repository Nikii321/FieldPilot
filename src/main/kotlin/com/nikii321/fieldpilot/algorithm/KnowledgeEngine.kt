package com.nikii321.fieldpilot.algorithm

import com.nikii321.fieldpilot.domain.Well
import com.nikii321.fieldpilot.model.response.*

object KnowledgeEngine {
    val rules =
        listOf(
            KnowledgeRule(
                "R1",
                "Допуск не подтверждён",
                "Только остановка; проверка техническим специалистом",
            ),
            KnowledgeRule(
                "R2",
                "Данные не прошли контроль качества",
                "Не утверждать прогноз; проверить данные",
            ),
            KnowledgeRule(
                "R3",
                "Обводнённость ≥ 80% и снижение прогноза ≥ 10%",
                "Диагностика причин; технология ГТМ определяется экспертом",
            ),
            KnowledgeRule(
                "R4",
                "Эксперт подтвердил необходимость ремонта",
                "Рассмотреть допустимый ремонт",
            ),
            KnowledgeRule(
                "R5",
                "Необязательная мера имеет эффект ≤ 0",
                "Не финансировать по экономическому критерию",
            ),
            KnowledgeRule(
                "R6",
                "Выгодная мера не вошла в портфель",
                "Пересмотреть при изменении ресурсов",
            ),
        )

    fun evaluate(w: Well, forecast: Double?): List<RuleHit> = buildList {
        fun add(id: String, facts: String) {
            add(RuleHit(id, facts, rules.first { it.id == id }.conclusion))
        }
        if (!w.productionPermit) add("R1", "productionPermit=false")
        if (!w.dataValid) add("R2", "dataValid=false")
        if (
            forecast != null &&
                w.features.waterCut >= .8 &&
                w.features.q7 > 0 &&
                forecast <= w.features.q7 * .9
        )
            add(
                "R3",
                "Обводнённость ${w.features.waterCut}; прогноз $forecast против q7=${w.features.q7}",
            )
        if (w.repairNeeded) add("R4", "repairNeeded=true")
    }
}
