package com.ganadeia.app.infrastructure.persistence.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que persiste un [AiRecommendationRecord] localmente.
 *
 * Decisiones de diseño:
 * - FK a `animals(id)` con CASCADE DELETE: si el animal se elimina,
 *   sus recomendaciones se eliminan también.
 * - Los campos de respuesta son nullable: están vacíos mientras la
 *   solicitud está en estado PENDING o FAILED.
 * - [status] se guarda como String (AiRecommendationStatus.name)
 *   para legibilidad y tolerancia a cambios del enum.
 * - [retryCount] permite al sistema de sincronización evitar
 *   reintentar indefinidamente solicitudes que siempre fallan.
 */
@Entity(
    tableName = "ai_recommendations",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["id"],
            childColumns = ["animalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["animalId"]),          // Acelera historial por animal
        Index(value = ["status"]),            // Acelera búsqueda de PENDING/FAILED
        Index(value = ["requestedAt"])        // Acelera ordenamiento cronológico
    ]
)
data class AiRecommendationEntity(
    @PrimaryKey val id: String,
    val animalId: String,
    val requestedAt: Long,

    // Estado: PENDING | COMPLETED | FAILED
    val status: String,

    // Respuesta de la IA — null mientras status == PENDING o FAILED
    val generalDiagnosis: String?,
    val priorityAction: String?,
    val nutritionalRecommendation: String?,
    val confidenceScore: Float?,
    val respondedAt: Long?,

    // Control de reintentos
    val retryCount: Int = 0,
    val lastErrorMessage: String? = null
)