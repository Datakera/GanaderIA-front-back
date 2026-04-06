package com.ganadeia.app.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ganadeia.app.infrastructure.persistence.room.entity.AnimalEntity


@Dao
interface AnimalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimal(animal: AnimalEntity)

    @Query("SELECT * FROM animals WHERE ownerId = :ownerId")
    suspend fun getAnimalsByOwner(ownerId: String): List<AnimalEntity>

    @Query("SELECT * FROM animals WHERE status = 'ACTIVE'")
    suspend fun getAllActiveAnimals(): List<AnimalEntity>

    // Nuevo: Para actualizar solo la fecha de seguimiento
    @Query("UPDATE animals SET nextFollowUpDate = :newDate WHERE id = :id")
    suspend fun updateFollowUpDate(id: String, newDate: Long)

    // Requerido por el test de CASCADE DELETE en VaccinationDatabaseTest
    @Query("DELETE FROM animals WHERE id = :id")
    suspend fun deleteAnimal(id: String)
}