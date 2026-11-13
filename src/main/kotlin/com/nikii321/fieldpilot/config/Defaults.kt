package com.nikii321.fieldpilot.config

import com.nikii321.fieldpilot.domain.Scenario

object Defaults {
    val matrix =
        listOf(
            listOf(1.0, .5, 2.0, 1.0),
            listOf(2.0, 1.0, 3.0, 2.0),
            listOf(.5, 1.0 / 3, 1.0, .5),
            listOf(1.0, .5, 2.0, 1.0),
        )
    val scenarios =
        listOf(
            Scenario("Неблагоприятный", .25, .7, .010, 1.5),
            Scenario("Базовый", .5, 1.0, .012, 1.0),
            Scenario("Благоприятный", .25, 1.2, .014, .8),
        )
}
