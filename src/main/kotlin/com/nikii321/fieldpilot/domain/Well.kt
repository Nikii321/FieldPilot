package com.nikii321.fieldpilot.domain

data class Well(
    val id: String,
    val name: String,
    val owner: String,
    val features: WellFeatures,
    val productionPermit: Boolean = true,
    val dataValid: Boolean = true,
    val repairNeeded: Boolean = false,
    val riskScore: Int = 3,
    val urgency: Int = 3,
    val version: Int = 0,
) {
    fun validate() {
        features.validate()
        require(
            name.isNotBlank() && name.length <= 80 && owner.isNotBlank() && owner.length <= 100,
        ) {
            "Укажите название и владельца"
        }
        require(riskScore in 1..5 && urgency in 1..5) { "Риск и срочность должны быть от 1 до 5" }
    }
}
