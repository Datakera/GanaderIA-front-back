package com.ganadeia.app.domain.model

/**
 * Registro persistido localmente de una recomendación generada por la IA.
 *
 * Ciclo de vida:
 *   1. El ganadero solicita una recomendación → se crea con
 *      [AiRecommendationStatus.PENDING] y sin datos de respuesta.
 *   2. Cuando hay red y la API responde → status pasa a [AiRecommendationStatus.COMPLETED]
 *      y se llenan los campos de diagnóstico.
 *   3. Si la API falla → status pasa a [AiRecommendationStatus.FAILED]
 *      y el sistema puede reintentarlo.
 *
 * Esto permite el patrón offline-first: la solicitud se guarda localmente
 * aunque no haya red, y se procesa cuando la conexión vuelva.
 */
data class AiRecommendationRecord(
    val id: String,
    val animalId: String,
    val requestedAt: Long,               // Cuándo el ganadero solicitó la recomendación

    // ── Estado de la solicitud ────────────────────────────────────────────────
    val status: AiRecommendationStatus,

    // ── Respuesta de la IA (null hasta que llega la respuesta) ────────────────
    val generalDiagnosis: String?,
    val priorityAction: String?,
    val nutritionalRecommendation: String?,
    val medicalRecommendation: String?,
    val vaccineRecommendation: String?,
    val confidenceScore: Float?,
    val respondedAt: Long?,              // Cuándo respondió la API

    // ── Control de reintentos ─────────────────────────────────────────────────
    val retryCount: Int = 0,
    val lastErrorMessage: String? = null
)

/**
 * Estado de la solicitud de recomendación a la IA.
 *
 * Máquina de estados:
 *   PENDING ──(hay red, API responde)──► COMPLETED
 *   PENDING ──(hay red, API falla)────► FAILED ──(retry)──► PENDING
 *   PENDING ──(sin red)───────────────► PENDING  (queda guardada, se reintenta)
 */
enum class AiRecommendationStatus {
    /** Solicitud guardada localmente, aún no enviada o esperando respuesta. */
    PENDING,

    /** La IA respondió exitosamente. Los campos de diagnóstico están completos. */
    COMPLETED,

    /** El último intento falló. Se reintentará en la próxima sincronización. */
    FAILED
}