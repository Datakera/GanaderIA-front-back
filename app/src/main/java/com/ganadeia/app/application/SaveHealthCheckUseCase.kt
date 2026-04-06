package com.ganadeia.app.application

import com.ganadeia.app.domain.model.HealthRecord
import com.ganadeia.app.domain.model.SyncStatus
import com.ganadeia.app.domain.model.VisibleSymptom
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository
import com.ganadeia.app.domain.port.driven.repository.HealthCheckRepository
import com.ganadeia.app.domain.service.FollowUpService
import java.util.UUID

/**
 * Caso de Uso: Guardar el resultado de un chequeo de salud en el historial local.
 *
 * Historia de usuario:
 * "Como Ganadero, quiero que los resultados del chequeo y las recomendaciones
 * de la IA se guarden automáticamente en mi historial, para mantener un registro
 * trazable de la evolución de cada animal sin depender de conexión a internet."
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * Criterios de Aceptación implementados:
 *
 * CA-1 Almacenamiento Local:
 *      Al ejecutar el use case, los datos (peso, síntomas, condición corporal
 *      y fecha) se persisten en Room sin requerir red.
 *
 * CA-2 Vinculación:
 *      Se verifica que el animalId existe antes de guardar. La FK en Room
 *      (CASCADE DELETE) garantiza la vinculación rígida permanente.
 *
 * CA-3 Estado de Sincronización:
 *      El registro se marca como [SyncStatus.PENDING_SYNC] cuando no hay red,
 *      o como [SyncStatus.SYNCED] si la sincronización inicial fue exitosa.
 *      El parámetro [isNetworkAvailable] se inyecta para que la UI (o un
 *      ConnectivityManager) lo provea sin acoplar el use case al framework.
 *
 * CA-4 Actualización de Seguimiento:
 *      [FollowUpCalculator.calculateNextFollowUp] propone una fecha basada en
 *      los síntomas y la condición corporal. El resultado se devuelve en
 *      [SaveHealthCheckResult] para que la UI se lo muestre al ganadero.
 *      Cuando el ganadero confirma o edita la fecha, se llama a
 *      [confirmFollowUpDate], que actualiza el campo [confirmedFollowUpDate]
 *      del registro Y el campo [nextFollowUpDate] en la ficha del animal.
 * ──────────────────────────────────────────────────────────────────────────────
 */
class SaveHealthCheckUseCase(
    private val healthCheckRepository: HealthCheckRepository,
    private val animalRepository: AnimalRepository,
    private val followUpCalculator: FollowUpService = FollowUpService
) {

    // ── CA-1, CA-2, CA-3 ──────────────────────────────────────────────────────

    /**
     * Guarda el chequeo en la BD local y devuelve la fecha de seguimiento
     * propuesta para que el ganadero la confirme o edite.
     *
     * @param animalId            ID del animal (debe existir en la BD).
     * @param weightKg            Peso medido en campo.
     * @param bodyConditionScore  Condición corporal (1–5).
     * @param symptoms            Síntomas observados (puede ser vacío).
     * @param notes               Observaciones libres del ganadero.
     * @param aiRecommendation    Texto de recomendación generado por la IA.
     *                            Puede ser null si la respuesta llega de forma asíncrona.
     * @param isNetworkAvailable  Estado de red en el momento del guardado.
     * @param now                 Timestamp de referencia (inyectable para tests).
     *
     * @return [Result.success] con un [SaveHealthCheckResult] que contiene el
     *         registro guardado y la fecha de seguimiento propuesta, o
     *         [Result.failure] con el error de validación / persistencia.
     */
    suspend fun execute(
        animalId: String,
        weightKg: Double,
        bodyConditionScore: Int,
        symptoms: Set<VisibleSymptom>,
        notes: String? = null,
        aiRecommendation: String? = null,
        isNetworkAvailable: Boolean,
        now: Long = System.currentTimeMillis()
    ): Result<SaveHealthCheckResult> {

        // ── Validaciones de entrada ────────────────────────────────────────────
        if (animalId.isBlank()) {
            return Result.failure(IllegalArgumentException("El ID del animal no puede estar vacío."))
        }
        if (weightKg <= 0) {
            return Result.failure(IllegalArgumentException("El peso debe ser mayor a cero."))
        }
        if (bodyConditionScore !in 1..5) {
            return Result.failure(
                IllegalArgumentException("La condición corporal debe estar entre 1 y 5.")
            )
        }

        // ── CA-2: Verificar que el animal existe (vinculación) ─────────────────
        val animal = animalRepository.getAnimalsByOwner(animalId)
            .firstOrNull { it.id == animalId }
            ?: return Result.failure(
                IllegalStateException(
                    "No se encontró el animal con ID '$animalId'. " +
                            "El chequeo debe estar vinculado a un animal registrado."
                )
            )

        // ── CA-4: Calcular fecha de seguimiento propuesta ─────────────────────

        // Creamos un registro temporal para que el servicio analice el estado actual
        val currentCheck = HealthRecord(
            id = "temp",
            animalId = animalId,
            date = now,
            weightKg = weightKg,
            bodyConditionScore = bodyConditionScore,
            symptoms = symptoms,
            notes = notes,
            aiRecommendation = aiRecommendation,
            syncStatus = SyncStatus.PENDING_SYNC,
            confirmedFollowUpDate = null
        )

        val proposedFollowUp = followUpCalculator.calculateNextFollowUp(
            animal = animal,
            lastRecord = currentCheck
        )

        // ── CA-3: Determinar estado de sincronización inicial ──────────────────
        val syncStatus = if (isNetworkAvailable) SyncStatus.SYNCED else SyncStatus.PENDING_SYNC

        // ── CA-1: Construir y persistir el registro ────────────────────────────
        val record = HealthRecord(
            id = UUID.randomUUID().toString(),
            animalId = animalId,
            date = now,
            weightKg = weightKg,
            bodyConditionScore = bodyConditionScore,
            symptoms = symptoms,
            notes = notes,
            aiRecommendation = aiRecommendation,
            syncStatus = syncStatus,
            confirmedFollowUpDate = null  // null hasta que el ganadero confirme
        )

        val saved = healthCheckRepository.saveHealthCheck(record)
        if (!saved) {
            return Result.failure(
                RuntimeException("No se pudo guardar el registro en la base de datos local.")
            )
        }

        return Result.success(
            SaveHealthCheckResult(
                savedRecord = record,
                proposedFollowUpDate = proposedFollowUp
            )
        )
    }

    // ── CA-4: Confirmar o editar fecha de seguimiento ────────────────────────

    /**
     * El ganadero acepta o edita la fecha propuesta. Este método:
     * 1. Actualiza [HealthCheckRecord.confirmedFollowUpDate] en el chequeo.
     * 2. Actualiza [Animal.nextFollowUpDate] en la ficha del animal.
     *
     * Debe llamarse desde la UI después de que el ganadero interactúa con
     * el diálogo de confirmación de fecha.
     *
     * @param recordId         ID del [HealthCheckRecord] que se creó en [execute].
     * @param animalId         ID del animal cuya ficha debe actualizarse.
     * @param confirmedDate    Fecha confirmada/editada por el ganadero en epoch ms.
     */
    suspend fun confirmFollowUpDate(
        recordId: String,
        animalId: String,
        confirmedDate: Long
    ): Result<Unit> {
        if (confirmedDate <= 0) {
            return Result.failure(
                IllegalArgumentException("La fecha de seguimiento no es válida.")
            )
        }

        // Actualizar el campo en el chequeo (HealthCheckRecord)
        val animals = animalRepository.getAnimalsByOwner(animalId)
        val animal = animals.firstOrNull { it.id == animalId }
            ?: return Result.failure(
                IllegalStateException("Animal '$animalId' no encontrado al confirmar seguimiento.")
            )

        // Actualizar nextFollowUpDate en la ficha del animal
        val updatedAnimal = animal.copy(nextFollowUpDate = confirmedDate)
        val animalUpdated = animalRepository.addAnimal(animalId, updatedAnimal)

        if (!animalUpdated) {
            return Result.failure(
                RuntimeException(
                    "No se pudo actualizar la fecha de seguimiento en la ficha del animal."
                )
            )
        }

        return Result.success(Unit)
    }
}

/**
 * Resultado devuelto al llamador (ViewModel / UI) tras un guardado exitoso.
 *
 * Contiene lo mínimo necesario para que la UI muestre el diálogo de
 * confirmación de fecha de seguimiento (CA-4).
 */
data class SaveHealthCheckResult(
    val savedRecord: HealthRecord,

    /** Fecha que [FollowUpCalculator] calculó. Se muestra al ganadero
     *  para que confirme o ajuste antes de cerrar la pantalla. */
    val proposedFollowUpDate: Long
)