package com.nikii321.fieldpilot.algorithm

import com.nikii321.fieldpilot.model.response.AhpResult
import kotlin.math.abs
import kotlin.math.max

object AhpCalculator {
    val criteria =
        listOf("Дополнительная добыча", "Чистый эффект", "Технологический риск", "Срочность")

    fun calculate(matrix: List<List<Double>>): AhpResult {
        require(matrix.size == 4 && matrix.all { it.size == 4 }) { "Матрица AHP должна быть 4×4" }
        for (i in 0..3) for (j in 0..3) {
            val v = matrix[i][j]
            require(v.isFinite() && v in 1.0 / 9.0..9.0) { "Сравнения AHP должны быть от 1/9 до 9" }
            require(abs(v * matrix[j][i] - 1) < 1e-6) { "Матрица AHP должна быть взаимно обратной" }
            if (i == j) require(abs(v - 1) < 1e-9) { "Диагональ AHP должна равняться 1" }
        }
        var w = List(4) { 0.25 }
        repeat(200) {
            val product = List(4) { i -> (0..3).sumOf { j -> matrix[i][j] * w[j] } }
            val sum = product.sum()
            w = product.map { it / sum }
        }
        val lambda = (0..3).sumOf { i -> (0..3).sumOf { j -> matrix[i][j] * w[j] } / w[i] } / 4
        val ci = max(0.0, (lambda - 4) / 3)
        return AhpResult(criteria, matrix, w, lambda, ci, ci / 0.90, ci / 0.90 <= 0.10)
    }

    // For each criterion, alternatives form a consistent ratio matrix a_ij = value_i / value_j.
    fun rank(values: List<List<Double>>, weights: List<Double>): List<Double> {
        if (values.isEmpty()) return emptyList()
        val sums = List(4) { j -> values.sumOf { max(it[j], 1e-6) } }
        return values.map { row -> (0..3).sumOf { j -> weights[j] * max(row[j], 1e-6) / sums[j] } }
    }
}
