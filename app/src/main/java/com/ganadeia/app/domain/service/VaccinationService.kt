package com.ganadeia.app.domain.service

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.AnimalType
import com.ganadeia.app.domain.model.VaccinationRecord
import com.ganadeia.app.domain.model.VaccineStatus
import java.util.Calendar

object VaccinationService {

    fun generateInitialVaccinationPlan(animal: Animal): List<VaccinationRecord> {
        return when (animal.type) {
            AnimalType.BOVINE -> generateBovinePlan(animal)
            AnimalType.SWINE -> generateSwinePlan(animal)
            AnimalType.EQUINE -> generateEquinePlan(animal)
            AnimalType.CAPRINE -> generateCaprinePlan(animal)
            AnimalType.POULTRY -> generatePoultryPlan(animal)
            // Si mañana agregas una especie nueva, el compilador te obligará a atenderla aquí
        }
    }

    private fun generateBovinePlan(animal: Animal) = listOf(
        createRecord(animal.id, "Fiebre Aftosa (Ciclo I)", animal.birthDate, 4),
        createRecord(animal.id, "Brucelosis B19", animal.birthDate, 5),
        createRecord(animal.id, "Rabia Silvestre", animal.birthDate, 6)
    )

    private fun generateEquinePlan(animal: Animal) = listOf(
        createRecord(animal.id, "Tétanos", animal.birthDate, 3),
        createRecord(animal.id, "Encefalitis Equina", animal.birthDate, 4)
    )

    private fun generateCaprinePlan(animal: Animal) = listOf(
        createRecord(animal.id, "Clostridiosis", animal.birthDate, 2),
        createRecord(animal.id, "Ectima Contagioso", animal.birthDate, 3)
    )

    private fun generatePoultryPlan(animal: Animal) = listOf(
        createRecord(animal.id, "Newcastle", animal.birthDate, 1),
        createRecord(animal.id, "Gumboro", animal.birthDate, 1)
    )

    private fun generateSwinePlan(animal: Animal) = listOf(
        createRecord(animal.id, "Peste Porcina Clásica", animal.birthDate, 2)
    )

    private fun createRecord(animalId: String, name: String, birthDate: Long, months: Int): VaccinationRecord {
        val cal = Calendar.getInstance().apply { timeInMillis = birthDate }
        cal.add(Calendar.MONTH, months)
        return VaccinationRecord(
            id = java.util.UUID.randomUUID().toString(),
            animalId = animalId,
            vaccineName = name,
            scheduledDate = cal.timeInMillis,
            appliedDate = null,
            status = VaccineStatus.PENDING,
            doseMl = null
        )
    }
}