package com.ganadeia.app.domain.port.driven.repository

import com.ganadeia.app.domain.model.Animal

interface AnimalRepository {
    suspend fun addAnimal(ownerId: String, animal: Animal): Boolean

    suspend fun getAnimalsByOwner(ownerId: String): List<Animal>
}