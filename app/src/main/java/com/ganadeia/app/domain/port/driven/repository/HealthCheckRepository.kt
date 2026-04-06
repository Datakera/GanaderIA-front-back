package com.ganadeia.app.domain.port.driven.repository

import com.ganadeia.app.domain.model.HealthRecord
import com.ganadeia.app.domain.model.SyncStatus

/**
 * Puerto de salida (driven port) que define el contrato de persistencia
 * para los registros de chequeo de salud.
 *
 * La implementación concreta vive en la capa de infraestructura
 * ([RoomHealthCheckRepository]) y es inyectada en el use case.
 */
interface HealthCheckRepository {

    /**
     * Persiste un nuevo [HealthCheckRecord] en la base de datos local.
     * Debe ejecutarse aunque no haya conexión a internet.
     *
     * @return `true` si la operación fue exitosa.
     */
    suspend fun saveHealthCheck(record: HealthRecord): Boolean

    /**
     * Devuelve todos los chequeos de un animal, ordenados del más
     * reciente al más antiguo.
     */
    suspend fun getHealthChecksByAnimal(animalId: String): List<HealthRecord>

    /**
     * Devuelve todos los registros con [com.ganadeia.app.domain.model.SyncStatus.PENDING_SYNC] o
     * [com.ganadeia.app.domain.model.SyncStatus.SYNC_ERROR] que están esperando ser subidos a la nube.
     */
    suspend fun getPendingSyncRecords(): List<HealthRecord>

    /**
     * Actualiza el estado de sincronización de un registro existente.
     */
    suspend fun updateSyncStatus(recordId: String, status: SyncStatus): Boolean
}