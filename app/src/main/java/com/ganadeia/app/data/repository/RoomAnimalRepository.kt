package com.ganadeia.app.data.repository

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.AnimalPurpose
import com.ganadeia.app.domain.model.AnimalStatus
import com.ganadeia.app.domain.model.AnimalType
import com.ganadeia.app.domain.model.BreedHardiness
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository
import com.ganadeia.app.infrastructure.persistence.room.dao.AnimalDao
import com.ganadeia.app.infrastructure.persistence.room.entity.AnimalEntity

class RoomAnimalRepository(
    private val animalDao: AnimalDao
) : AnimalRepository {

    // ── Escritura ─────────────────────────────────────────────────────────────

    override suspend fun addAnimal(ownerId: String, animal: Animal): Boolean {
        return try {
            animalDao.insertAnimal(animal.toEntity(ownerId))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun updateAnimal(ownerId: String, animal: Animal): Boolean {
        return try {
            animalDao.updateAnimal(
                id = animal.id,
                name = animal.name,
                weight = animal.currentWeight,
                purpose = animal.purpose.name,
                status = animal.status.name,
                nextFollowUpDate = animal.nextFollowUpDate
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun updateAnimalStatus(animalId: String, newStatus: AnimalStatus): Boolean {
        return try {
            animalDao.updateAnimalStatus(id = animalId, status = newStatus.name)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun updateFollowUpDate(id: String, newDate: Long): Boolean {
        return try {
            animalDao.updateFollowUpDate(id, newDate)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun deleteAnimal(animalId: String): Boolean {
        return try {
            animalDao.deleteAnimal(animalId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ── Lectura ───────────────────────────────────────────────────────────────

    override suspend fun getAnimalsByOwner(ownerId: String): List<Animal> {
        return try {
            animalDao.getAnimalsByOwner(ownerId).map { it.toDomain() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getAllActiveAnimals(): List<Animal> {
        return try {
            animalDao.getAllActiveAnimals().map { it.toDomain() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ── Mappers privados ──────────────────────────────────────────────────────

    /**
     * Dominio → Entity.
     * Se llama solo en addAnimal para inserciones nuevas o reemplazos completos.
     */
    private fun Animal.toEntity(ownerId: String) = AnimalEntity(
        id = id,
        ownerId = ownerId,
        name = name,
        type = type.name,
        breed = breed,
        hardiness = hardiness.name,
        weight = currentWeight,
        birthDate = birthDate,
        purpose = purpose.name,
        status = status.name,
        nextFollowUpDate = nextFollowUpDate,
        photoPath = photoPath
    )

    /**
     * Entity → Dominio.
     * Usado en todas las lecturas desde Room.
     */
    private fun AnimalEntity.toDomain() = Animal(
        id = id,
        userId = ownerId,
        name = name,
        type = AnimalType.valueOf(type),
        breed = breed,
        hardiness = BreedHardiness.valueOf(hardiness),
        currentWeight = weight,
        birthDate = birthDate,
        purpose = AnimalPurpose.valueOf(purpose),
        status = AnimalStatus.valueOf(status),
        nextFollowUpDate = nextFollowUpDate,
        photoPath = photoPath
    )
}