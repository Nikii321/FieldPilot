package com.nikii321.fieldpilot.controller

import com.nikii321.fieldpilot.algorithm.KnowledgeEngine
import com.nikii321.fieldpilot.model.request.AnalysisRequest
import com.nikii321.fieldpilot.service.AnalysisService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class AnalysisController(private val service: AnalysisService) {
    @GetMapping("/analysis") fun demo() = service.analyze(AnalysisRequest())

    @PostMapping("/analysis") fun calculate(@RequestBody r: AnalysisRequest) = service.analyze(r)

    @GetMapping("/knowledge") fun rules() = KnowledgeEngine.rules
}
