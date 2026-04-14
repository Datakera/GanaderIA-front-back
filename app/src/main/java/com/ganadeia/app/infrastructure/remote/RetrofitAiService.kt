package com.ganadeia.app.infrastructure.remote

import com.ganadeia.app.domain.model.AiRecommendationRequest
import com.ganadeia.app.domain.model.AiRecommendationResponse
import com.ganadeia.app.domain.port.driven.service.AiServicePort
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// ── Retrofit API interface ────────────────────────────────────────────────────

/**
 * Contrato HTTP para el endpoint de recomendación de la API de FastAPI.
 *
 * El endpoint esperado en FastAPI es:
 *   POST /recommendations/analyze
 *
 * Request body:  [AiRecommendationRequestDto]
 * Response body: [AiRecommendationResponseDto]
 */
interface AiApiService {
    @POST("recommendations/analyze")
    suspend fun analyze(@Body request: AiRecommendationRequestDto): Response<AiRecommendationResponseDto>
}

// ── DTOs de red (lo que Retrofit serializa/deserializa) ───────────────────────

/**
 * DTO de request que Retrofit serializa a JSON y envía a FastAPI.
 * Los nombres de campo usan snake_case para coincidir con la convención Python.
 *
 * NOTA: Cuando implementes el endpoint en FastAPI, el modelo Pydantic
 * debe tener exactamente estos nombres de campo.
 */
data class AiRecommendationRequestDto(
    val animal_id: String,
    val animal_type: String,
    val breed: String,
    val hardiness: String,
    val current_weight_kg: Double,
    val age_in_months: Int,
    val purpose: String,
    val last_check_date: Long?,
    val last_check_weight_kg: Double?,
    val last_check_body_condition_score: Int?,
    val last_check_symptoms: List<String>,
    val health_history: List<HealthCheckSummaryDto>,
    val applied_vaccines: List<VaccineSummaryDto>,
    val pending_vaccines: List<VaccineSummaryDto>
)

data class HealthCheckSummaryDto(
    val date: Long,
    val weight_kg: Double,
    val body_condition_score: Int,
    val symptoms: List<String>
)

data class VaccineSummaryDto(
    val vaccine_name: String,
    val scheduled_date: Long,
    val applied_date: Long?
)

/**
 * DTO de response que Retrofit deserializa desde el JSON de FastAPI.
 * Debe coincidir con el modelo Pydantic de respuesta en FastAPI.
 */
data class AiRecommendationResponseDto(
    val animal_id: String,
    val generated_at: Long,
    val general_diagnosis: String,
    val priority_action: String,
    val nutritional_recommendation: String,
    val confidence_score: Float
)

// ── Implementación del puerto ─────────────────────────────────────────────────

/**
 * Implementación de [AiServicePort] usando Retrofit.
 *
 * Transforma los modelos de dominio en DTOs, llama al endpoint
 * y transforma la respuesta de vuelta a modelos de dominio.
 *
 * El caso de uso nunca ve Retrofit ni los DTOs: solo ve [AiServicePort].
 */
class RetrofitAiService(
    private val apiService: AiApiService
) : AiServicePort {

    override suspend fun requestRecommendation(
        request: AiRecommendationRequest
    ): Result<AiRecommendationResponse> {
        return try {
            val response = apiService.analyze(request.toDto())

            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(
                        RuntimeException("La API respondió vacío (HTTP ${response.code()}).")
                    )
                Result.success(body.toDomain())
            } else {
                Result.failure(
                    RuntimeException(
                        "Error HTTP ${response.code()}: ${response.errorBody()?.string()}"
                    )
                )
            }
        } catch (e: Exception) {
            // Cubre timeout, sin conexión, error de parseo, etc.
            Result.failure(e)
        }
    }

    // ── Mappers dominio → DTO ─────────────────────────────────────────────────

    private fun AiRecommendationRequest.toDto() = AiRecommendationRequestDto(
        animal_id                        = animalId,
        animal_type                      = animalType,
        breed                            = breed,
        hardiness                        = hardiness,
        current_weight_kg                = currentWeightKg,
        age_in_months                    = ageInMonths,
        purpose                          = purpose,
        last_check_date                  = lastCheckDate,
        last_check_weight_kg             = lastCheckWeightKg,
        last_check_body_condition_score  = lastCheckBodyConditionScore,
        last_check_symptoms              = lastCheckSymptoms,
        health_history                   = healthHistory.map { it.toDto() },
        applied_vaccines                 = appliedVaccines.map { it.toDto() },
        pending_vaccines                 = pendingVaccines.map { it.toDto() }
    )

    private fun com.ganadeia.app.domain.model.HealthCheckSummary.toDto() =
        HealthCheckSummaryDto(
            date                 = date,
            weight_kg            = weightKg,
            body_condition_score = bodyConditionScore,
            symptoms             = symptoms
        )

    private fun com.ganadeia.app.domain.model.VaccineSummary.toDto() =
        VaccineSummaryDto(
            vaccine_name   = vaccineName,
            scheduled_date = scheduledDate,
            applied_date   = appliedDate
        )

    // ── Mapper DTO → dominio ──────────────────────────────────────────────────

    private fun AiRecommendationResponseDto.toDomain() = AiRecommendationResponse(
        animalId                  = animal_id,
        generatedAt               = generated_at,
        generalDiagnosis          = general_diagnosis,
        priorityAction            = priority_action,
        nutritionalRecommendation = nutritional_recommendation,
        confidenceScore           = confidence_score
    )

    companion object {
        /**
         * Factory para crear la instancia de Retrofit.
         * En el futuro, reemplaza [baseUrl] por la URL real de tu FastAPI.
         * La inyección de dependencias (Hilt/Koin) debería proveer esta instancia.
         */
        fun create(baseUrl: String): RetrofitAiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return RetrofitAiService(retrofit.create(AiApiService::class.java))
        }
    }
}