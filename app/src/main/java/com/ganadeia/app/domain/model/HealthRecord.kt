package com.ganadeia.app.domain.model

enum class Symptom {
    NONE,
    FEVER,
    DIARRHEA,
    COUGH,
    LACK_OF_APPETITE,
    LIMPING,
    WEIGHT_LOSS,
    DEHYDRATION,
    SKIN_LESIONS
}

data class HealthRecord(
    val id: String,
    val animalId: String,
    val date: Long,
    val symptoms: List<Symptom>, // <-- Cambiado: Ahora es una lista controlada
    val recordedWeight: Double,
    val bodyCondition: Int, //  Escala del 1 al 5 (1 flaco 3 mediano 5 obeso)
    val aiRecommendation: String?
)