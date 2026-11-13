package com.nikii321.fieldpilot.controller

import com.nikii321.fieldpilot.model.request.*
import com.nikii321.fieldpilot.service.WellService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/wells")
class WellController(private val service: WellService) {
    @GetMapping fun all() = service.all()

    @GetMapping("/{id}") fun get(@PathVariable id: String) = service.get(id)

    @PostMapping fun create(@RequestBody r: WellRequest) = service.create(r)

    @PutMapping("/{id}")
    fun update(@PathVariable id: String, @RequestBody r: WellRequest) = service.update(id, r)

    @DeleteMapping("/{id}") fun delete(@PathVariable id: String) = service.delete(id)

    @GetMapping("/{id}/actions") fun actions(@PathVariable id: String) = service.actions(id)

    @PutMapping("/{id}/actions")
    fun actions(@PathVariable id: String, @RequestBody r: ActionSetRequest) =
        service.saveActions(id, r)
}
