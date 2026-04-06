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

    override suspend fun addAnimal(ownerId: String, animal: Animal): Boolean {
        return try {
            val entity = AnimalEntity(
                id = animal.id,
                ownerId = ownerId,
                name = animal.name,
                type = animal.type.name,      // <--- Agregado
                breed = animal.breed,
                hardiness = animal.hardiness.name, // <--- Agregado
                weight = animal.currentWeight,
                birthDate = animal.birthDate,
                purpose = animal.purpose.name,
                status = animal.status.name,
                nextFollowUpDate = animal.nextFollowUpDate
            )
            animalDao.insertAnimal(entity)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun getAnimalsByOwner(ownerId: String): List<Animal> {
        return try {
            val entities = animalDao.getAnimalsByOwner(ownerId)
            entities.map { it.toDomain() } // Convertimos la lista
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getAllActiveAnimals(): List<Animal> {
        return try {
            val entities = animalDao.getAllActiveAnimals()
            entities.map { it.toDomain() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
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

    /**
     * Función de extensión (Mapper) para limpiar el código.
     * Convierte una Entity de Room a un objeto de Dominio.
     */
    private fun AnimalEntity.toDomain(): Animal {
        return Animal(
            id = this.id,
            userId = this.ownerId,
            name = this.name,
            type = AnimalType.valueOf(this.type),
            breed = this.breed,
            hardiness = BreedHardiness.valueOf(this.hardiness),
            currentWeight = this.weight,
            birthDate = this.birthDate,
            purpose = AnimalPurpose.valueOf(this.purpose),
            status = AnimalStatus.valueOf(this.status),
            nextFollowUpDate = this.nextFollowUpDate
        )
    }
}