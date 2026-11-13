package com.nikii321.fieldpilot

import com.nikii321.fieldpilot.algorithm.AhpCalculator
import com.nikii321.fieldpilot.config.Defaults
import com.nikii321.fieldpilot.model.request.AnalysisRequest
import kotlin.test.*
import org.junit.jupiter.api.Test

class InputValidationTest {
    @Test
    fun `invalid budgets scenarios and inconsistent judgments are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            AnalysisRequest(budget = Double.NaN).validate()
        }
        assertFailsWith<IllegalArgumentException> { AnalysisRequest(budget = -1.0).validate() }
        assertFailsWith<IllegalArgumentException> {
            AnalysisRequest(scenarios = Defaults.scenarios.map { it.copy(probability = .1) })
                .validate()
        }
        val bad = List(4) { MutableList(4) { 1.0 } }
        bad[0][1] = 9.0
        bad[1][0] = 1.0 / 9
        bad[1][2] = 9.0
        bad[2][1] = 1.0 / 9
        bad[2][0] = 9.0
        bad[0][2] = 1.0 / 9
        assertFalse(AhpCalculator.calculate(bad).acceptable)
        assertFailsWith<IllegalArgumentException> {
            AhpCalculator.calculate(Defaults.matrix.map { it.map { 0.0 } })
        }
    }
}
