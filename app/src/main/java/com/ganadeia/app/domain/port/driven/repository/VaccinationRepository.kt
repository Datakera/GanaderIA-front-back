package com.ganadeia.app.domain.port.driven.repository

import com.ganadeia.app.domain.model.VaccinationRecord

/**
 * Puerto de salida (driven port) que define el contrato de persistencia
 * para los registros de vacunación.
 *
 * La implementación concreta ([RoomVaccinationRepository]) vive en la capa
 * de infraestructura y es inyectada en el caso de uso.
 */
interface VaccinationRepository {

    /**
     * Persiste un lote de registros de vacunación en la base de datos local.
     * La operación es atómica: o se guardan todos, o se revierte el lote.
     *
     * @param records Lista de registros validados y listos para persistir.
     * @return `true` si todos los registros fueron guardados correctamente.
     */
    suspend fun saveAll(records: List<VaccinationRecord>): Boolean

    /**
     * Devuelve el historial completo de vacunación de un animal.
     *
     * @param animalId ID del animal dueño de los registros.
     */
    suspend fun getVaccinationsByAnimal(animalId: String): List<VaccinationRecord>
}
