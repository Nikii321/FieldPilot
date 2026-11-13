package com.nikii321.fieldpilot

import com.nikii321.fieldpilot.ml.data.HistoryCsv
import com.nikii321.fieldpilot.repository.ModelRepository
import com.nikii321.fieldpilot.service.ml.*
import kotlin.test.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor

@SpringBootTest(
    properties =
        [
            "spring.datasource.url=jdbc:h2:mem:training;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "fieldpilot.ml.enabled=false",
            "fieldpilot.ml.source-path=",
        ],
)
class RetrainingTest {
    @Autowired lateinit var model: ModelService
    @Autowired lateinit var retraining: RetrainingService
    @Autowired lateinit var repository: ModelRepository
    @Autowired lateinit var scheduled: ScheduledAnnotationBeanPostProcessor

    @Test
    fun `scheduled task registered new history evaluated once and failed candidate keeps active`() {
        assertTrue(
            scheduled.scheduledTasks.any {
                it.toString().contains("ModelTrainingScheduler.retrain")
            },
        )
        val before = model.snapshot()
        assertEquals("SKIPPED", retraining.run().status)
        val rows =
            HistoryCsv.parse(before.csv).map {
                it.copy(features = it.features.copy(q7 = it.features.q7 * 1.03))
            }
        retraining.stage(HistoryCsv.write(rows), "updated.csv")
        assertEquals(before.artifact.id, model.snapshot().artifact.id)
        assertEquals("ACTIVATED", retraining.run("SCHEDULED").status)
        val good = model.snapshot()
        assertNotEquals(before.artifact.id, good.artifact.id)
        assertEquals("SKIPPED", retraining.run().status)
        assertEquals(good.artifact.id, repository.active()!!.artifact.id)
        val validation = HistoryCsv.split(rows).validationPeriods
        retraining.stage(
            HistoryCsv.write(
                rows.map { if (it.period in validation) it.copy(target = it.features.q7) else it },
            ),
            "bad-validation.csv",
        )
        assertEquals("REJECTED", retraining.run().status)
        assertEquals(good.artifact.id, model.snapshot().artifact.id)
        retraining.stage(
            HistoryCsv.write(rows.map { it.copy(features = it.features.copy(pressure = 8.0)) }),
            "constant.csv",
        )
        assertEquals("FAILED", retraining.run().status)
        assertEquals(good.artifact.id, model.snapshot().artifact.id)
        assertTrue(retraining.status().runs.any { it.status == "ACTIVATED" })
        assertNull(retraining.status().nextRunAt)
        model.activate(before.artifact.id)
        assertEquals(before.artifact, model.snapshot().artifact)
    }
}
