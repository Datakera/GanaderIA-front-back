package com.ganadeia.app.application

import com.ganadeia.app.domain.model.AgeRange
import com.ganadeia.app.domain.model.HealthEvaluationSubmission
import com.ganadeia.app.domain.model.VisibleSymptom

/**
 * Caso de Uso: Enviar un animal a evaluación técnica por IA.
 *
 * Historia de Usuario: "Como ganadero en campo, quiero ingresar el peso,
 * rango de edad y seleccionar síntomas visibles de una lista, para enviar
 * la información técnica del animal a evaluación."
 *
 * Criterios de aceptación implementados:
 * 1. El campo de peso debe ser numérico y mayor a cero.
 * 2. Los síntomas son de selección múltiple (Set<VisibleSymptom>).
 * 3. El caso de uso solo procede si los datos obligatorios están presentes:
 *    - animalId (no vacío)
 *    - weightKg (> 0)
 *    - ageRangeMonths (no nulo)
 *    Los síntomas son opcionales; la evaluación IA puede operar sin ellos,
 *    aunque el modelo será más preciso con síntomas indicados.
 */
class SubmitAnimalForEvaluationUseCase {

    /**
     * Valida los datos ingresados en campo y construye un
     * [HealthEvaluationSubmission] listo para ser enviado al servicio de IA.
     *
     * @param animalId   Identificador o arete del animal. Obligatorio.
     * @param weightKg   Peso actual en kilogramos. Debe ser numérico y > 0. Obligatorio.
     * @param ageRange   Rango de edad seleccionado. Obligatorio.
     * @param symptoms   Conjunto de síntomas visibles seleccionados. Puede estar vacío.
     *
     * @return [Result.success] con el [HealthEvaluationSubmission] si todos los
     *         datos obligatorios son válidos, o [Result.failure] con un mensaje
     *         descriptivo del error de validación.
     */
    fun execute(
        animalId: String,
        weightKg: Double?,
        ageRange: AgeRange?,
        symptoms: Set<VisibleSymptom>
    ): Result<HealthEvaluationSubmission> {

        // Validación 1: ID del animal no puede estar vacío
        if (animalId.isBlank()) {
            return Result.failure(
                IllegalArgumentException("El identificador del animal es obligatorio.")
            )
        }

        // Validación 2: Peso es obligatorio y debe ser numérico mayor a cero
        if (weightKg == null) {
            return Result.failure(
                IllegalArgumentException("El peso del animal es obligatorio.")
            )
        }
        if (weightKg <= 0) {
            return Result.failure(
                IllegalArgumentException("El peso del animal debe ser mayor a cero.")
            )
        }
        // Límite razonable para bovinos (evitar errores de tipeo)
        if (weightKg > 1500) {
            return Result.failure(
                IllegalArgumentException("El peso ingresado ($weightKg kg) parece inusualmente alto para un bovino.")
            )
        }

        // Validación 3: El rango de edad es obligatorio
        if (ageRange == null) {
            return Result.failure(
                IllegalArgumentException("El rango de edad del animal es obligatorio.")
            )
        }

        val submission = HealthEvaluationSubmission(
            animalId = animalId.trim(),
            weightKg = weightKg,
            ageRangeMonths = ageRange,
            symptoms = symptoms,
            submittedAt = System.currentTimeMillis()
        )

        return Result.success(submission)
    }
}