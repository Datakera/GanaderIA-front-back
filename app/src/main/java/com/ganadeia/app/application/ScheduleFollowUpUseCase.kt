package com.ganadeia.app.application

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository
import com.ganadeia.app.domain.port.driven.repository.HealthRepository
import com.ganadeia.app.domain.service.FollowUpService

/**
 * CASO DE USO: Programar Seguimiento
 * Permite asignar una fecha manual o sugerida por la IA.
 */
class ScheduleFollowUpUseCase(
    private val animalRepository: AnimalRepository,
    private val healthRepository: HealthRepository
) {
    // Función 1: Para obtener la sugerencia y mostrarla en la UI
    suspend fun getRecommendedDate(animal: Animal): Long {
        val lastRecord = healthRepository.getLastRecordForAnimal(animal.id)
        return FollowUpService.calculateNextFollowUp(animal, lastRecord)
    }

    // Función 2: Para guardar la fecha que el usuario finalmente decidió
    suspend fun confirmSchedule(animalId: String, selectedDate: Long): Boolean {
        return animalRepository.updateFollowUpDate(animalId, selectedDate)
    }
}