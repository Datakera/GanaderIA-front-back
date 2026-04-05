package com.ganadeia.app.application

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.AnimalPurpose
import com.ganadeia.app.domain.model.AnimalStatus
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository
import com.ganadeia.app.domain.service.DateCalculator
import java.util.UUID

class AddAnimalUseCase(
    private val animalRepository: AnimalRepository
) {
    suspend fun execute(owner: User, animalName: String, breed: String, weight: Double,
                        ageInMonths: Int, purpose: AnimalPurpose): Result<Animal> {

        if (owner.role != UserRole.RANCHER) {
            return Result.failure(Exception("Only Ranchers can add animals."))
        }

        if (ageInMonths < 0) return Result.failure(Exception("Age cannot be negative"))

        if (animalName.isBlank()) {
            return Result.failure(Exception("Animal name cannot be empty."))
        }

        if (weight <= 0) {
            return Result.failure(Exception("Animal weight must be greater than zero."))
        }

        val calculatedBirthDate = DateCalculator.monthsToBirthDate(ageInMonths)

        val newAnimal = Animal(
            id = UUID.randomUUID().toString(),
            name = animalName.trim(),
            breed = breed.trim(),
            currentWeight = weight,
            birthDate = calculatedBirthDate,
            purpose = purpose,
            status = AnimalStatus.ACTIVE,
            nextFollowUpDate = null
        )

        val success = animalRepository.addAnimal(owner.id, newAnimal)

        return if (success) {
            Result.success(newAnimal)
        } else {
            Result.failure(Exception("Failed to save the animal to the local database."))
        }
    }
}