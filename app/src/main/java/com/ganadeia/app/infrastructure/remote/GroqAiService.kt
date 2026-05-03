package com.ganadeia.app.infrastructure.remote

import com.ganadeia.app.domain.model.AiRecommendationRequest
import com.ganadeia.app.domain.model.AiRecommendationResponse
import com.ganadeia.app.domain.port.driven.service.AiServicePort
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ── Retrofit API interface para Groq ──────────────────────────────────────────

interface GroqApiService {
    @POST("openai/v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: GroqChatRequest
    ): Response<GroqChatResponse>
}

// ── DTOs de red para Groq ─────────────────────────────────────────────────────

data class GroqChatRequest(
    val model: String = "llama-3.1-8b-instant",
    val messages: List<GroqMessage>,
    val temperature: Double = 0.3,
    val response_format: GroqResponseFormat = GroqResponseFormat(type = "json_object")
)

data class GroqMessage(
    val role: String,
    val content: String
)

data class GroqResponseFormat(
    val type: String
)

data class GroqChatResponse(
    val id: String,
    val choices: List<GroqChoice>
)

data class GroqChoice(
    val message: GroqMessage
)

// DTO del JSON que le pediremos a Groq que devuelva
data class GroqStructuredResponse(
    @SerializedName("general_diagnosis") val generalDiagnosis: String,
    @SerializedName("priority_action") val priorityAction: String,
    @SerializedName("nutritional_recommendation") val nutritionalRecommendation: String,
    @SerializedName("medical_recommendation") val medicalRecommendation: String,
    @SerializedName("vaccine_recommendation") val vaccineRecommendation: String,
    @SerializedName("confidence_score") val confidenceScore: Float
)

// ── Implementación del puerto usando Groq ─────────────────────────────────────

class GroqAiService(
    private val apiService: GroqApiService,
    private val apiKey: String,
    private val gson: Gson = Gson()
) : AiServicePort {

    override suspend fun requestRecommendation(
        request: AiRecommendationRequest
    ): Result<AiRecommendationResponse> {
        return try {
            val systemPrompt = """
                Eres un asistente veterinario experto. Vas a recibir los datos de un animal (bovino), incluyendo su historial de salud y vacunas.
                Debes analizar los datos y devolver tu respuesta ÚNICAMENTE en formato JSON con la siguiente estructura exacta:
                {
                  "general_diagnosis": "Tu diagnóstico general aquí (máx 3 oraciones)",
                  "priority_action": "La acción principal a seguir (ej. Re-evaluar en 7 días...)",
                  "nutritional_recommendation": "Recomendación de alimentación (máx 2 oraciones)",
                  "medical_recommendation": "Recomendación médica completa: qué tratamientos, medicamentos o procedimientos veterinarios se sugieren para este animal según su estado actual de salud, síntomas observados y edad. Si está sano, indica chequeos preventivos recomendados (máx 3 oraciones)",
                  "vaccine_recommendation": "Plan de vacunación recomendado según la edad en meses del animal, su raza y síntomas. Indica vacunas específicas con nombres y edades recomendadas de aplicación. Si ya tiene vacunas aplicadas, sugiere refuerzos o vacunas faltantes (máx 3 oraciones)",
                  "confidence_score": 85.5
                }
                El confidence_score debe ser un número entre 0 y 100.
                No incluyas nada más en tu respuesta que no sea el JSON.
            """.trimIndent()

            val userPrompt = buildUserPrompt(request)

            val groqRequest = GroqChatRequest(
                messages = listOf(
                    GroqMessage(role = "system", content = systemPrompt),
                    GroqMessage(role = "user", content = userPrompt)
                )
            )

            val authHeader = "Bearer ${apiKey.trim()}"
            val response = apiService.createChatCompletion(authHeader, groqRequest)

            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(RuntimeException("Groq respondió vacío"))

                val content = body.choices.firstOrNull()?.message?.content
                    ?: return Result.failure(RuntimeException("No hay mensaje de respuesta en Groq"))

                // Parsear el JSON devuelto por Groq
                val structuredResponse = try {
                    gson.fromJson(content, GroqStructuredResponse::class.java)
                } catch (e: Exception) {
                    return Result.failure(RuntimeException("Error parseando JSON de Groq: $content", e))
                }

                Result.success(
                    AiRecommendationResponse(
                        animalId = request.animalId,
                        generatedAt = System.currentTimeMillis(),
                        generalDiagnosis = structuredResponse.generalDiagnosis,
                        priorityAction = structuredResponse.priorityAction,
                        nutritionalRecommendation = structuredResponse.nutritionalRecommendation,
                        medicalRecommendation = structuredResponse.medicalRecommendation,
                        vaccineRecommendation = structuredResponse.vaccineRecommendation,
                        confidenceScore = structuredResponse.confidenceScore
                    )
                )

            } else {
                Result.failure(
                    RuntimeException("Error HTTP ${response.code()}: ${response.errorBody()?.string()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildUserPrompt(request: AiRecommendationRequest): String {
        return """
            Datos del animal:
            - ID: ${request.animalId}
            - Tipo: ${request.animalType}
            - Raza: ${request.breed}
            - Rusticidad: ${request.hardiness}
            - Peso Actual: ${request.currentWeightKg} kg
            - Edad: ${request.ageInMonths} meses
            - Propósito: ${request.purpose}
            
            Historial de Salud:
            ${if (request.healthHistory.isEmpty()) "Sin historial." else request.healthHistory.joinToString("\n") { 
                "Fecha: ${it.date}, Peso: ${it.weightKg}kg, Condición Corporal: ${it.bodyConditionScore}, Síntomas: ${it.symptoms.joinToString()}" 
            }}
            
            Vacunas Aplicadas:
            ${if (request.appliedVaccines.isEmpty()) "Ninguna." else request.appliedVaccines.joinToString("\n") { it.vaccineName }}
            
            Vacunas Pendientes:
            ${if (request.pendingVaccines.isEmpty()) "Ninguna." else request.pendingVaccines.joinToString("\n") { it.vaccineName }}
        """.trimIndent()
    }

    companion object {
        fun create(apiKey: String): GroqAiService {
            val okHttpClient = OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.groq.com/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return GroqAiService(
                apiService = retrofit.create(GroqApiService::class.java),
                apiKey = apiKey
            )
        }
    }
}
