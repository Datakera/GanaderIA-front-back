package com.ganadeia.app.domain.port.driven.service

import com.ganadeia.app.domain.model.AiRecommendationRequest
import com.ganadeia.app.domain.model.AiRecommendationResponse

/**
 * Puerto de salida (driven port) que define el contrato de comunicación
 * con el servicio externo de IA (FastAPI).
 *
 * La implementación concreta ([RetrofitAiService]) vive en la capa de
 * infraestructura y es inyectada en el caso de uso.
 *
 * El caso de uso NO sabe si la implementación usa Retrofit, Ktor,
 * o cualquier otro cliente HTTP. Solo conoce este contrato.
 */
interface AiServicePort {

    /**
     * Envía los datos del animal a la API de FastAPI y espera la recomendación.
     *
     * @param request  Payload completo con datos del animal, chequeos y vacunas.
     * @return [Result.success] con la [AiRecommendationResponse] si la API
     *         respondió correctamente, o [Result.failure] con el error
     *         (timeout, error HTTP, sin conexión, etc.).
     */
    suspend fun requestRecommendation(request: AiRecommendationRequest): Result<AiRecommendationResponse>
}