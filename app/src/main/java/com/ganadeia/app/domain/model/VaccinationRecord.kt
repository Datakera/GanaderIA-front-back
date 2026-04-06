package com.ganadeia.app.domain.model

enum class VaccineStatus { PENDING, APPLIED, OVERDUE }

data class VaccinationRecord(
    val id: String,
    val animalId: String,
    val vaccineName: String,
    val scheduledDate: Long,
    val appliedDate: Long?,
    val status: VaccineStatus,
    val doseMl: Double?
)