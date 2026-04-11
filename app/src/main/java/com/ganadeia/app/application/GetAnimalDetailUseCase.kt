package com.ganadeia.app.application

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.HealthRecord
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.model.VaccinationRecord
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository
import com.ganadeia.app.domain.port.driven.repository.HealthCheckRepository
import com.ganadeia.app.domain.port.driven.repository.VaccinationRepository

/**
 * Caso de Uso: Obtener la ficha completa de un animal.
 *
 * Historia de usuario:
 * "Como ganadero, quiero ver la ficha completa de un animal con su historial
 * de chequeos y vacunaciones, para tener toda la información disponible antes
 * de enviarla a la IA o tomar decisiones sobre el animal."
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * Criterios de Aceptación implementados:
 *
 * CA-1 Acceso restringido al dueño:
 *      Solo el ganadero dueño del animal puede ver su ficha.
 *
 * CA-2 Datos consolidados en un solo resultado:
 *      Devuelve [AnimalDetail] con el animal, su historial de chequeos
 *      y su historial de vacunaciones, listo para ser consumido por la UI
 *      o por el caso de uso de la IA.
 *
 * CA-3 Historial ordenado:
 *      Los chequeos vienen del más reciente al más antiguo (lo hace el DAO).
 *      Las vacunaciones vienen ordenadas por fecha programada (lo hace el DAO).
 *
 * CA-4 Funciona offline:
 *      Todos los datos provienen de Room. No requiere red.
 * ──────────────────────────────────────────────────────────────────────────────
 */
class GetAnimalDetailUseCase(
    private val animalRepository: AnimalRepository,
    private val healthCheckRepository: HealthCheckRepository,
    private val vaccinationRepository: VaccinationRepository
) {

    suspend fun execute(owner: User, animalId: String): Result<AnimalDetail> {

        // CA-1: Verificar rol
        if (owner.role != UserRole.RANCHER) {
            return Result.failure(
                IllegalArgumentException("Solo los ganaderos pueden acceder a fichas de animales.")
            )
        }

        if (animalId.isBlank()) {
            return Result.failure(
                IllegalArgumentException("El ID del animal no puede estar vacío.")
            )
        }

        // CA-1: Verificar que el animal pertenece al dueño
        val animal = animalRepository
            .getAnimalsByOwner(owner.id)
            .firstOrNull { it.id == animalId }
            ?: return Result.failure(
                IllegalStateException(
                    "No se encontró el animal con ID '$animalId' " +
                            "para el usuario '${owner.id}'."
                )
            )

        // CA-2 + CA-3: Obtener historial de chequeos y vacunaciones en paralelo
        val healthHistory = healthCheckRepository.getHealthChecksByAnimal(animalId)
        val vaccinationHistory = vaccinationRepository.getVaccinationsByAnimal(animalId)

        return Result.success(
            AnimalDetail(
                animal = animal,
                healthHistory = healthHistory,
                vaccinationHistory = vaccinationHistory,
                lastHealthCheck = healthHistory.firstOrNull()
            )
        )
    }
}

/**
 * Ficha completa de un animal con toda su información consolidada.
 * Este objeto es lo que la UI muestra en la pantalla de detalle,
 * y también es lo que el caso de uso de IA necesita como entrada.
 *
 * @param animal              Datos base del animal.
 * @param healthHistory       Historial de chequeos, del más reciente al más antiguo.
 * @param vaccinationHistory  Historial de vacunaciones ordenado por fecha programada.
 * @param lastHealthCheck     Último chequeo registrado. Null si nunca fue chequeado.
 */
data class AnimalDetail(
    val animal: Animal,
    val healthHistory: List<HealthRecord>,
    val vaccinationHistory: List<VaccinationRecord>,
    val lastHealthCheck: HealthRecord?
)