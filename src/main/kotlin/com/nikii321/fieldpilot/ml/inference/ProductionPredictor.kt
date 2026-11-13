package com.nikii321.fieldpilot.ml.inference

import com.nikii321.fieldpilot.domain.WellFeatures
import com.nikii321.fieldpilot.ml.model.ModelReport

class ProductionPredictor(private val report: ModelReport) {
    fun predict(f: WellFeatures): Double {
        f.validate()
        return (report.intercept +
                f.vector().indices.sumOf { report.coefficients[it] * f.vector()[it] })
            .coerceAtLeast(0.0)
    }

    fun contributions(f: WellFeatures) =
        report.featureNames.indices.associate {
            report.featureNames[it] to report.coefficients[it] * (f.vector()[it] - report.means[it])
        }

    fun reference() =
        report.intercept + report.means.indices.sumOf { report.coefficients[it] * report.means[it] }
}
