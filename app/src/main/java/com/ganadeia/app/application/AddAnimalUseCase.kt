package com.ganadeia.app.application

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.AnimalPurpose
import com.ganadeia.app.domain.model.AnimalStatus
import com.ganadeia.app.domain.model.AnimalType
import com.ganadeia.app.domain.model.BreedHardiness
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository
import com.ganadeia.app.domain.service.DateCalculator
import java.util.UUID

class AddAnimalUseCase(
    private val animalRepository: AnimalRepository
) {
    suspend fun execute(request: AddAnimalRequest): Result<Animal> {

        if (request.owner.role != UserRole.RANCHER) {
            return Result.failure(Exception("Only Ranchers can add animals."))
        }

        if (request.ageInMonths < 0) return Result.failure(Exception("Age cannot be negative"))

        if (request.name.isBlank()) {
            return Result.failure(Exception("Animal name cannot be empty."))
        }

        if (request.weight <= 0) {
            return Result.failure(Exception("Animal weight must be greater than zero."))
        }

        val calculatedBirthDate = DateCalculator.monthsToBirthDate(request.ageInMonths)

        val newAnimal = Animal(
            id = UUID.randomUUID().toString(),
            userId = request.owner.id,
            name = request.name.trim(),
            breed = request.breed.trim(),
            type = request.type,
            hardiness = request.hardiness,
            currentWeight = request.weight,
            birthDate = calculatedBirthDate,
            purpose = request.purpose,
            status = AnimalStatus.ACTIVE,
            nextFollowUpDate = request.initialFollowUpDate
        )

        val success = animalRepository.addAnimal(request.owner.id, newAnimal)

        return if (success) Result.success(newAnimal)
        else Result.failure(Exception("Failed to save the animal to the local database."))
    }
}
data class AddAnimalRequest(
    val owner: User,
    val name: String,
    val type: AnimalType,
    val breed: String,
    val hardiness: BreedHardiness,
    val weight: Double,
    val ageInMonths: Int,
    val purpose: AnimalPurpose,
    val initialFollowUpDate: Long? = null // <--- Opcional
)