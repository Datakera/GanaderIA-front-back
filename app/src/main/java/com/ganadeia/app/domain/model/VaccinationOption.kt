package com.ganadeia.app.domain.model

/**
 * DTO de presentación que expone una vacuna del catálogo al ganadero.
 *
 * Este objeto NO es persistido directamente. Es el "menú" que la UI
 * muestra al usuario para que seleccione y personalice antes de guardar.
 *
 * Flujo de vida:
 *   VaccinationService → VaccinationOption (UI la muestra)
 *                      → Usuario selecciona/edita
 *                      → ProcessedVaccinationInput (entra al use case)
 *                      → VaccinationRecord (se persiste en Room)
 */
data class VaccinationOption(
    /** Nombre técnico de la vacuna. Ej: "Fiebre Aftosa (Ciclo I)" */
    val vaccineName: String,

    /** Descripción educativa para el ganadero. Explica para qué sirve. */
    val educationalDescription: String,

    /** Fecha calculada por [VaccinationService] según la fecha de nacimiento del animal. */
    val suggestedDate: Long
)
