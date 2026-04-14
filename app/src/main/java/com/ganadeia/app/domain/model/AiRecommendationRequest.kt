package com.ganadeia.app.domain.model

/**
 * Payload que se envía a la API de FastAPI para solicitar una recomendación.
 *
 * Contiene toda la información que el modelo necesita para generar
 * un diagnóstico preciso: datos base del animal, último chequeo,
 * historial completo y estado de vacunación.
 *
 * Este objeto NO se persiste directamente. Es el DTO de salida
 * que el puerto [AiServicePort] serializa antes de enviarlo a la red.
 */
data class AiRecommendationRequest(

    // ── Datos base del animal ─────────────────────────────────────────────────
    val animalId: String,
    val animalType: String,          // AnimalType.name  (ej: "BOVINE")
    val breed: String,
    val hardiness: String,           // BreedHardiness.name (ej: "LOW")
    val currentWeightKg: Double,
    val ageInMonths: Int,
    val purpose: String,             // AnimalPurpose.name (ej: "MILK")

    // ── Último chequeo ────────────────────────────────────────────────────────
    val lastCheckDate: Long?,
    val lastCheckWeightKg: Double?,
    val lastCheckBodyConditionScore: Int?,
    val lastCheckSymptoms: List<String>,   // VisibleSymptom.name por cada síntoma

    // ── Historial completo de chequeos ────────────────────────────────────────
    // Ordenado del más reciente al más antiguo, máximo los últimos 10
    val healthHistory: List<HealthCheckSummary>,

    // ── Estado de vacunación ──────────────────────────────────────────────────
    val appliedVaccines: List<VaccineSummary>,
    val pendingVaccines: List<VaccineSummary>
)

/**
 * Resumen de un chequeo individual para el historial.
 * Se usa en lugar de [HealthRecord] completo para no enviar
 * campos internos (id, syncStatus, etc.) a la API externa.
 */
data class HealthCheckSummary(
    val date: Long,
    val weightKg: Double,
    val bodyConditionScore: Int,
    val symptoms: List<String>       // VisibleSymptom.name
)

/**
 * Resumen de una vacuna para el contexto de la IA.
 */
data class VaccineSummary(
    val vaccineName: String,
    val scheduledDate: Long,
    val appliedDate: Long?           // null si está pendiente
)

/**
 * Respuesta de la API de FastAPI con la recomendación generada por la IA.
 *
 * Todos los campos de texto vienen listos para mostrar al ganadero.
 * [confidenceScore] está en rango 0.0–1.0.
 */
data class AiRecommendationResponse(
    val animalId: String,
    val generatedAt: Long,           // epoch ms — cuándo generó la IA la respuesta
    val generalDiagnosis: String,
    val priorityAction: String,
    val nutritionalRecommendation: String,
    val confidenceScore: Float       // 0.0 – 1.0
)