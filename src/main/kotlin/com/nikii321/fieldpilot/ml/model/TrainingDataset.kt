package com.nikii321.fieldpilot.ml.model

data class TrainingDataset(
    val sourceName: String,
    val csv: String,
    val fingerprint: String,
    val uploadedAt: String,
)
