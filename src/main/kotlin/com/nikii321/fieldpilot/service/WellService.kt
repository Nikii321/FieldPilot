package com.nikii321.fieldpilot.service

import com.nikii321.fieldpilot.config.DefaultActionFactory
import com.nikii321.fieldpilot.domain.*
import com.nikii321.fieldpilot.exception.ConflictException
import com.nikii321.fieldpilot.model.request.*
import com.nikii321.fieldpilot.repository.*
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WellService(private val wells: WellRepository, private val actions: ActionRepository) {
    fun all() = wells.all()

    fun get(id: String) = wells.get(id)

    @Transactional
    fun create(r: WellRequest): Well {
        require(r.version == 0)
        val w = r.toWell(UUID.randomUUID().toString())
        w.validate()
        wells.create(w)
        actions.save(w.id, DefaultActionFactory.create())
        return w
    }

    @Transactional
    fun update(id: String, r: WellRequest): Well {
        val w = r.toWell(id)
        w.validate()
        return wells.update(w)
    }

    @Transactional fun delete(id: String) = wells.delete(id)

    fun actions(id: String): List<WellAction> {
        wells.get(id)
        return actions.get(id)
    }

    @Transactional
    fun saveActions(id: String, r: ActionSetRequest): Well {
        val w = wells.get(id)
        require(
            r.actions.size == 5 && r.actions.map { it.kind }.toSet() == ActionKind.entries.toSet(),
        ) {
            "Нужны пять разных действий"
        }
        r.actions.forEach { it.validate() }
        if (r.wellVersion != w.version)
            throw ConflictException("Скважина изменена; обновите карточку")
        val next = wells.update(w)
        actions.save(id, r.actions)
        return next
    }
}
