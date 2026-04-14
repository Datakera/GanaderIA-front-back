package com.ganadeia.app.application

import com.ganadeia.app.domain.model.AiRecommendationRecord
import com.ganadeia.app.domain.model.AiRecommendationRequest
import com.ganadeia.app.domain.model.AiRecommendationStatus
import com.ganadeia.app.domain.model.HealthCheckSummary
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.domain.model.VaccineSummary
import com.ganadeia.app.domain.model.VaccineStatus
import com.ganadeia.app.domain.port.driven.repository.AiRecommendationRepository
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository
import com.ganadeia.app.domain.port.driven.repository.HealthCheckRepository
import com.ganadeia.app.domain.port.driven.repository.VaccinationRepository
import com.ganadeia.app.domain.port.driven.service.AiServicePort
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Caso de Uso: Obtener recomendación de la IA para un animal.
 *
 * Historia de usuario:
 * "Como ganadero, quiero solicitar un análisis de IA para un animal,
 * para recibir un diagnóstico, una acción prioritaria y una recomendación
 * nutricional basados en todos sus datos históricos."
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * Criterios de Aceptación implementados:
 *
 * CA-1 Recopilación de datos:
 *      Se consolidan datos base del animal, último chequeo, historial
 *      completo (máximo 10 chequeos) y plan de vacunación antes de
 *      enviar la solicitud a la API.
 *
 * CA-2 Offline-first:
 *      La solicitud siempre se guarda en Room con status PENDING antes
 *      de intentar la llamada a la API. Si no hay red, queda guardada
 *      para ser procesada por [RetrySyncAiRecommendationsUseCase].
 *
 * CA-3 Actualización del registro al recibir respuesta:
 *      Si la API responde exitosamente, el registro en Room pasa a
 *      COMPLETED y se llenan los campos de diagnóstico.
 *      Si la API falla, el registro pasa a FAILED para ser reintentado.
 *
 * CA-4 Restricción de acceso:
 *      Solo el ganadero dueño del animal puede solicitar la recomendación.
 *
 * CA-5 Historial limitado:
 *      Se envían máximo los últimos [MAX_HEALTH_HISTORY] chequeos para
 *      no sobrecargar el payload de la API.
 * ──────────────────────────────────────────────────────────────────────────────
 */
class GetAiRecommendationUseCase(
    private val animalRepository: AnimalRepository,
    private val healthCheckRepository: HealthCheckRepository,
    private val vaccinationRepository: VaccinationRepository,
    private val aiRecommendationRepository: AiRecommendationRepository,
    private val aiServicePort: AiServicePort,
    private val isNetworkAvailable: () -> Boolean
) {

    suspend fun execute(
        owner: User,
        animalId: String,
        now: Long = System.currentTimeMillis()
    ): Result<GetAiRecommendationResult> {

        // CA-4: Solo ganaderos pueden solicitar recomendaciones
        if (owner.role != UserRole.RANCHER) {
            return Result.failure(
                IllegalArgumentException("Solo los ganaderos pueden solicitar recomendaciones de IA.")
            )
        }

        if (animalId.isBlank()) {
            return Result.failure(
                IllegalArgumentException("El ID del animal no puede estar vacío.")
            )
        }

        // CA-4: Verificar que el animal pertenece al dueño
        val animal = animalRepository
            .getAnimalsByOwner(owner.id)
            .firstOrNull { it.id == animalId }
            ?: return Result.failure(
                IllegalStateException(
                    "No se encontró el animal con ID '$animalId' " +
                            "para el usuario '${owner.id}'."
                )
            )

        // CA-1: Recopilar historial de chequeos (máx. 10) y vacunaciones
        val healthHistory = healthCheckRepository
            .getHealthChecksByAnimal(animalId)
            .take(MAX_HEALTH_HISTORY)

        val vaccinations = vaccinationRepository.getVaccinationsByAnimal(animalId)

        // CA-1: Calcular edad en meses a partir de birthDate
        val ageInMonths = TimeUnit.MILLISECONDS
            .toDays(now - animal.birthDate)
            .div(30)
            .toInt()

        val lastCheck = healthHistory.firstOrNull()

        // CA-1: Construir el payload para la API
        val request = AiRecommendationRequest(
            animalId            = animal.id,
            animalType          = animal.type.name,
            breed               = animal.breed,
            hardiness           = animal.hardiness.name,
            currentWeightKg     = animal.currentWeight,
            ageInMonths         = ageInMonths,
            purpose             = animal.purpose.name,
            lastCheckDate       = lastCheck?.date,
            lastCheckWeightKg   = lastCheck?.weightKg,
            lastCheckBodyConditionScore = lastCheck?.bodyConditionScore,
            lastCheckSymptoms   = lastCheck?.symptoms?.map { it.name } ?: emptyList(),
            healthHistory       = healthHistory.map { record ->
                HealthCheckSummary(
                    date                 = record.date,
                    weightKg             = record.weightKg,
                    bodyConditionScore   = record.bodyConditionScore,
                    symptoms             = record.symptoms.map { it.name }
                )
            },
            appliedVaccines  = vaccinations
                .filter { it.status == VaccineStatus.APPLIED }
                .map { VaccineSummary(it.vaccineName, it.scheduledDate, it.appliedDate) },
            pendingVaccines  = vaccinations
                .filter { it.status != VaccineStatus.APPLIED }
                .map { VaccineSummary(it.vaccineName, it.scheduledDate, null) }
        )

        // CA-2: Guardar solicitud en Room como PENDING antes de llamar a la API
        val pendingRecord = AiRecommendationRecord(
            id                        = UUID.randomUUID().toString(),
            animalId                  = animalId,
            requestedAt               = now,
            status                    = AiRecommendationStatus.PENDING,
            generalDiagnosis          = null,
            priorityAction            = null,
            nutritionalRecommendation = null,
            confidenceScore           = null,
            respondedAt               = null
        )

        val saved = aiRecommendationRepository.saveRecommendation(pendingRecord)
        if (!saved) {
            return Result.failure(
                RuntimeException("No se pudo guardar la solicitud en la base de datos local.")
            )
        }

        // CA-2: Si no hay red, devolvemos éxito con estado PENDING
        // El caso de uso de sincronización procesará esto cuando vuelva la red
        if (!isNetworkAvailable()) {
            return Result.success(
                GetAiRecommendationResult(
                    record = pendingRecord,
                    isImmediate = false
                )
            )
        }

        // CA-3: Hay red — intentar llamar a la API
        val apiResult = aiServicePort.requestRecommendation(request)

        return apiResult.fold(
            onSuccess = { response ->
                val completedRecord = pendingRecord.copy(
                    status                    = AiRecommendationStatus.COMPLETED,
                    generalDiagnosis          = response.generalDiagnosis,
                    priorityAction            = response.priorityAction,
                    nutritionalRecommendation = response.nutritionalRecommendation,
                    confidenceScore           = response.confidenceScore,
                    respondedAt               = response.generatedAt
                )
                aiRecommendationRepository.updateRecommendation(completedRecord)
                Result.success(
                    GetAiRecommendationResult(
                        record = completedRecord,
                        isImmediate = true
                    )
                )
            },
            onFailure = { error ->
                // CA-3: API falló — marcar como FAILED para reintento
                val failedRecord = pendingRecord.copy(
                    status           = AiRecommendationStatus.FAILED,
                    retryCount       = 1,
                    lastErrorMessage = error.message
                )
                aiRecommendationRepository.updateRecommendation(failedRecord)
                // Devolvemos failure con el error original para que la UI lo muestre
                Result.failure(
                    RuntimeException(
                        "La API de IA no está disponible en este momento. " +
                                "La solicitud quedó guardada y se reintentará automáticamente. " +
                                "Causa: ${error.message}"
                    )
                )
            }
        )
    }

    companion object {
        /** Máximo de chequeos históricos enviados al modelo para no sobrecargar el payload. */
        private const val MAX_HEALTH_HISTORY = 10
    }
}

/**
 * Resultado devuelto al ViewModel tras ejecutar el caso de uso.
 *
 * @param record       El registro de recomendación (PENDING, COMPLETED o FAILED).
 * @param isImmediate  true si la IA respondió en esta misma llamada.
 *                     false si quedó PENDING por falta de red.
 */
data class GetAiRecommendationResult(
    val record: AiRecommendationRecord,
    val isImmediate: Boolean
)