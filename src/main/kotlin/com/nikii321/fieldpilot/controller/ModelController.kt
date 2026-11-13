package com.nikii321.fieldpilot.controller

import com.nikii321.fieldpilot.domain.WellFeatures
import com.nikii321.fieldpilot.service.ml.*
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/ml")
class ModelController(private val models: ModelService, private val retraining: RetrainingService) {
    @GetMapping("/model") fun model() = models.snapshot().artifact

    @GetMapping("/versions") fun versions() = models.versions()

    @PostMapping("/predict")
    fun predict(@RequestBody features: WellFeatures) = models.forecast(features)

    @PostMapping("/versions/{id}/activate")
    fun activate(@PathVariable id: String) = models.activate(id)

    @GetMapping("/history.csv", produces = ["text/csv"]) fun csv() = models.snapshot().csv

    @GetMapping("/training") fun status() = retraining.status()

    @PostMapping("/dataset", consumes = [MediaType.TEXT_PLAIN_VALUE, "text/csv"])
    fun upload(
        @RequestBody csv: String,
        @RequestParam(defaultValue = "Загруженная история") name: String,
    ): Any {
        require(name.length in 1..100)
        return retraining.stage(csv, name)
    }

    @PostMapping("/train") fun train() = retraining.run()
}
