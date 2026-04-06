package com.ganadeia.app.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ganadeia.app.infrastructure.persistence.room.entity.VaccinationEntity

@Dao
interface VaccinationDao {

    /**
     * Inserta un lote de vacunaciones de forma atómica.
     * REPLACE permite re-guardar si la vacuna fue editada (fecha o dosis).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<VaccinationEntity>)

    /**
     * Historial completo de vacunación de un animal, del más reciente al más antiguo.
     */
    @Query("SELECT * FROM vaccinations WHERE animalId = :animalId ORDER BY scheduledDate ASC")
    suspend fun getVaccinationsByAnimal(animalId: String): List<VaccinationEntity>

    /**
     * Vacunas pendientes o vencidas que el ganadero aún no ha aplicado.
     * Útil para notificaciones y alertas en el Dashboard.
     */
    @Query("SELECT * FROM vaccinations WHERE status IN ('PENDING', 'OVERDUE') ORDER BY scheduledDate ASC")
    suspend fun getPendingVaccinations(): List<VaccinationEntity>

    /**
     * Marca una vacuna como aplicada con fecha y dosis registradas.
     */
    @Query(
        "UPDATE vaccinations SET status = 'APPLIED', appliedDate = :appliedDate, " +
                "doseMl = :doseMl WHERE id = :id"
    )
    suspend fun markAsApplied(id: String, appliedDate: Long, doseMl: Double?)
}
