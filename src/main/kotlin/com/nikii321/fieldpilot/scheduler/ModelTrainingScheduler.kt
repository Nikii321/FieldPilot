package com.nikii321.fieldpilot.scheduler

import com.nikii321.fieldpilot.config.TrainingProperties
import com.nikii321.fieldpilot.service.ml.RetrainingService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ModelTrainingScheduler(
    private val service: RetrainingService,
    private val properties: TrainingProperties,
) {
    @Scheduled(cron = "\${fieldpilot.ml.cron}", zone = "\${fieldpilot.ml.zone}")
    fun retrain() {
        if (properties.enabled) service.run("SCHEDULED")
    }
}
