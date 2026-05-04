package com.ganadeia.app.infrastructure.persistence.firebase

import com.ganadeia.app.domain.model.HealthRecord
import com.ganadeia.app.domain.model.SyncStatus
import com.ganadeia.app.domain.port.driven.repository.HealthCheckRepository
import com.ganadeia.app.infrastructure.monitoring.CrashReporter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Decorador que intercepta las escrituras de chequeos de salud para
 * respaldarlas en Firebase Firestore.
 * - Las lecturas siempre provienen de Room (fuente de verdad local).
 * - Las escrituras se guardan primero en Room y luego en Firestore.
 * - Soporte Offline-First: el SDK de Firestore usa su caché local
 *   y sincroniza automáticamente cuando la red vuelva.
 */
class FirestoreHealthCheckRepository(
    private val firestore: FirebaseFirestore,
    private val localHealthCheckRepository: HealthCheckRepository
) : HealthCheckRepository {

    private val healthChecksCollection = firestore.collection("health_checks")

    override suspend fun saveHealthCheck(record: HealthRecord): Boolean {
        // 1. Guardar localmente primero (fuente de verdad)
        val localSuccess = localHealthCheckRepository.saveHealthCheck(record)
        // 2. Respaldar en Firestore
        if (localSuccess) {
            try {
                val data = mapOf(
                    "id" to record.id,
                    "animalId" to record.animalId,
                    "date" to record.date,
                    "weightKg" to record.weightKg,
                    "bodyConditionScore" to record.bodyConditionScore,
                    "symptoms" to record.symptoms.map { it.name },
                    "notes" to record.notes,
                    "aiRecommendation" to record.aiRecommendation,
                    "syncStatus" to record.syncStatus.name,
                    "confirmedFollowUpDate" to record.confirmedFollowUpDate
                )
                healthChecksCollection.document(record.id).set(data).await()
            } catch (e: Exception) {
                CrashReporter.logError("FirestoreHealthCheck_Save", e)
                // Falla silenciosa (caché Firestore)
            }
        }
        return localSuccess
    }

    override suspend fun updateSyncStatus(recordId: String, status: SyncStatus): Boolean {
        val localSuccess = localHealthCheckRepository.updateSyncStatus(recordId, status)
        if (localSuccess) {
            try {
                healthChecksCollection.document(recordId)
                    .update("syncStatus", status.name).await()
            } catch (e: Exception) {
                CrashReporter.logError("FirestoreHealthCheck_SyncStatus", e)
            }
        }
        return localSuccess
    }

    // ── LECTURAS: Siempre de Room ──────────────────────────────────────────

    override suspend fun getHealthChecksByAnimal(animalId: String): List<HealthRecord> =
        localHealthCheckRepository.getHealthChecksByAnimal(animalId)

    override suspend fun getPendingSyncRecords(): List<HealthRecord> =
        localHealthCheckRepository.getPendingSyncRecords()
}
