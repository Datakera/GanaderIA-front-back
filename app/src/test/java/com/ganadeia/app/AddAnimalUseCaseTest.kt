package com.ganadeia.app

import com.ganadeia.app.application.AddAnimalUseCase
import com.ganadeia.app.domain.model.AnimalPurpose
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

class AddAnimalUseCaseTest {

    private val animalRepository = mock(AnimalRepository::class.java)
    private val addAnimalUseCase = AddAnimalUseCase(animalRepository)

    // Helper para no repetir la creación del usuario en cada test
    private val mockUser = User(
        id = "123", name = "Cristian", email = "c@test.com",
        role = UserRole.RANCHER, ranchName = "La Finca",
        location = null, permissions = null, createdAt = 0, updatedAt = 0
    )

    @Test
    fun `should return failure when animal weight is zero or negative`() = runBlocking {
        // When: Pasamos un peso de -5.0 y completamos los nuevos parámetros
        val result = addAnimalUseCase.execute(
            owner = mockUser,
            animalName = "Lola",
            breed = "Holstein",
            weight = -5.0,
            ageInMonths = 12,
            purpose = AnimalPurpose.MEAT
        )

        // Then
        assertTrue(result.isFailure)
        assertEquals("Animal weight must be greater than zero.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should return failure when age is negative`() = runBlocking {
        // When: Probamos la nueva validación de edad
        val result = addAnimalUseCase.execute(
            owner = mockUser,
            animalName = "Lola",
            breed = "Holstein",
            weight = 100.0,
            ageInMonths = -1, // Edad inválida
            purpose = AnimalPurpose.MEAT
        )

        // Then
        assertTrue(result.isFailure)
        assertEquals("Age cannot be negative", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should return success when all data is valid`() = runBlocking {
        // Given
        `when`(animalRepository.addAnimal(eq("123"), any())).thenReturn(true)

        // When
        val result = addAnimalUseCase.execute(
            owner = mockUser,
            animalName = "Lola",
            breed = "Holstein",
            weight = 250.0,
            ageInMonths = 24,
            purpose = AnimalPurpose.MILK
        )

        // Then
        assertTrue(result.isSuccess)
        val animal = result.getOrNull()
        assertEquals("Lola", animal?.name)
        assertEquals(AnimalPurpose.MILK, animal?.purpose)
        // No verificamos la fecha exacta porque depende del momento del test,
        // pero sabemos que el UseCase usó el DateCalculator internamente.
    }
}