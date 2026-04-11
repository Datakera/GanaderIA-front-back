package com.ganadeia.app.application

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.AnimalPurpose
import com.ganadeia.app.domain.model.AnimalStatus
import com.ganadeia.app.domain.model.AnimalType
import com.ganadeia.app.domain.model.BreedHardiness
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class UpdateAnimalUseCaseTest {

    private lateinit var animalRepository: AnimalRepository
    private lateinit var useCase: UpdateAnimalUseCase

    private val mockOwner = User(
        id = "owner-1", name = "Cristian", email = "c@test.com",
        role = UserRole.RANCHER, ranchName = "La Finca",
        location = null, permissions = null, createdAt = 0, updatedAt = 0
    )

    private val mockAdmin = mockOwner.copy(role = UserRole.ADMIN)

    private val existingAnimal = Animal(
        id = "A-001",
        userId = "owner-1",
        name = "Lola",
        type = AnimalType.BOVINE,
        breed = "Holstein",
        hardiness = BreedHardiness.LOW,
        currentWeight = 300.0,
        birthDate = 1_000_000L,
        purpose = AnimalPurpose.MILK,
        status = AnimalStatus.ACTIVE,
        nextFollowUpDate = null
    )

    @Before
    fun setUp() {
        animalRepository = mock(AnimalRepository::class.java)
        useCase = UpdateAnimalUseCase(animalRepository)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-1: Solo el dueño puede actualizar
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA1 - should fail when user is not a RANCHER`() = runBlocking {
        val result = useCase.execute(
            UpdateAnimalRequest(owner = mockAdmin, animalId = "A-001", newWeight = 350.0)
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("ganaderos") == true)
    }

    @Test
    fun `CA1 - should fail when animal does not belong to owner`() = runBlocking {
        whenever(animalRepository.getAnimalsByOwner(eq("owner-1"))).thenReturn(emptyList())

        val result = useCase.execute(
            UpdateAnimalRequest(owner = mockOwner, animalId = "A-999", newWeight = 350.0)
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("A-999") == true)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-2: El peso debe ser válido
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA2 - should fail when new weight is zero`() = runBlocking {
        givenAnimalExists()

        val result = useCase.execute(
            UpdateAnimalRequest(owner = mockOwner, animalId = "A-001", newWeight = 0.0)
        )
        assertTrue(result.isFailure)
        assertEquals("El peso del animal debe ser mayor a cero.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `CA2 - should fail when new weight is negative`() = runBlocking {
        givenAnimalExists()

        val result = useCase.execute(
            UpdateAnimalRequest(owner = mockOwner, animalId = "A-001", newWeight = -50.0)
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `CA2 - should fail when new weight exceeds bovine limit`() = runBlocking {
        givenAnimalExists()

        val result = useCase.execute(
            UpdateAnimalRequest(owner = mockOwner, animalId = "A-001", newWeight = 2000.0)
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("inusualmente alto") == true)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-3: Campos inmutables se preservan
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA3 - should preserve immutable fields after update`() = runBlocking {
        givenAnimalExists()
        whenever(animalRepository.addAnimal(any(), any())).thenReturn(true)

        val result = useCase.execute(
            UpdateAnimalRequest(owner = mockOwner, animalId = "A-001", newWeight = 380.0)
        )

        assertTrue(result.isSuccess)
        val updated = result.getOrNull()!!
        // Campos inmutables intactos
        assertEquals("A-001", updated.id)
        assertEquals(AnimalType.BOVINE, updated.type)
        assertEquals("Holstein", updated.breed)
        assertEquals(BreedHardiness.LOW, updated.hardiness)
        assertEquals(1_000_000L, updated.birthDate)
        // Campo actualizado
        assertEquals(380.0, updated.currentWeight, 0.001)
    }

    @Test
    fun `CA3 - should only update provided fields, keep rest unchanged`() = runBlocking {
        givenAnimalExists()
        whenever(animalRepository.addAnimal(any(), any())).thenReturn(true)

        // Solo actualizamos el peso, lo demás debe quedar igual
        val result = useCase.execute(
            UpdateAnimalRequest(owner = mockOwner, animalId = "A-001", newWeight = 350.0)
        )

        val updated = result.getOrNull()!!
        assertEquals("Lola", updated.name)          // nombre intacto
        assertEquals(AnimalPurpose.MILK, updated.purpose) // propósito intacto
        assertEquals(AnimalStatus.ACTIVE, updated.status) // status intacto
        assertEquals(350.0, updated.currentWeight, 0.001) // solo peso cambió
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Actualización de múltiples campos
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `should update multiple fields at once`() = runBlocking {
        givenAnimalExists()
        whenever(animalRepository.addAnimal(any(), any())).thenReturn(true)

        val result = useCase.execute(
            UpdateAnimalRequest(
                owner = mockOwner,
                animalId = "A-001",
                newName = "Lola II",
                newWeight = 400.0,
                newPurpose = AnimalPurpose.DUAL_PURPOSE
            )
        )

        assertTrue(result.isSuccess)
        val updated = result.getOrNull()!!
        assertEquals("Lola II", updated.name)
        assertEquals(400.0, updated.currentWeight, 0.001)
        assertEquals(AnimalPurpose.DUAL_PURPOSE, updated.purpose)
    }

    @Test
    fun `should fail when new name is blank`() = runBlocking {
        givenAnimalExists()

        val result = useCase.execute(
            UpdateAnimalRequest(owner = mockOwner, animalId = "A-001", newName = "   ")
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("nombre") == true)
    }

    @Test
    fun `should return failure when repository save fails`() = runBlocking {
        givenAnimalExists()
        whenever(animalRepository.addAnimal(any(), any())).thenReturn(false)

        val result = useCase.execute(
            UpdateAnimalRequest(owner = mockOwner, animalId = "A-001", newWeight = 350.0)
        )
        assertTrue(result.isFailure)
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private suspend fun givenAnimalExists() {
        whenever(animalRepository.getAnimalsByOwner(eq("owner-1")))
            .thenReturn(listOf(existingAnimal))
    }
}