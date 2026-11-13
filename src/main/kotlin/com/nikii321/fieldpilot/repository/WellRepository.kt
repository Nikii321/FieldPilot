package com.nikii321.fieldpilot.repository

import com.nikii321.fieldpilot.domain.Well
import com.nikii321.fieldpilot.exception.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Repository
class WellRepository(private val jdbc: JdbcTemplate, private val mapper: ObjectMapper) {
    fun all(): List<Well> =
        jdbc.query("SELECT payload FROM well ORDER BY name") { rs, _ ->
            mapper.readValue(rs.getString(1), Well::class.java)
        }

    fun get(id: String) =
        all().find { it.id == id } ?: throw NotFoundException("Скважина не найдена")

    @Transactional
    fun create(w: Well): Well {
        jdbc.queryForList("SELECT id FROM write_lock WHERE id=1 FOR UPDATE")
        require((jdbc.queryForObject("SELECT COUNT(*) FROM well", Int::class.java) ?: 0) < 12) {
            "MVP поддерживает не более 12 скважин"
        }
        jdbc.update(
            "INSERT INTO well(id,name,version,payload) VALUES (?,?,?,?)",
            w.id,
            w.name,
            w.version,
            mapper.writeValueAsString(w),
        )
        return w
    }

    fun update(w: Well): Well {
        val next = w.copy(version = w.version + 1)
        if (
            jdbc.update(
                "UPDATE well SET name=?,version=?,payload=? WHERE id=? AND version=?",
                next.name,
                next.version,
                mapper.writeValueAsString(next),
                w.id,
                w.version,
            ) == 0
        ) {
            get(w.id)
            throw ConflictException("Скважина изменена другим запросом; обновите данные")
        }
        return next
    }

    fun delete(id: String) {
        if (jdbc.update("DELETE FROM well WHERE id=?", id) == 0)
            throw NotFoundException("Скважина не найдена")
    }

    fun seeded() =
        (jdbc.queryForObject("SELECT COUNT(*) FROM metadata WHERE name='seed'", Int::class.java)
            ?: 0) > 0

    fun markSeeded() {
        jdbc.update("INSERT INTO metadata(name,meta_value) VALUES ('seed','v1')")
    }
}
