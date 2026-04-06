package com.ganadeia.app.data.repository

import com.ganadeia.app.domain.model.HealthRecord
import com.ganadeia.app.domain.model.SyncStatus
import com.ganadeia.app.domain.model.VisibleSymptom
import com.ganadeia.app.domain.port.driven.repository.HealthCheckRepository
import com.ganadeia.app.infrastructure.persistence.room.dao.HealthCheckDao
import com.ganadeia.app.infrastructure.persistence.room.entity.HealthCheckEntity

/**
 * Implementación concreta de [HealthCheckRepository] usando Room.
 *
 * Responsabilidades:
 * 1. Mapear [HealthCheckRecord] ↔ [HealthCheckEntity].
 * 2. Serializar/deserializar el Set<VisibleSymptom> a/desde CSV.
 * 3. Convertir el enum SyncStatus a/desde String.
 */
class RoomHealthCheckRepository(
    private val dao: HealthCheckDao
) : HealthCheckRepository {

    // ── Escritura ─────────────────────────────────────────────────────────────

    override suspend fun saveHealthCheck(record: HealthRecord): Boolean {
        return try {
            dao.insertHealthCheck(record.toEntity())
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateSyncStatus(recordId: String, status: SyncStatus): Boolean {
        return try {
            dao.updateSyncStatus(recordId, status.name)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ── Lectura ───────────────────────────────────────────────────────────────

    override suspend fun getHealthChecksByAnimal(animalId: String): List<HealthRecord> =
        dao.getHealthChecksByAnimal(animalId).map { it.toDomain() }

    override suspend fun getPendingSyncRecords(): List<HealthRecord> =
        dao.getPendingSyncRecords().map { it.toDomain() }

    // ── Mappers privados ──────────────────────────────────────────────────────

    private fun HealthRecord.toEntity() = HealthCheckEntity(
        id = id,
        animalId = animalId,
        date = date,
        weightKg = weightKg,
        bodyConditionScore = bodyConditionScore,
        symptoms = symptoms.joinToString(",") { it.name },  // Set → CSV
        notes = notes,
        aiRecommendation = aiRecommendation,
        syncStatus = syncStatus.name,
        confirmedFollowUpDate = confirmedFollowUpDate
    )

    private fun HealthCheckEntity.toDomain() = HealthRecord(
        id = id,
        animalId = animalId,
        date = date,
        weightKg = weightKg,
        bodyConditionScore = bodyConditionScore,
        symptoms = parseSymptoms(symptoms),               // CSV → Set
        notes = notes,
        aiRecommendation = aiRecommendation,
        syncStatus = SyncStatus.valueOf(syncStatus),
        confirmedFollowUpDate = confirmedFollowUpDate
    )

    /**
     * Convierte "COJERA,FIEBRE,DIARREA" → Set<VisibleSymptom>.
     * Ignora silenciosamente valores inválidos para que migraciones
     * futuras del enum no rompan registros históricos.
     */
    private fun parseSymptoms(csv: String): Set<VisibleSymptom> {
        if (csv.isBlank()) return emptySet()
        return csv.split(",")
            .mapNotNull { token ->
                try { VisibleSymptom.valueOf(token.trim()) }
                catch (_: IllegalArgumentException) { null }
            }
            .toSet()
    }
}