package com.ganadeia.app.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ganadeia.app.infrastructure.persistence.room.entity.AiRecommendationEntity

@Dao
interface AiRecommendationDao {

    /**
     * Inserta una solicitud nueva (status PENDING).
     * REPLACE permite sobrescribir si por algún motivo se vuelve a insertar
     * con el mismo ID (ej: retry que recrea el registro).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AiRecommendationEntity)

    /**
     * Actualiza una recomendación existente cuando llega la respuesta de la IA
     * o cuando falla el intento. Actualiza todos los campos mutables.
     */
    @Query(
        """
        UPDATE ai_recommendations SET
            status                   = :status,
            generalDiagnosis         = :generalDiagnosis,
            priorityAction           = :priorityAction,
            nutritionalRecommendation = :nutritionalRecommendation,
            medicalRecommendation    = :medicalRecommendation,
            vaccineRecommendation    = :vaccineRecommendation,
            confidenceScore          = :confidenceScore,
            respondedAt              = :respondedAt,
            retryCount               = :retryCount,
            lastErrorMessage         = :lastErrorMessage
        WHERE id = :id
        """
    )
    suspend fun update(
        id: String,
        status: String,
        generalDiagnosis: String?,
        priorityAction: String?,
        nutritionalRecommendation: String?,
        medicalRecommendation: String?,
        vaccineRecommendation: String?,
        confidenceScore: Float?,
        respondedAt: Long?,
        retryCount: Int,
        lastErrorMessage: String?
    )

    /**
     * Historial completo de recomendaciones de un animal,
     * de la más reciente a la más antigua.
     */
    @Query(
        "SELECT * FROM ai_recommendations WHERE animalId = :animalId " +
                "ORDER BY requestedAt DESC"
    )
    suspend fun getByAnimal(animalId: String): List<AiRecommendationEntity>

    /**
     * La recomendación completada más reciente de un animal.
     * La usa la UI para mostrar el último diagnóstico en la ficha del animal.
     */
    @Query(
        "SELECT * FROM ai_recommendations WHERE animalId = :animalId " +
                "AND status = 'COMPLETED' ORDER BY requestedAt DESC LIMIT 1"
    )
    suspend fun getLastCompleted(animalId: String): AiRecommendationEntity?

    /**
     * Solicitudes pendientes o fallidas que el sistema debe reintentar
     * cuando vuelva la red. Ordenadas de la más antigua a la más reciente
     * para procesar en orden FIFO.
     */
    @Query(
        "SELECT * FROM ai_recommendations WHERE status IN ('PENDING', 'FAILED') " +
                "ORDER BY requestedAt ASC"
    )
    suspend fun getPending(): List<AiRecommendationEntity>

    /** Elimina un registro por ID. */
    @Query("DELETE FROM ai_recommendations WHERE id = :id")
    suspend fun delete(id: String)
}