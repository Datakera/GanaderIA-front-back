package com.ganadeia.app.data.repository

import com.ganadeia.app.domain.model.AiRecommendationRecord
import com.ganadeia.app.domain.model.AiRecommendationStatus
import com.ganadeia.app.domain.port.driven.repository.AiRecommendationRepository
import com.ganadeia.app.infrastructure.persistence.room.dao.AiRecommendationDao
import com.ganadeia.app.infrastructure.persistence.room.entity.AiRecommendationEntity

/**
 * Implementación concreta de [AiRecommendationRepository] usando Room.
 *
 * Responsabilidades:
 * 1. Mapear [AiRecommendationRecord] ↔ [AiRecommendationEntity].
 * 2. Serializar/deserializar [AiRecommendationStatus] a/desde String.
 * 3. Encapsular todas las llamadas al DAO.
 */
class RoomAiRecommendationRepository(
    private val dao: AiRecommendationDao
) : AiRecommendationRepository {

    // ── Escritura ─────────────────────────────────────────────────────────────

    override suspend fun saveRecommendation(record: AiRecommendationRecord): Boolean {
        return try {
            dao.insert(record.toEntity())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun updateRecommendation(record: AiRecommendationRecord): Boolean {
        return try {
            dao.update(
                id                        = record.id,
                status                    = record.status.name,
                generalDiagnosis          = record.generalDiagnosis,
                priorityAction            = record.priorityAction,
                nutritionalRecommendation = record.nutritionalRecommendation,
                confidenceScore           = record.confidenceScore,
                respondedAt               = record.respondedAt,
                retryCount                = record.retryCount,
                lastErrorMessage          = record.lastErrorMessage
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun deleteRecommendation(id: String): Boolean {
        return try {
            dao.delete(id)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ── Lectura ───────────────────────────────────────────────────────────────

    override suspend fun getRecommendationsByAnimal(
        animalId: String
    ): List<AiRecommendationRecord> =
        dao.getByAnimal(animalId).map { it.toDomain() }

    override suspend fun getLastCompletedRecommendation(
        animalId: String
    ): AiRecommendationRecord? =
        dao.getLastCompleted(animalId)?.toDomain()

    override suspend fun getPendingRecommendations(): List<AiRecommendationRecord> =
        dao.getPending().map { it.toDomain() }

    // ── Mappers privados ──────────────────────────────────────────────────────

    private fun AiRecommendationRecord.toEntity() = AiRecommendationEntity(
        id                        = id,
        animalId                  = animalId,
        requestedAt               = requestedAt,
        status                    = status.name,
        generalDiagnosis          = generalDiagnosis,
        priorityAction            = priorityAction,
        nutritionalRecommendation = nutritionalRecommendation,
        confidenceScore           = confidenceScore,
        respondedAt               = respondedAt,
        retryCount                = retryCount,
        lastErrorMessage          = lastErrorMessage
    )

    private fun AiRecommendationEntity.toDomain() = AiRecommendationRecord(
        id                        = id,
        animalId                  = animalId,
        requestedAt               = requestedAt,
        status                    = AiRecommendationStatus.valueOf(status),
        generalDiagnosis          = generalDiagnosis,
        priorityAction            = priorityAction,
        nutritionalRecommendation = nutritionalRecommendation,
        confidenceScore           = confidenceScore,
        respondedAt               = respondedAt,
        retryCount                = retryCount,
        lastErrorMessage          = lastErrorMessage
    )
}