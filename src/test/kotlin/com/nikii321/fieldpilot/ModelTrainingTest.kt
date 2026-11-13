package com.nikii321.fieldpilot

import com.nikii321.fieldpilot.domain.*
import com.nikii321.fieldpilot.ml.data.HistoryCsv
import com.nikii321.fieldpilot.ml.inference.ProductionPredictor
import com.nikii321.fieldpilot.ml.training.RidgeTrainer
import kotlin.test.*
import org.junit.jupiter.api.Test

class ModelTrainingTest {
    private fun rows() = HistoryCsv.parse(javaClass.getResource("/demo/history.csv")!!.readText())

    @Test
    fun `chronological split has no overlap and regression beats persistence`() {
        val rows = rows()
        val s = HistoryCsv.split(rows)
        assertEquals(480, s.train.size)
        assertEquals(120, s.validation.size)
        assertEquals(120, s.test.size)
        assertTrue(s.train.maxOf { it.period } < s.validation.minOf { it.period })
        assertTrue(s.validation.maxOf { it.period } < s.test.minOf { it.period })
        val report = RidgeTrainer().train(rows, "synthetic test")
        assertTrue(report.metrics.mae < 1.6)
        assertTrue(report.metrics.r2!! > .96)
        assertTrue(report.metrics.mae < report.metrics.baselineMae)
        val p = ProductionPredictor(report)
        val f = rows[0].features
        assertEquals(p.predict(f), p.reference() + p.contributions(f).values.sum(), 1e-8)
        val unchanged =
            RidgeTrainer()
                .train(
                    rows.map { if (it.period in s.testPeriods) it.copy(target = 900.0) else it },
                    "test changed",
                )
        assertEquals(report.lambda, unchanged.lambda)
        assertEquals(report.coefficients, unchanged.coefficients)
    }

    @Test
    fun `CSV canonicalization stable rejects duplicates and nonfinite values`() {
        val csv = HistoryCsv.write(rows())
        assertEquals(csv, HistoryCsv.write(HistoryCsv.parse(csv)))
        assertEquals(
            HistoryCsv.fingerprint(csv),
            HistoryCsv.fingerprint(HistoryCsv.write(rows().reversed())),
        )
        assertFailsWith<IllegalArgumentException> { HistoryCsv.parse(csv + csv.lines()[1] + "\n") }
        assertFailsWith<IllegalArgumentException> {
            HistoryCsv.parse(csv.replaceFirst(",1,", ",NaN,"))
        }
        assertFailsWith<IllegalArgumentException> { WellFeatures(waterCut = 1.1).validate() }
    }
}
