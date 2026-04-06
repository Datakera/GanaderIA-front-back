package com.ganadeia.app.infrastructure.persistence.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que persiste un [HealthCheckRecord] localmente.
 *
 * Decisiones de diseño:
 *
 * - FOREIGN KEY a `animals(id)` con CASCADE DELETE: si un animal se borra,
 *   sus chequeos se borran también. Esto garantiza la integridad referencial
 *   ("vinculación rígida" del criterio de aceptación).
 *
 * - `symptoms` se serializa como String separado por comas (ej.
 *   "COJERA,FIEBRE"). Simple, sin dependencias extra. El repositorio
 *   se encarga de la conversión.
 *
 * - `syncStatus` se guarda como String para que sea legible en debugging
 *   y no se rompa si Room migra el esquema.
 */
@Entity(
    tableName = "health_checks",
    foreignKeys = [
        ForeignKey(
            entity = com.ganadeia.app.infrastructure.persistence.room.entity.AnimalEntity::class,
            parentColumns = ["id"],
            childColumns = ["animalId"],
            onDelete = ForeignKey.CASCADE   // integridad referencial garantizada
        )
    ],
    indices = [
        Index(value = ["animalId"]),        // acelera las consultas por animal
        Index(value = ["syncStatus"])       // acelera la búsqueda de pendientes
    ]
)
data class HealthCheckEntity(
    @PrimaryKey val id: String,
    val animalId: String,                   // FK rígida → animals.id
    val date: Long,

    // Datos clínicos
    val weightKg: Double,
    val bodyConditionScore: Int,
    val symptoms: String,                   // "COJERA,FIEBRE,DIARREA" (CSV)
    val notes: String?,

    // Resultado IA
    val aiRecommendation: String?,

    // Sincronización
    val syncStatus: String,                 // SyncStatus.name

    // Seguimiento
    val confirmedFollowUpDate: Long?
)