package com.ganadeia.app.infrastructure.persistence.firebase

import com.ganadeia.app.domain.model.AiRecommendationRecord
import com.ganadeia.app.domain.model.AiRecommendationStatus
import com.ganadeia.app.domain.port.driven.repository.AiRecommendationRepository
import com.ganadeia.app.infrastructure.monitoring.CrashReporter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Decorador que intercepta las escrituras de recomendaciones de IA para
 * respaldarlas en Firebase Firestore.
 * - Las lecturas siempre provienen de Room (fuente de verdad local).
 * - Las escrituras se guardan primero en Room y luego en Firestore.
 * - Soporte Offline-First: el SDK de Firestore usa su caché local
 *   y sincroniza automáticamente cuando la red vuelva.
 */
class FirestoreAiRecommendationRepository(
    private val firestore: FirebaseFirestore,
    private val localAiRecommendationRepository: AiRecommendationRepository
) : AiRecommendationRepository {

    private val aiRecommendationsCollection = firestore.collection("ai_recommendations")

    override suspend fun saveRecommendation(record: AiRecommendationRecord): Boolean {
        val localSuccess = localAiRecommendationRepository.saveRecommendation(record)
        if (localSuccess) {
            try {
                aiRecommendationsCollection.document(record.id).set(recordToMap(record)).await()
            } catch (e: Exception) {
                CrashReporter.logError("FirestoreAiRec_Save", e)
            }
        }
        return localSuccess
    }

    override suspend fun updateRecommendation(record: AiRecommendationRecord): Boolean {
        val localSuccess = localAiRecommendationRepository.updateRecommendation(record)
        if (localSuccess) {
            try {
                aiRecommendationsCollection.document(record.id).set(recordToMap(record)).await()
            } catch (e: Exception) {
                CrashReporter.logError("FirestoreAiRec_Update", e)
            }
        }
        return localSuccess
    }

    override suspend fun deleteRecommendation(id: String): Boolean {
        val localSuccess = localAiRecommendationRepository.deleteRecommendation(id)
        if (localSuccess) {
            try {
                aiRecommendationsCollection.document(id).delete().await()
            } catch (e: Exception) {
                CrashReporter.logError("FirestoreAiRec_Delete", e)
            }
        }
        return localSuccess
    }

    // ── LECTURAS: Siempre de Room ──────────────────────────────────────────

    override suspend fun getRecommendationsByAnimal(animalId: String): List<AiRecommendationRecord> =
        localAiRecommendationRepository.getRecommendationsByAnimal(animalId)

    override suspend fun getLastCompletedRecommendation(animalId: String): AiRecommendationRecord? =
        localAiRecommendationRepository.getLastCompletedRecommendation(animalId)

    override suspend fun getPendingRecommendations(): List<AiRecommendationRecord> =
        localAiRecommendationRepository.getPendingRecommendations()

    // ── Utilidad para convertir el record a un Map serializable ─────────

    private fun recordToMap(record: AiRecommendationRecord): Map<String, Any?> = mapOf(
        "id" to record.id,
        "animalId" to record.animalId,
        "requestedAt" to record.requestedAt,
        "status" to record.status.name,
        "generalDiagnosis" to record.generalDiagnosis,
        "priorityAction" to record.priorityAction,
        "nutritionalRecommendation" to record.nutritionalRecommendation,
        "medicalRecommendation" to record.medicalRecommendation,
        "vaccineRecommendation" to record.vaccineRecommendation,
        "confidenceScore" to record.confidenceScore,
        "respondedAt" to record.respondedAt,
        "retryCount" to record.retryCount,
        "lastErrorMessage" to record.lastErrorMessage
    )
}
