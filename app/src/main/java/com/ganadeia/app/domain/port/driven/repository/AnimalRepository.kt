package com.ganadeia.app.domain.port.driven.repository

import com.ganadeia.app.domain.model.Animal

interface AnimalRepository {
    suspend fun addAnimal(ownerId: String, animal: Animal): Boolean

    suspend fun getAnimalsByOwner(ownerId: String): List<Animal>

    suspend fun getAllActiveAnimals(): List<Animal>

    suspend fun updateFollowUpDate(id: String, newDate: Long): Boolean
}