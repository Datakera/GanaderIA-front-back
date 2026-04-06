package com.ganadeia.app.data.repository

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.AnimalPurpose
import com.ganadeia.app.domain.model.AnimalStatus
import com.ganadeia.app.domain.model.AnimalType
import com.ganadeia.app.domain.model.BreedHardiness
import com.ganadeia.app.infrastructure.persistence.room.dao.AnimalDao
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.junit.Assert.assertTrue

class RoomAnimalRepositoryTest {

    private val animalDao = mock(AnimalDao::class.java)
    private val repository = RoomAnimalRepository(animalDao)

    @Test
    fun `addAnimal should convert domain model to entity and call dao`() = runBlocking {
        // Given: Datos completos del dominio
        val animal = Animal(
            id = "1",
            userId = "owner-123",
            name = "Lola",
            type = AnimalType.BOVINE,
            breed = "Jersey",
            hardiness = BreedHardiness.LOW,
            currentWeight = 200.0,
            birthDate = 12345L,
            purpose = AnimalPurpose.MEAT,
            status = AnimalStatus.ACTIVE,
            nextFollowUpDate = null
        )

        // When
        val result = repository.addAnimal("owner-123", animal)

        // Then
        assertTrue(result)
        // Verificamos que el DAO recibió una entidad
        verify(animalDao).insertAnimal(any())
    }
}