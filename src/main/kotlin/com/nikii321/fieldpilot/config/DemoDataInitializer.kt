package com.nikii321.fieldpilot.config

import com.nikii321.fieldpilot.domain.*
import com.nikii321.fieldpilot.repository.*
import com.nikii321.fieldpilot.service.ml.ModelService
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DemoDataInitializer(
    private val wells: WellRepository,
    private val actions: ActionRepository,
    private val models: ModelService,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (wells.seeded()) return
        val report = models.snapshot().artifact.report
        val rates = listOf(28.0, 18.0, 12.0, 35.0, 8.0, 0.0)
        val water = listOf(.35, .65, .8, .2, .9, 1.0)
        val scores = listOf(3, 4, 5, 2, 3, 5)
        for (i in rates.indices) {
            val f = WellFeatures(30.0, water[i], 8.0, 12.0, -1.0)
            val other =
                report.intercept +
                    report.coefficients.drop(1).zip(f.vector().drop(1)).sumOf {
                        it.first * it.second
                    }
            val calibrated =
                if (i == 5) 0.0
                else ((rates[i] - other) / report.coefficients[0]).coerceAtLeast(0.0)
            val w =
                Well(
                    "W%02d".format(i + 1),
                    "Скважина %02d".format(i + 1),
                    "Демо: цех добычи",
                    f.copy(q7 = calibrated),
                    i != 5,
                    true,
                    i == 2,
                    scores[i],
                    scores[i],
                )
            wells.create(w)
            actions.save(w.id, DefaultActionFactory.demo(i))
        }
        wells.markSeeded()
    }
}
