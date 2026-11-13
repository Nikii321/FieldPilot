package com.nikii321.fieldpilot.ml.training

import com.nikii321.fieldpilot.ml.data.HistoryCsv
import com.nikii321.fieldpilot.ml.inference.ProductionPredictor
import com.nikii321.fieldpilot.ml.model.*
import kotlin.math.*
import smile.data.DataFrame
import smile.data.formula.Formula
import smile.regression.RidgeRegression

class RidgeTrainer {
    val names = listOf("q7", "water_cut", "pressure", "age_months", "trend")

    fun metrics(
        rows: List<TrainingRow>,
        predict: (com.nikii321.fieldpilot.domain.WellFeatures) -> Double,
    ): RegressionMetrics {
        val errors = rows.map { predict(it.features) - it.target }
        val mean = rows.map { it.target }.average()
        val variance = rows.sumOf { (it.target - mean).pow(2) }
        return RegressionMetrics(
            rows.size,
            errors.map { abs(it) }.average(),
            sqrt(errors.map { it * it }.average()),
            if (variance > 1e-12) 1 - errors.sumOf { it * it } / variance else null,
            rows.map { abs(it.features.q7 - it.target) }.average(),
        )
    }

    fun train(rows: List<TrainingRow>, provenance: String): ModelReport {
        val s = HistoryCsv.split(rows)
        require(s.train.size >= 30 && s.validation.size >= 10 && s.test.size >= 10)
        for (j in 0..4) require(s.train.map { it.features.vector()[j] }.distinct().size > 1) {
            "Признак ${names[j]} постоянен в train"
        }
        val frame =
            DataFrame.of(
                s.train.map { it.features.vector() + it.target }.toTypedArray(),
                *(names + "target").toTypedArray(),
            )
        val means = List(5) { j -> s.train.map { it.features.vector()[j] }.average() }
        val dummy = RegressionMetrics(0, 0.0, 0.0, null, 0.0)
        val candidate =
            listOf(.01, .1, 1.0, 10.0)
                .map { lambda ->
                    val fitted = RidgeRegression.fit(Formula.lhs("target"), frame, lambda)
                    val report =
                        ModelReport(
                            "Smile 4.4.2 / Ridge",
                            lambda,
                            fitted.coefficients().toList(),
                            fitted.intercept(),
                            means,
                            names,
                            s.train.size,
                            s.validation.size,
                            s.test.size,
                            s.trainPeriods,
                            s.validationPeriods,
                            s.testPeriods,
                            dummy,
                            dummy,
                            provenance,
                        )
                    report.copy(
                        validationMetrics =
                            metrics(s.validation, ProductionPredictor(report)::predict),
                    )
                }
                .minBy { it.validationMetrics.mae }
        return candidate.copy(metrics = metrics(s.test, ProductionPredictor(candidate)::predict))
    }
}
