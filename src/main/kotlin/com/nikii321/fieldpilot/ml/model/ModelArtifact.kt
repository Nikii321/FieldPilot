package com.nikii321.fieldpilot.ml.model

data class ModelArtifact(
    val id: String,
    val createdAt: String,
    val sourceName: String,
    val fingerprint: String,
    val report: ModelReport,
)
