package com.ganadeia.app.application

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.AnimalPurpose
import com.ganadeia.app.domain.model.AnimalStatus
import com.ganadeia.app.domain.model.AnimalType
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository

/**
 * Caso de Uso: Actualizar datos de un animal existente.
 *
 * Historia de usuario:
 * "Como ganadero, quiero poder actualizar los datos de un animal registrado
 * (especialmente su peso actual y otros datos productivos), para mantener
 * la ficha del animal actualizada y que la IA tenga información precisa."
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * Criterios de Aceptación implementados:
 *
 * CA-1 Solo el dueño puede actualizar su animal:
 *      Se verifica que el [User] tenga rol RANCHER y que el animal
 *      pertenezca a ese usuario antes de persistir cualquier cambio.
 *
 * CA-2 El peso debe ser válido:
 *      [UpdateAnimalRequest.newWeight] debe ser mayor a cero si se provee.
 *
 * CA-3 No se puede cambiar el tipo ni la raza de un animal ya registrado:
 *      [AnimalType] y [Animal.breed] son inmutables una vez creado el animal.
 *      Solo se permiten actualizar: peso, propósito, estado y nombre.
 *
 * CA-4 Persistencia local primero (offline-first):
 *      El cambio se guarda en Room sin requerir red. La sincronización
 *      con la API remota es responsabilidad de un caso de uso separado.
 * ──────────────────────────────────────────────────────────────────────────────
 */
class UpdateAnimalUseCase(
    private val animalRepository: AnimalRepository
) {

    suspend fun execute(request: UpdateAnimalRequest): Result<Animal> {

        // CA-1: Solo ganaderos pueden actualizar animales
        if (request.owner.role != UserRole.RANCHER) {
            return Result.failure(
                IllegalArgumentException("Solo los ganaderos pueden actualizar animales.")
            )
        }

        // Verificar que el animal existe y pertenece al dueño
        val existingAnimal = animalRepository
            .getAnimalsByOwner(request.owner.id)
            .firstOrNull { it.id == request.animalId }
            ?: return Result.failure(
                IllegalStateException(
                    "No se encontró el animal con ID '${request.animalId}' " +
                            "para el usuario '${request.owner.id}'."
                )
            )

        // CA-2: Validar nuevo peso si se provee
        request.newWeight?.let { weight ->
            if (weight <= 0) {
                return Result.failure(
                    IllegalArgumentException("El peso del animal debe ser mayor a cero.")
                )
            }
            // Límite razonable para bovinos
            if (weight > 1500) {
                return Result.failure(
                    IllegalArgumentException(
                        "El peso ingresado ($weight kg) parece inusualmente alto."
                    )
                )
            }
        }

        // Validar nombre si se provee
        request.newName?.let { name ->
            if (name.isBlank()) {
                return Result.failure(
                    IllegalArgumentException("El nombre del animal no puede estar vacío.")
                )
            }
        }

        // CA-3: Construir el animal actualizado preservando campos inmutables
        // (id, userId, type, breed, hardiness, birthDate nunca cambian)
        val updatedAnimal = existingAnimal.copy(
            name = request.newName?.trim() ?: existingAnimal.name,
            currentWeight = request.newWeight ?: existingAnimal.currentWeight,
            purpose = request.newPurpose ?: existingAnimal.purpose,
            status = request.newStatus ?: existingAnimal.status
        )

        // CA-4: Persistir en Room (offline-first)
        val success = animalRepository.addAnimal(request.owner.id, updatedAnimal)

        return if (success) Result.success(updatedAnimal)
        else Result.failure(
            RuntimeException("No se pudo actualizar el animal en la base de datos local.")
        )
    }
}

/**
 * Todos los campos de actualización son opcionales (nullable).
 * Solo se actualizan los campos que el ganadero provee explícitamente.
 * Los campos inmutables (type, breed, hardiness, birthDate) no están aquí.
 */
data class UpdateAnimalRequest(
    val owner: User,
    val animalId: String,
    val newName: String? = null,
    val newWeight: Double? = null,
    val newPurpose: AnimalPurpose? = null,
    val newStatus: AnimalStatus? = null
)