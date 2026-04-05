package com.ganadeia.app.domain.model

data class AiRecommendation(
    val id: String,
    val healthRecordId: String,
    val serviceId: String,
    val date: Long,
    val description: String, // La recomendación legible
    val rawResponse: String? // La respuesta técnica completa de la IA
)