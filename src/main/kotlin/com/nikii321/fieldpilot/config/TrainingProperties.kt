package com.nikii321.fieldpilot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("fieldpilot.ml")
data class TrainingProperties(
    val enabled: Boolean = true,
    val cron: String = "0 0 3 * * *",
    val zone: String = "Europe/Moscow",
    val sourcePath: String = "./training-data/history.csv",
)
