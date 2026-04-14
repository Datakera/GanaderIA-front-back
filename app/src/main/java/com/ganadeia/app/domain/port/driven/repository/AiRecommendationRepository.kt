package com.ganadeia.app.domain.port.driven.repository

import com.ganadeia.app.domain.model.AiRecommendationRecord
import com.ganadeia.app.domain.model.AiRecommendationStatus

/**
 * Puerto de salida para la persistencia local de recomendaciones de IA.
 *
 * Separa la lógica de red (AiServicePort) de la persistencia local (Room),
 * siguiendo el mismo patrón offline-first del resto del proyecto.
 */
interface AiRecommendationRepository {

    /** Guarda una solicitud nueva con status PENDING. */
    suspend fun saveRecommendation(record: AiRecommendationRecord): Boolean

    /** Actualiza una recomendación existente (cuando llega la respuesta de la IA). */
    suspend fun updateRecommendation(record: AiRecommendationRecord): Boolean

    /** Historial de recomendaciones de un animal, del más reciente al más antiguo. */
    suspend fun getRecommendationsByAnimal(animalId: String): List<AiRecommendationRecord>

    /** La recomendación más reciente completada de un animal. Null si no tiene ninguna. */
    suspend fun getLastCompletedRecommendation(animalId: String): AiRecommendationRecord?

    /**
     * Solicitudes en estado PENDING o FAILED que deben reintentarse.
     * Las usa el caso de uso de sincronización cuando vuelve la red.
     */
    suspend fun getPendingRecommendations(): List<AiRecommendationRecord>

    /** Elimina recomendaciones antiguas (limpieza opcional, no es crítica). */
    suspend fun deleteRecommendation(id: String): Boolean
}