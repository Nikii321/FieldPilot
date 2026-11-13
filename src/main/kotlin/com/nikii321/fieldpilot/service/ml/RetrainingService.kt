package com.nikii321.fieldpilot.service.ml

import com.nikii321.fieldpilot.config.TrainingProperties
import com.nikii321.fieldpilot.ml.data.HistoryCsv
import com.nikii321.fieldpilot.ml.model.*
import com.nikii321.fieldpilot.repository.TrainingRepository
import java.nio.file.*
import java.time.*
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Service

@Service
class RetrainingService(
    private val model: ModelService,
    private val repository: TrainingRepository,
    private val properties: TrainingProperties,
) {
    private val busy = AtomicBoolean(false)

    @Synchronized
    fun stage(csv: String, name: String): Map<String, Any> {
        val rows = HistoryCsv.parse(csv)
        HistoryCsv.split(rows)
        val canonical = HistoryCsv.write(rows)
        val d =
            TrainingDataset(
                name.take(120),
                canonical,
                HistoryCsv.fingerprint(canonical),
                Instant.now().toString(),
            )
        repository.stage(d)
        return mapOf(
            "sourceName" to d.sourceName,
            "rows" to rows.size,
            "fingerprint" to d.fingerprint,
        )
    }

    fun status(): RetrainingStatus {
        val enabled = properties.enabled && properties.cron != "-"
        val d = repository.dataset()
        return RetrainingStatus(
            enabled,
            properties.cron,
            properties.zone,
            if (enabled)
                CronExpression.parse(properties.cron)
                    .next(ZonedDateTime.now(ZoneId.of(properties.zone)))
                    ?.toOffsetDateTime()
                    ?.toString()
            else null,
            properties.sourcePath,
            d?.sourceName,
            d?.fingerprint,
            repository.runs(),
        )
    }

    fun run(trigger: String = "MANUAL"): TrainingRun {
        var r =
            TrainingRun(
                UUID.randomUUID().toString(),
                Instant.now().toString(),
                null,
                "RUNNING",
                trigger,
                null,
                null,
                null,
                "Проверка истории",
            )
        if (!busy.compareAndSet(false, true))
            return r.copy(
                    status = "SKIPPED",
                    finishedAt = Instant.now().toString(),
                    message = "Другой запуск уже выполняется",
                )
                .also { repository.save(it) }
        try {
            repository.save(r)
            val path = properties.sourcePath.takeIf { it.isNotBlank() }?.let { Path.of(it) }
            if (path != null && Files.exists(path)) {
                require(Files.isRegularFile(path) && Files.size(path) <= 1_000_000)
                val csv = Files.readString(path)
                val canonical = HistoryCsv.write(HistoryCsv.parse(csv))
                if (repository.dataset()?.fingerprint != HistoryCsv.fingerprint(canonical))
                    stage(canonical, path.fileName.toString())
            }
            val d =
                repository.dataset()
                    ?: model.snapshot().let {
                        TrainingDataset(
                            it.artifact.sourceName,
                            it.csv,
                            it.artifact.fingerprint,
                            it.artifact.createdAt,
                        )
                    }
            r = r.copy(sourceName = d.sourceName, fingerprint = d.fingerprint)
            repository.save(r)
            val result =
                if (repository.evaluated(d.fingerprint)) "SKIPPED" to "Такие данные уже проверялись"
                else model.retrain(d)
            r =
                r.copy(
                    status = result.first,
                    finishedAt = Instant.now().toString(),
                    modelId = model.snapshot().artifact.id,
                    message = result.second,
                )
            repository.save(r)
            return r
        } catch (e: Exception) {
            r =
                r.copy(
                    status = "FAILED",
                    finishedAt = Instant.now().toString(),
                    message = (e.message ?: "Ошибка обучения").take(400),
                )
            repository.save(r)
            return r
        } finally {
            busy.set(false)
        }
    }
}
