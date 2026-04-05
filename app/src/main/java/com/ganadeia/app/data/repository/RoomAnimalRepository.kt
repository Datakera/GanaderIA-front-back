package com.ganadeia.app.data.repository

import com.ganadeia.app.domain.model.Animal
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
                breed = animal.breed,
                weight = animal.currentWeight,
                birthDate = animal.birthDate,
                purpose = animal.purpose.name,
                status = animal.status.name,
                nextFollowUpDate = animal.nextFollowUpDate
            )
            animalDao.insertAnimal(entity)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAnimalsByOwner(ownerId: String): List<Animal> {
        return emptyList()
    }
}