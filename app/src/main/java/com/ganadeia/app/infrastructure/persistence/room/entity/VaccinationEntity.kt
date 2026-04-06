package com.ganadeia.app.infrastructure.persistence.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que persiste un [VaccinationRecord] localmente.
 *
 * Decisiones de diseño:
 * - FK a `animals(id)` con CASCADE DELETE: si un animal se elimina,
 *   su historial de vacunación se elimina también.
 * - `status` se guarda como String (VaccineStatus.name) para legibilidad
 *   en debugging y tolerancia a futuros cambios del enum.
 * - `doseMl` es nullable porque no siempre se registra la dosis exacta.
 * - `appliedDate` es nullable: solo presente cuando status == "APPLIED".
 */
@Entity(
    tableName = "vaccinations",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["id"],
            childColumns = ["animalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["animalId"]),    // Acelera historial por animal
        Index(value = ["status"])       // Acelera búsqueda de PENDING / OVERDUE
    ]
)
data class VaccinationEntity(
    @PrimaryKey val id: String,
    val animalId: String,               // FK rígida → animals.id
    val vaccineName: String,
    val scheduledDate: Long,
    val appliedDate: Long?,             // Null hasta que se aplica
    val status: String,                 // VaccineStatus.name  (PENDING | APPLIED | OVERDUE)
    val doseMl: Double?                 // Opcional
)