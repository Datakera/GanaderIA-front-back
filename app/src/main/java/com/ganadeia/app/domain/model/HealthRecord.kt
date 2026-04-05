package com.ganadeia.app.domain.model

data class HealthRecord(
    val id: String,
    val animalId: String,
    val date: Long,
    val symptoms: String,
    val recordedWeight: Double,
    val bodyCondition: String,
    val aiRecommendation: String?
)