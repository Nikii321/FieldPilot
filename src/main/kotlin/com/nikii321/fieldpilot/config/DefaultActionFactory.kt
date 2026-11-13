package com.nikii321.fieldpilot.config

import com.nikii321.fieldpilot.domain.*

object DefaultActionFactory {
    fun create(): List<WellAction> =
        listOf(
            WellAction(ActionKind.CONTINUE, "Продолжить эксплуатацию", 0.0, 0, 0.0, 0.0),
            WellAction(ActionKind.REGIME, "Изменить режим", .2, 1, 30.0, .02),
            WellAction(ActionKind.GTM, "Геолого-техническое мероприятие", .8, 3, 100.0, .08),
            WellAction(ActionKind.REPAIR, "Ремонт", 1.1, 4, 120.0, .06),
            WellAction(ActionKind.STOP, "Остановить", .05, 1, 0.0, 0.0),
        )

    fun demo(i: Int): List<WellAction> {
        if (i == 5)
            return create().map { if (it.kind == ActionKind.STOP) it.copy(cost = .2) else it }
        val sets =
            listOf(
                listOf(.25, 1.0, 45.0, .03, 1.1, 3.0, 170.0, .10, 1.6, 5.0, 150.0, .08),
                listOf(.2, 1.0, 30.0, .02, .8, 3.0, 140.0, .08, 1.3, 4.0, 100.0, .05),
                listOf(.15, 1.0, 18.0, .01, .7, 3.0, 80.0, .08, 1.1, 4.0, 170.0, .06),
                listOf(.3, 1.0, 70.0, .03, 1.4, 4.0, 160.0, .12, 1.8, 5.0, 120.0, .08),
                listOf(.1, 1.0, 10.0, .01, .5, 2.0, 45.0, .06, .9, 4.0, 110.0, .05),
            )
        return create().mapIndexed { n, a ->
            if (n in 1..3) {
                val k = (n - 1) * 4
                val s = sets[i]
                a.copy(
                    cost = s[k],
                    crewDays = s[k + 1].toInt(),
                    gainTonnes = s[k + 2],
                    riskReserve = s[k + 3],
                )
            } else a
        }
    }
}
