package com.ganadeia.app.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ganadeia.app.infrastructure.persistence.room.entity.HealthCheckEntity

@Dao
interface HealthCheckDao {

    /**
     * Inserta o reemplaza un chequeo. REPLACE permite que si la IA
     * actualiza la recomendación más tarde, se pueda re-guardar el
     * mismo registro con el campo aiRecommendation ya completo.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthCheck(entity: HealthCheckEntity)

    /**
     * Historial completo de un animal, del más reciente al más antiguo.
     */
    @Query("SELECT * FROM health_checks WHERE animalId = :animalId ORDER BY date DESC")
    suspend fun getHealthChecksByAnimal(animalId: String): List<HealthCheckEntity>

    /**
     * Registros que aún no se han sincronizado con la nube.
     * Incluye PENDING_SYNC y SYNC_ERROR (ambos deben reintentarse).
     */
    @Query(
        "SELECT * FROM health_checks WHERE syncStatus IN ('PENDING_SYNC', 'SYNC_ERROR') " +
                "ORDER BY date ASC"
    )
    suspend fun getPendingSyncRecords(): List<HealthCheckEntity>

    /**
     * Actualiza solo el campo syncStatus de un registro.
     * Evita sobreescribir el resto de los datos clínicos.
     */
    @Query("UPDATE health_checks SET syncStatus = :status WHERE id = :recordId")
    suspend fun updateSyncStatus(recordId: String, status: String)

    /**
     * Confirma (o edita) la fecha de seguimiento de un chequeo.
     */
    @Query(
        "UPDATE health_checks SET confirmedFollowUpDate = :confirmedDate WHERE id = :recordId"
    )
    suspend fun confirmFollowUpDate(recordId: String, confirmedDate: Long)
}