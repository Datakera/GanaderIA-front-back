package com.ganadeia.app.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ganadeia.app.infrastructure.persistence.room.entity.AnimalEntity

@Dao
interface AnimalDao {

    /**
     * Inserta o reemplaza un animal.
     * REPLACE permite re-guardar cuando se actualiza peso, propósito o nombre.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimal(animal: AnimalEntity)

    /** Todos los animales de un dueño sin filtrar por status. */
    @Query("SELECT * FROM animals WHERE ownerId = :ownerId")
    suspend fun getAnimalsByOwner(ownerId: String): List<AnimalEntity>

    /** Solo animales activos del hato completo. */
    @Query("SELECT * FROM animals WHERE status = 'ACTIVE'")
    suspend fun getAllActiveAnimals(): List<AnimalEntity>

    /**
     * Actualiza solo la fecha de próximo seguimiento.
     * Evita sobreescribir el resto de datos clínicos del animal.
     */
    @Query("UPDATE animals SET nextFollowUpDate = :newDate WHERE id = :id")
    suspend fun updateFollowUpDate(id: String, newDate: Long)

    /**
     * Actualiza los campos mutables del animal (peso, nombre, propósito).
     * Los campos inmutables (type, breed, hardiness, birthDate) no se incluyen
     * intencionalmente para evitar corrupción de datos históricos.
     */
    @Query(
        """
        UPDATE animals SET
            name        = :name,
            weight      = :weight,
            purpose     = :purpose,
            status      = :status,
            nextFollowUpDate = :nextFollowUpDate
        WHERE id = :id
        """
    )
    suspend fun updateAnimal(
        id: String,
        name: String,
        weight: Double,
        purpose: String,
        status: String,
        nextFollowUpDate: Long?
    )

    /**
     * Cambia el status de un animal sin tocar ningún otro dato.
     * Usado por DeactivateAnimalUseCase.
     */
    @Query("UPDATE animals SET status = :status, nextFollowUpDate = NULL WHERE id = :id")
    suspend fun updateAnimalStatus(id: String, status: String)

    /**
     * Requerido por el test de CASCADE DELETE en VaccinationDatabaseTest
     * y por la posible futura funcionalidad de borrado duro en admin.
     */
    @Query("DELETE FROM animals WHERE id = :id")
    suspend fun deleteAnimal(id: String)
}