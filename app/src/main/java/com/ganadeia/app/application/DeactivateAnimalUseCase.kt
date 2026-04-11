package com.ganadeia.app.application

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.AnimalStatus
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository

/**
 * Caso de Uso: Desactivar un animal (venta o muerte).
 *
 * Historia de usuario:
 * "Como ganadero, quiero poder registrar que un animal fue vendido o murió,
 * para que no aparezca en mis listas activas ni genere alertas de seguimiento,
 * pero conservar su historial para trazabilidad."
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * Criterios de Aceptación implementados:
 *
 * CA-1 Solo el dueño puede desactivar su animal:
 *      Se verifica rol RANCHER y propiedad del animal.
 *
 * CA-2 Se requiere una razón explícita:
 *      El ganadero debe especificar [DeactivationReason.SOLD] o
 *      [DeactivationReason.DECEASED]. Esto determina el nuevo [AnimalStatus].
 *
 * CA-3 No se puede desactivar un animal ya inactivo:
 *      Si el animal ya tiene status INACTIVE o SOLD, se retorna failure
 *      para evitar operaciones redundantes.
 *
 * CA-4 El historial se conserva:
 *      Solo se cambia el [AnimalStatus]. Todos los chequeos, vacunaciones
 *      y registros asociados permanecen intactos en Room (no hay DELETE).
 *
 * CA-5 Persistencia local primero (offline-first):
 *      El cambio se guarda en Room sin requerir red.
 * ──────────────────────────────────────────────────────────────────────────────
 */
class DeactivateAnimalUseCase(
    private val animalRepository: AnimalRepository
) {

    suspend fun execute(request: DeactivateAnimalRequest): Result<Animal> {

        // CA-1: Solo ganaderos pueden desactivar animales
        if (request.owner.role != UserRole.RANCHER) {
            return Result.failure(
                IllegalArgumentException("Solo los ganaderos pueden desactivar animales.")
            )
        }

        if (request.animalId.isBlank()) {
            return Result.failure(
                IllegalArgumentException("El ID del animal no puede estar vacío.")
            )
        }

        // CA-1: Verificar que el animal pertenece al dueño
        val existingAnimal = animalRepository
            .getAnimalsByOwner(request.owner.id)
            .firstOrNull { it.id == request.animalId }
            ?: return Result.failure(
                IllegalStateException(
                    "No se encontró el animal con ID '${request.animalId}' " +
                            "para el usuario '${request.owner.id}'."
                )
            )

        // CA-3: Verificar que el animal aún está activo
        if (existingAnimal.status != AnimalStatus.ACTIVE) {
            return Result.failure(
                IllegalStateException(
                    "El animal '${existingAnimal.name}' ya se encuentra " +
                            "en estado ${existingAnimal.status.name}. " +
                            "No es posible desactivarlo nuevamente."
                )
            )
        }

        // CA-2: Determinar el nuevo status según la razón
        val newStatus = when (request.reason) {
            DeactivationReason.SOLD -> AnimalStatus.SOLD
            DeactivationReason.DECEASED -> AnimalStatus.INACTIVE
        }

        // CA-4: Conservar completo el historial, solo cambiar el status
        val deactivatedAnimal = existingAnimal.copy(
            status = newStatus,
            nextFollowUpDate = null // Ya no necesita seguimiento
        )

        // CA-5: Persistir en Room
        val success = animalRepository.addAnimal(request.owner.id, deactivatedAnimal)

        return if (success) Result.success(deactivatedAnimal)
        else Result.failure(
            RuntimeException(
                "No se pudo actualizar el estado del animal en la base de datos local."
            )
        )
    }
}

/**
 * Razón por la que el animal sale del hato activo.
 * Determina el [AnimalStatus] resultante:
 * - SOLD → AnimalStatus.SOLD
 * - DECEASED → AnimalStatus.INACTIVE
 */
enum class DeactivationReason {
    /** El animal fue vendido a otro productor. */
    SOLD,

    /** El animal murió (enfermedad, accidente, sacrificio). */
    DECEASED
}

data class DeactivateAnimalRequest(
    val owner: User,
    val animalId: String,
    val reason: DeactivationReason,
    val notes: String? = null  // Observaciones opcionales (precio de venta, causa de muerte, etc.)
)