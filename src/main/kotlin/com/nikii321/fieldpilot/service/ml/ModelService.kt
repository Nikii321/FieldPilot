package com.nikii321.fieldpilot.service.ml

import com.nikii321.fieldpilot.domain.WellFeatures
import com.nikii321.fieldpilot.exception.NotFoundException
import com.nikii321.fieldpilot.ml.data.HistoryCsv
import com.nikii321.fieldpilot.ml.inference.ProductionPredictor
import com.nikii321.fieldpilot.ml.model.*
import com.nikii321.fieldpilot.ml.training.RidgeTrainer
import com.nikii321.fieldpilot.model.response.PredictionResponse
import com.nikii321.fieldpilot.repository.ModelRepository
import java.time.Instant
import java.util.UUID
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class ModelService(private val repository: ModelRepository) {
    @Volatile
    private var active =
        repository.active()
            ?: build(
                    ClassPathResource("demo/history.csv").inputStream.bufferedReader().use {
                        it.readText()
                    },
                    "Синтетическая история 60 скважин × 12 периодов",
                    synthetic = true,
                )
                .also { repository.save(it, true) }

    fun snapshot() = active

    fun versions() = repository.versions().map { it.artifact }

    fun forecast(f: WellFeatures): PredictionResponse {
        f.validate()
        val s = active
        val predictor = ProductionPredictor(s.artifact.report)
        val q = predictor.predict(f)
        return PredictionResponse(
            s.artifact.id,
            q,
            q * 30,
            predictor.reference(),
            predictor.contributions(f),
            "Базовый режим на 30 суток; эффекты ГТМ задаются отдельно. Вклады не доказывают причинность.",
        )
    }

    private fun build(csv: String, source: String, synthetic: Boolean = false): ModelSnapshot {
        val rows = HistoryCsv.parse(csv)
        val canonical = HistoryCsv.write(rows)
        val provenance =
            if (synthetic) "Синтетические данные; учебный пример"
            else "Загруженный CSV; достоверность и переносимость требуют проверки"
        return ModelSnapshot(
            ModelArtifact(
                UUID.randomUUID().toString(),
                Instant.now().toString(),
                source,
                HistoryCsv.fingerprint(canonical),
                RidgeTrainer().train(rows, provenance),
            ),
            canonical,
        )
    }

    @Synchronized
    fun activate(id: String): ModelArtifact {
        val next =
            repository.versions().find { it.artifact.id == id }
                ?: throw NotFoundException("Модель не найдена")
        repository.activate(id)
        active = next
        return next.artifact
    }

    @Synchronized
    fun retrain(d: TrainingDataset): Pair<String, String> {
        if (d.fingerprint == active.artifact.fingerprint)
            return "SKIPPED" to "Данные и настройки не изменились"
        val candidate = build(d.csv, d.sourceName)
        val validation = HistoryCsv.split(HistoryCsv.parse(d.csv)).validation
        val previous =
            RidgeTrainer().metrics(validation, ProductionPredictor(active.artifact.report)::predict)
        val quality = candidate.artifact.report.validationMetrics
        val accepted = quality.mae <= quality.baselineMae && quality.mae <= previous.mae * 1.02
        repository.save(candidate, accepted)
        if (accepted) active = candidate
        return (if (accepted) "ACTIVATED" else "REJECTED") to
            "Кандидат ${candidate.artifact.id.take(8)}: validation MAE=%.3f; текущая=%.3f; база=%.3f т/сут"
                .format(quality.mae, previous.mae, quality.baselineMae)
    }
}
