package com.nikii321.fieldpilot.ml.model

data class RetrainingStatus(
    val enabled: Boolean,
    val cron: String,
    val zone: String,
    val nextRunAt: String?,
    val sourcePath: String,
    val queuedSource: String?,
    val queuedFingerprint: String?,
    val runs: List<TrainingRun>,
)
