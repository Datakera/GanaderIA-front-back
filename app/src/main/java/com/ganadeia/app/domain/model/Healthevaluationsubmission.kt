package com.ganadeia.app.domain.model

data class HealthEvaluationSubmission(
    val animalId: String,
    val weightKg: Double,
    val ageRangeMonths: AgeRange,
    val symptoms: Set<VisibleSymptom>,
    val submittedAt: Long
)

enum class AgeRange(val label: String) {
    CRIA("0 – 6 meses (Cría)"),
    DESTETE("7 – 12 meses (Destete)"),
    LEVANTE("13 – 24 meses (Levante)"),
    NOVILLO("25 – 36 meses (Novillo)"),
    ADULTO_JOVEN("37 – 60 meses (Adulto joven)"),
    ADULTO("Más de 60 meses (Adulto)")
}