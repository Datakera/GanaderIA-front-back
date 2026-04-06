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
    private const val IDEAL_BODY_CONDITION = 3
    private const val ADULT_AGE_MONTHS = 24
    private const val CALF_AGE_MONTHS = 6

    fun calculateNextFollowUp(animal: Animal, lastRecord: HealthRecord?): Long {
        val calendar = Calendar.getInstance()
        val ageInMonths = getAgeInMonths(animal.birthDate)

        // --- FACTOR 1: SALUD CRÍTICA (ALERTA ROJA) ---
        // Si hay síntomas activos (que no sea NONE), el seguimiento es casi inmediato.
        val hasSymptoms = lastRecord?.symptoms?.any { it != Symptom.NONE } ?: false
        if (hasSymptoms) {
            calendar.add(Calendar.DAY_OF_YEAR, CRITICAL_HEALTH_FOLLOW_UP_DAYS)
            return calendar.timeInMillis
        }

        // --- FACTOR 2: ETAPA DE VIDA (BASE) ---
        // Los animales jóvenes requieren supervisión constante para asegurar crecimiento.
        var daysToAdd = when {
            ageInMonths <= CALF_AGE_MONTHS -> 10 // Terneros: Chequeo muy frecuente
            ageInMonths <= ADULT_AGE_MONTHS -> 30 // Jóvenes: Control mensual
            else -> 45 // Adultos sanos: Control trimestral/bimestral
        }

        // --- FACTOR 3: PERFIL GENÉTICO (RESISTENCIA) ---
        // Ajustamos según qué tan delicada es la raza (Baja resistencia = más chequeos).
        daysToAdd += when (animal.hardiness) {
            BreedHardiness.LOW -> -5    // Ej: Holstein (Europeas) necesitan más ojo
            BreedHardiness.MEDIUM -> 0   // Cruces F1
            BreedHardiness.HIGH -> 5    // Ej: Brahman (Cebú) aguantan más
        }

        // --- FACTOR 4: PROPÓSITO PRODUCTIVO ---
        // Adaptamos el calendario al ciclo de producción del ganadero.
        daysToAdd = when (animal.purpose) {
            AnimalPurpose.BREEDING -> if (daysToAdd > 21) 21 else daysToAdd // Ciclo de celo
            AnimalPurpose.MILK -> if (daysToAdd > 15) 15 else daysToAdd     // Control de mastitis
            AnimalPurpose.DUAL_PURPOSE -> if (daysToAdd > 20) 20 else daysToAdd
            AnimalPurpose.MEAT -> daysToAdd // Se rige principalmente por edad/peso
        }

        // --- FACTOR 5: RIESGO FÍSICO (CONDICIÓN CORPORAL) ---
        // Si el animal está flaco (BCS < 3), adelantamos el chequeo para ajustar dieta.
        lastRecord?.let {
            if (it.bodyCondition < IDEAL_BODY_CONDITION) {
                daysToAdd -= 7 // Adelantamos una semana para monitorear recuperación
            }
        }

        // Aplicamos el cálculo final
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return calendar.timeInMillis
    }

    /**
     * Helper para calcular la edad exacta en meses a partir del timestamp de nacimiento.
     */
    private fun getAgeInMonths(birthDate: Long): Int {
        val diff = System.currentTimeMillis() - birthDate
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return (days / 30).toInt()
    }
}