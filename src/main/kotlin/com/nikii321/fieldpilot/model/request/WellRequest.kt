package com.nikii321.fieldpilot.model.request

import com.nikii321.fieldpilot.domain.*

data class WellRequest(
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
    fun toWell(id: String) =
        Well(
            id,
            name.trim(),
            owner.trim(),
            features,
            productionPermit,
            dataValid,
            repairNeeded,
            riskScore,
            urgency,
            version,
        )
}
