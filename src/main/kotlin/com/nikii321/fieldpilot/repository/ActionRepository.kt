package com.nikii321.fieldpilot.repository

import com.nikii321.fieldpilot.domain.WellAction
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper

@Repository
class ActionRepository(private val jdbc: JdbcTemplate, private val mapper: ObjectMapper) {
    fun get(id: String): List<WellAction> =
        jdbc
            .query(
                "SELECT payload FROM well_actions WHERE well_id=?",
                { rs, _ ->
                    mapper.readValue(rs.getString(1), Array<WellAction>::class.java).toList()
                },
                id,
            )
            .firstOrNull() ?: emptyList()

    fun save(id: String, actions: List<WellAction>) {
        val json = mapper.writeValueAsString(actions)
        if (jdbc.update("UPDATE well_actions SET payload=? WHERE well_id=?", json, id) == 0)
            jdbc.update("INSERT INTO well_actions(well_id,payload) VALUES (?,?)", id, json)
    }
}
