package com.nikii321.fieldpilot.repository

import com.nikii321.fieldpilot.ml.model.ModelSnapshot
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Repository
class ModelRepository(private val jdbc: JdbcTemplate, private val mapper: ObjectMapper) {
    fun versions(): List<ModelSnapshot> =
        jdbc.query("SELECT payload FROM model_version ORDER BY created_at DESC") { rs, _ ->
            mapper.readValue(rs.getString(1), ModelSnapshot::class.java)
        }

    fun active(): ModelSnapshot? =
        jdbc
            .query(
                "SELECT m.payload FROM model_version m JOIN metadata a ON a.meta_value=m.id WHERE a.name='active_model'",
            ) { rs, _ ->
                mapper.readValue(rs.getString(1), ModelSnapshot::class.java)
            }
            .firstOrNull()

    @Transactional
    fun save(s: ModelSnapshot, activate: Boolean) {
        jdbc.update(
            "INSERT INTO model_version(id,created_at,payload) VALUES (?,?,?)",
            s.artifact.id,
            s.artifact.createdAt,
            mapper.writeValueAsString(s),
        )
        if (activate) activate(s.artifact.id)
    }

    @Transactional
    fun activate(id: String) {
        require(versions().any { it.artifact.id == id })
        if (jdbc.update("UPDATE metadata SET meta_value=? WHERE name='active_model'", id) == 0)
            jdbc.update("INSERT INTO metadata(name,meta_value) VALUES ('active_model',?)", id)
    }
}
