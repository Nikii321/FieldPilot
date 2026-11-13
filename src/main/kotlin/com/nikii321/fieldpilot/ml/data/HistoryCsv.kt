package com.nikii321.fieldpilot.ml.data

import com.nikii321.fieldpilot.domain.WellFeatures
import com.nikii321.fieldpilot.ml.model.*
import java.security.MessageDigest

object HistoryCsv {
    const val HEADER = "well_id,period,q7,water_cut,pressure,age_months,trend,q_next30"

    fun parse(csv: String): List<TrainingRow> {
        require(csv.toByteArray().size <= 1_000_000) { "CSV не больше 1 МБ" }
        val lines = csv.trim().removePrefix("\uFEFF").lines().filter { it.isNotBlank() }
        require(lines.firstOrNull()?.trim() == HEADER) { "Неверный заголовок CSV: $HEADER" }
        val rows =
            lines.drop(1).mapIndexed { i, line ->
                try {
                    val c = line.split(',').map { it.trim() }
                    require(c.size == 8)
                    require(c[0].matches(Regex("[A-Za-z0-9_-]{1,40}")))
                    val f =
                        WellFeatures(
                            c[2].toDouble(),
                            c[3].toDouble(),
                            c[4].toDouble(),
                            c[5].toDouble(),
                            c[6].toDouble(),
                        )
                    f.validate()
                    val period = c[1].toInt()
                    val target = c[7].toDouble()
                    require(period in 1..10000 && target.isFinite() && target in 0.0..1000.0)
                    TrainingRow(c[0], period, f, target)
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException("Некорректная строка CSV ${i+2}: ${e.message}")
                }
            }
        require(rows.size in 90..5000 && rows.map { it.wellId }.toSet().size >= 10) {
            "Нужно 90–5000 записей и минимум 10 скважин"
        }
        require(rows.map { it.wellId to it.period }.toSet().size == rows.size) {
            "Повторяется пара well_id,period"
        }
        return rows
    }

    fun write(rows: List<TrainingRow>) =
        HEADER +
            "\n" +
            rows.sortedWith(compareBy<TrainingRow> { it.period }.thenBy { it.wellId }).joinToString(
                "\n",
            ) { r ->
                listOf(r.wellId, r.period, *r.features.vector().toTypedArray(), r.target)
                    .joinToString(",")
            } +
            "\n"

    fun split(rows: List<TrainingRow>): DatasetSplit {
        val periods = rows.map { it.period }.distinct().sorted()
        require(periods.size >= 6) { "Нужно минимум 6 непересекающихся периодов" }
        val a = periods.size * 2 / 3
        val b = a + (periods.size - a) / 2
        return DatasetSplit(
            rows.filter { it.period in periods.take(a) },
            rows.filter { it.period in periods.subList(a, b) },
            rows.filter { it.period in periods.drop(b) },
            periods.take(a),
            periods.subList(a, b),
            periods.drop(b),
        )
    }

    fun fingerprint(csv: String) =
        MessageDigest.getInstance("SHA-256")
            .digest(("smile-ridge-4.4.2:time-split-2/3:v1\n" + csv).toByteArray())
            .joinToString("") { "%02x".format(it) }
}
