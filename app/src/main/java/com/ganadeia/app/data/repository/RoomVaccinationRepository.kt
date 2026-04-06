package com.ganadeia.app.data.repository

import com.ganadeia.app.domain.model.VaccinationRecord
import com.ganadeia.app.domain.model.VaccineStatus
import com.ganadeia.app.domain.port.driven.repository.VaccinationRepository
import com.ganadeia.app.infrastructure.persistence.room.dao.VaccinationDao
import com.ganadeia.app.infrastructure.persistence.room.entity.VaccinationEntity

/**
 * Implementación concreta de [VaccinationRepository] usando Room.
 *
 * Responsabilidades:
 * 1. Mapear [VaccinationRecord] ↔ [VaccinationEntity].
 * 2. Serializar/deserializar [VaccineStatus] a/desde String.
 * 3. Encapsular todas las llamadas al DAO.
 */
class RoomVaccinationRepository(
    private val dao: VaccinationDao
) : VaccinationRepository {

    override suspend fun saveAll(records: List<VaccinationRecord>): Boolean {
        return try {
            dao.insertAll(records.map { it.toEntity() })
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun getVaccinationsByAnimal(animalId: String): List<VaccinationRecord> =
        dao.getVaccinationsByAnimal(animalId).map { it.toDomain() }

    // ── Mappers privados ──────────────────────────────────────────────────────

    private fun VaccinationRecord.toEntity() = VaccinationEntity(
        id = id,
        animalId = animalId,
        vaccineName = vaccineName,
        scheduledDate = scheduledDate,
        appliedDate = appliedDate,
        status = status.name,
        doseMl = doseMl
    )

    private fun VaccinationEntity.toDomain() = VaccinationRecord(
        id = id,
        animalId = animalId,
        vaccineName = vaccineName,
        scheduledDate = scheduledDate,
        appliedDate = appliedDate,
        status = VaccineStatus.valueOf(status),
        doseMl = doseMl
    )
}