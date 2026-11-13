package com.nikii321.fieldpilot.repository

import com.nikii321.fieldpilot.ml.model.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper

@Repository
class TrainingRepository(private val jdbc: JdbcTemplate, private val mapper: ObjectMapper) {
    fun dataset(): TrainingDataset? =
        jdbc
            .query("SELECT payload FROM training_dataset WHERE id=1") { rs, _ ->
                mapper.readValue(rs.getString(1), TrainingDataset::class.java)
            }
            .firstOrNull()

    fun stage(d: TrainingDataset) {
        val p = mapper.writeValueAsString(d)
        if (jdbc.update("UPDATE training_dataset SET payload=? WHERE id=1", p) == 0)
            jdbc.update("INSERT INTO training_dataset(id,payload) VALUES (1,?)", p)
    }

    fun runs(): List<TrainingRun> =
        jdbc.query("SELECT payload FROM training_run ORDER BY started_at DESC LIMIT 50") { rs, _ ->
            mapper.readValue(rs.getString(1), TrainingRun::class.java)
        }

    fun evaluated(fp: String) =
        (jdbc.queryForObject(
            "SELECT COUNT(*) FROM training_run WHERE fingerprint=? AND status IN ('ACTIVATED','REJECTED')",
            Int::class.java,
            fp,
        ) ?: 0) > 0

    fun save(r: TrainingRun) {
        val p = mapper.writeValueAsString(r)
        if (
            jdbc.update(
                "UPDATE training_run SET payload=?,status=?,fingerprint=? WHERE id=?",
                p,
                r.status,
                r.fingerprint,
                r.id,
            ) == 0
        )
            jdbc.update(
                "INSERT INTO training_run(id,started_at,status,fingerprint,payload) VALUES (?,?,?,?,?)",
                r.id,
                r.startedAt,
                r.status,
                r.fingerprint,
                p,
            )
    }
}
