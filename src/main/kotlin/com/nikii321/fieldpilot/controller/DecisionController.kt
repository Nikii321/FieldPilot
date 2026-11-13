package com.nikii321.fieldpilot.controller

import com.nikii321.fieldpilot.model.request.SaveDecisionRequest
import com.nikii321.fieldpilot.service.DecisionService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/decisions")
class DecisionController(private val service: DecisionService) {
    @GetMapping fun all() = service.all()

    @GetMapping("/compare")
    fun compare(@RequestParam left: String, @RequestParam right: String) =
        service.compare(left, right)

    @GetMapping("/{id}") fun get(@PathVariable id: String) = service.get(id)

    @PostMapping fun save(@RequestBody r: SaveDecisionRequest) = service.save(r)

    @DeleteMapping("/{id}") fun delete(@PathVariable id: String) = service.delete(id)
}
