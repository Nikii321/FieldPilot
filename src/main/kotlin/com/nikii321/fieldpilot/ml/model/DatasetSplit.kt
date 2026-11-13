package com.nikii321.fieldpilot.ml.model

data class DatasetSplit(
    val train: List<TrainingRow>,
    val validation: List<TrainingRow>,
    val test: List<TrainingRow>,
    val trainPeriods: List<Int>,
    val validationPeriods: List<Int>,
    val testPeriods: List<Int>,
)
