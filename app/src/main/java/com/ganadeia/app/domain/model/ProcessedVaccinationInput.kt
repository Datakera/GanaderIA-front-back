package com.ganadeia.app.domain.model

/**
 * Representa una vacuna ya procesada por la interfaz de usuario,
 * lista para ser validada y persistida por el caso de uso.
 *
 * La UI construye esta clase por cada [VaccinationOption] del catálogo,
 * indicando si el ganadero la seleccionó y cómo la configuró.
 *
 * @param vaccineName       Nombre técnico de la vacuna (viene del catálogo).
 * @param isSelected        true si el ganadero marcó esta vacuna para guardar.
 * @param scheduledDate     Fecha elegida. Puede ser la sugerida o una editada manualmente.
 * @param initialStatus     [VaccineStatus.PENDING] si es a futuro,
 *                          [VaccineStatus.APPLIED] si ya fue aplicada.
 * @param appliedDate       Obligatorio cuando [initialStatus] == APPLIED.
 *                          Debe ser null cuando [initialStatus] == PENDING.
 * @param doseMl            Dosis opcional en mililitros.
 */
data class ProcessedVaccinationInput(
    val vaccineName: String,
    val isSelected: Boolean,
    val scheduledDate: Long,
    val initialStatus: VaccineStatus,
    val appliedDate: Long?,
    val doseMl: Double?
)
