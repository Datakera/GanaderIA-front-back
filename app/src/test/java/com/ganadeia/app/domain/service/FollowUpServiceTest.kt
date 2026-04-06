package com.ganadeia.app.domain.service

import com.ganadeia.app.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class FollowUpServiceTest {

    // Helper para crear un animal base rápidamente
    private fun createBaseAnimal(
        ageInMonths: Int = 25, // Adulto por defecto
        hardiness: BreedHardiness = BreedHardiness.MEDIUM,
        purpose: AnimalPurpose = AnimalPurpose.MEAT
    ): Animal {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -ageInMonths)

        return Animal(
            id = "test-1", userId = "u1", name = "Test",
            type = AnimalType.BOVINE, breed = "Mix",
            hardiness = hardiness, currentWeight = 300.0,
            birthDate = cal.timeInMillis,
            purpose = purpose, status = AnimalStatus.ACTIVE,
            nextFollowUpDate = null
        )
    }

    // Helper para calcular la diferencia de días entre hoy y el resultado
    private fun getDaysDifference(resultTimestamp: Long): Long {
        // Usamos una fecha fija para "hoy" al inicio del cálculo para evitar que el tiempo corra
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val target = Calendar.getInstance().apply {
            timeInMillis = resultTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val diff = target - today
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    @Test
    fun `should recommend 3 days if animal has symptoms (Critical Health)`() {
        // Given: Una vaca adulta sana (debería tener 45 días) pero con fiebre
        val animal = createBaseAnimal()
        val record = HealthRecord(
            id = "r1", animalId = "test-1", date = System.currentTimeMillis(),
            symptoms = listOf(Symptom.FEVER), // <--- Alerta roja
            recordedWeight = 300.0, bodyCondition = 3, aiRecommendation = null
        )

        // When
        val result = FollowUpService.calculateNextFollowUp(animal, record)

        // Then: La salud manda sobre todos. 3 días.
        assertEquals(3L, getDaysDifference(result))
    }

    @Test
    fun `should recommend 10 days for calves (Age Factor)`() {
        // Given: Un ternero de 3 meses, raza resistente, sin records previos
        val animal = createBaseAnimal(ageInMonths = 3, hardiness = BreedHardiness.HIGH)

        // When
        val result = FollowUpService.calculateNextFollowUp(animal, null)

        // Then: Base 10 + 5 (High Hardiness) = 15 días
        assertEquals(15L, getDaysDifference(result))
    }

    @Test
    fun `should reduce days for low hardiness breeds like Holstein`() {
        // Given: Una vaca adulta (base 45) pero de raza delicada (LOW hardiness)
        val animal = createBaseAnimal(ageInMonths = 30, hardiness = BreedHardiness.LOW)

        // When
        val result = FollowUpService.calculateNextFollowUp(animal, null)

        // Then: 45 (Adulto) - 5 (Baja resistencia) = 40 días
        assertEquals(40L, getDaysDifference(result))
    }

    @Test
    fun `should cap follow up to 15 days for Milk purpose`() {
        // Given: Vaca adulta sana (45 días) pero dedicada a LECHE
        val animal = createBaseAnimal(ageInMonths = 40, purpose = AnimalPurpose.MILK)

        // When
        val result = FollowUpService.calculateNextFollowUp(animal, null)

        // Then: El ciclo de leche es estricto. Max 15 días.
        assertEquals(15L, getDaysDifference(result))
    }

    @Test
    fun `should accelerate follow up if body condition is poor (BCS less than 3)`() {
        // Given: Vaca joven (30 días base) con condición corporal 2 (flaca)
        val animal = createBaseAnimal(ageInMonths = 12, hardiness = BreedHardiness.MEDIUM)
        val record = HealthRecord(
            id = "r1", animalId = "test-1", date = System.currentTimeMillis(),
            symptoms = listOf(Symptom.NONE),
            recordedWeight = 200.0, bodyCondition = 2, // <--- Flaca
            aiRecommendation = null
        )

        // When
        val result = FollowUpService.calculateNextFollowUp(animal, record)

        // Then: 30 (Joven) - 7 (Ajuste por nutrición) = 23 días
        assertEquals(23L, getDaysDifference(result))
    }

    @Test
    fun `should combine multiple factors correctly`() {
        // Escenario complejo:
        // Adulto (45) + Raza Delicada (-5) + Flaca (-7) = 33 días
        val animal = createBaseAnimal(ageInMonths = 36, hardiness = BreedHardiness.LOW)
        val record = HealthRecord(
            id = "r1", animalId = "test-1", date = System.currentTimeMillis(),
            symptoms = listOf(Symptom.NONE),
            recordedWeight = 350.0, bodyCondition = 2,
            aiRecommendation = null
        )

        val result = FollowUpService.calculateNextFollowUp(animal, record)

        assertEquals(33L, getDaysDifference(result))
    }
}