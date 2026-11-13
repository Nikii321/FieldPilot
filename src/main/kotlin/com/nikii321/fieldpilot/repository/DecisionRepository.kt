package com.nikii321.fieldpilot.repository

import com.nikii321.fieldpilot.exception.NotFoundException
import com.nikii321.fieldpilot.model.response.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper

@Repository
class DecisionRepository(private val jdbc: JdbcTemplate, private val mapper: ObjectMapper) {
    fun all(): List<SavedDecision> =
        jdbc.query("SELECT payload FROM decision_snapshot ORDER BY created_at DESC") { rs, _ ->
            mapper.readValue(rs.getString(1), SavedDecision::class.java)
        }

    fun get(id: String) =
        all().find { it.id == id } ?: throw NotFoundException("Решение не найдено")

    fun save(d: SavedDecision) {
        jdbc.update(
            "INSERT INTO decision_snapshot(id,created_at,payload) VALUES (?,?,?)",
            d.id,
            d.createdAt,
            mapper.writeValueAsString(d),
        )
    }

    fun delete(id: String) {
        if (jdbc.update("DELETE FROM decision_snapshot WHERE id=?", id) == 0)
            throw NotFoundException("Решение не найдено")
    }
}
