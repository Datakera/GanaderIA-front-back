package com.ganadeia.app.domain.service

import com.ganadeia.app.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class VaccinationServiceTest {

    private fun createAnimal(type: AnimalType, birthDate: Long): Animal {
        return Animal(
            id = "a1", userId = "u1", name = "Test",
            type = type, breed = "Mix", hardiness = BreedHardiness.MEDIUM,
            currentWeight = 100.0, birthDate = birthDate,
            purpose = AnimalPurpose.MEAT, status = AnimalStatus.ACTIVE,
            nextFollowUpDate = null
        )
    }

    @Test
    fun `bovine plan should have 3 specific vaccines`() {
        val animal = createAnimal(AnimalType.BOVINE, System.currentTimeMillis())

        // Usando tu nombre exacto: generateInitialVaccinationPlan
        val plan = VaccinationService.generateInitialVaccinationPlan(animal)

        assertEquals(3, plan.size)
        // Usando tu campo exacto: vaccineName
        val names = plan.map { it.vaccineName }
        assertTrue(names.contains("Fiebre Aftosa (Ciclo I)"))
        assertTrue(names.contains("Brucelosis B19"))
        assertTrue(names.contains("Rabia Silvestre"))
    }

    @Test
    fun `vaccine date should be exactly 4 months after birth for Aftosa`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 10) // Nace el 10 de enero
        }
        val animal = createAnimal(AnimalType.BOVINE, cal.timeInMillis)

        val plan = VaccinationService.generateInitialVaccinationPlan(animal)
        val aftosa = plan.find { it.vaccineName.contains("Aftosa") }

        val resultCal = Calendar.getInstance().apply { timeInMillis = aftosa!!.scheduledDate }

        // Si nace en enero (0), +4 meses = Mayo (4)
        assertEquals(Calendar.MAY, resultCal.get(Calendar.MONTH))
        assertEquals(10, resultCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `swine plan should only have Classical Swine Fever`() {
        val animal = createAnimal(AnimalType.SWINE, System.currentTimeMillis())
        val plan = VaccinationService.generateInitialVaccinationPlan(animal)

        assertEquals(1, plan.size)
        assertEquals("Peste Porcina Clásica", plan[0].vaccineName)
    }

    @Test
    fun `poultry plan should have Newcastle and Gumboro at 1 month`() {
        val animal = createAnimal(AnimalType.POULTRY, System.currentTimeMillis())
        val plan = VaccinationService.generateInitialVaccinationPlan(animal)

        assertEquals(2, plan.size)
        assertTrue(plan.all { it.vaccineName == "Newcastle" || it.vaccineName == "Gumboro" })
    }

    @Test
    fun `initial status must be PENDING and dates must be null`() {
        val animal = createAnimal(AnimalType.EQUINE, System.currentTimeMillis())
        val plan = VaccinationService.generateInitialVaccinationPlan(animal)

        assertTrue(plan.all { it.status == VaccineStatus.PENDING })
        assertTrue(plan.all { it.appliedDate == null })
        assertTrue(plan.all { it.doseMl == null })
    }
}