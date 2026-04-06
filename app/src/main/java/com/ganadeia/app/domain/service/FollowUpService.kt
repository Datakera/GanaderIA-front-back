package com.ganadeia.app.domain.service

import com.ganadeia.app.domain.model.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Servicio de Dominio encargado de la lógica experta para determinar la próxima fecha
 * de seguimiento técnico y sanitario de un animal.
 */
object FollowUpService {

    // Constantes de configuración de días (Fáciles de ajustar en el futuro)
    private const val CRITICAL_HEALTH_FOLLOW_UP_DAYS = 3
    private const val MODERATE_HEALTH_FOLLOW_UP_DAYS = 7
    private const val IDEAL_BODY_CONDITION = 3
    private const val ADULT_AGE_MONTHS = 24
    private const val CALF_AGE_MONTHS = 6

    fun calculateNextFollowUp(animal: Animal, lastRecord: HealthRecord?): Long {
        val baseTime = lastRecord?.date ?: System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = baseTime }
        val ageInMonths = getAgeInMonths(animal.birthDate, baseTime)

        val symptoms = lastRecord?.symptoms ?: emptySet()
        val hasSymptoms = symptoms.isNotEmpty() && symptoms.any { it != VisibleSymptom.NONE }

        // --- FACTOR 1: PRIORIDAD SANITARIA (ALERTA SEGÚN SEVERIDAD) ---
        if (hasSymptoms) {
            if (symptoms.any { it.isCritical }) {
                // Caso: Dificultad Respiratoria -> 3 días
                calendar.add(Calendar.DAY_OF_YEAR, CRITICAL_HEALTH_FOLLOW_UP_DAYS)
            } else {
                // Caso: Fiebre (Moderado) -> 7 días
                calendar.add(Calendar.DAY_OF_YEAR, MODERATE_HEALTH_FOLLOW_UP_DAYS)
            }
            return calendar.timeInMillis
        }

        // --- FACTOR 2: ETAPA DE VIDA (BASE) ---
        // Se ejecuta solo si NO hay síntomas (hasSymptoms == false)
        var daysToAdd = when {
            ageInMonths <= CALF_AGE_MONTHS -> 10
            ageInMonths <= ADULT_AGE_MONTHS -> 30
            else -> 45
        }

        // --- FACTOR 3: PERFIL GENÉTICO ---
        daysToAdd += when (animal.hardiness) {
            BreedHardiness.LOW -> -5
            BreedHardiness.MEDIUM -> 0
            BreedHardiness.HIGH -> 5
        }

        // --- FACTOR 4: PROPÓSITO PRODUCTIVO ---
        daysToAdd = when (animal.purpose) {
            AnimalPurpose.BREEDING -> if (daysToAdd > 21) 21 else daysToAdd
            AnimalPurpose.MILK -> if (daysToAdd > 15) 15 else daysToAdd
            AnimalPurpose.DUAL_PURPOSE -> if (daysToAdd > 20) 20 else daysToAdd
            AnimalPurpose.MEAT -> daysToAdd
        }

        // --- FACTOR 5: RIESGO FÍSICO (BCS) ---
        lastRecord?.let {
            if (it.bodyConditionScore < IDEAL_BODY_CONDITION) {
                daysToAdd -= 7
            }
        }

        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return calendar.timeInMillis
    }

    private fun getAgeInMonths(birthDate: Long, referenceDate: Long): Int {
        val diff = referenceDate - birthDate
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return (days / 30).toInt()
    }
}