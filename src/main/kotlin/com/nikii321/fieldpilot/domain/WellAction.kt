package com.nikii321.fieldpilot.domain

data class WellAction(
    val kind: ActionKind,
    val title: String,
    val cost: Double,
    val crewDays: Int,
    val gainTonnes: Double,
    val riskReserve: Double,
    val enabled: Boolean = true,
) {
    fun validate() {
        require(title.isNotBlank() && title.length <= 160) { "Укажите название действия" }
        require(listOf(cost, gainTonnes, riskReserve).all { it.isFinite() }) {
            "Оценки действий должны быть конечными"
        }
        require(
            cost in 0.0..1000.0 &&
                crewDays in 0..60 &&
                gainTonnes in -10000.0..10000.0 &&
                riskReserve in 0.0..1000.0,
        ) {
            "Оценки действия вне границ MVP"
        }
        if (kind == ActionKind.CONTINUE)
            require(
                cost == 0.0 && crewDays == 0 && gainTonnes == 0.0 && riskReserve == 0.0 && enabled,
            ) {
                "Базовая эксплуатация должна быть доступна и иметь нулевые дополнительные оценки"
            }
        if (kind == ActionKind.STOP)
            require(gainTonnes == 0.0 && riskReserve == 0.0 && enabled) {
                "Прирост остановки рассчитывается системой; остановка должна быть доступна"
            }
    }
}
