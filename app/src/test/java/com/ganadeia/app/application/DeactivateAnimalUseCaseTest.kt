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

class DeactivateAnimalUseCaseTest {

    private lateinit var animalRepository: AnimalRepository
    private lateinit var useCase: DeactivateAnimalUseCase

    private val mockOwner = User(
        id = "owner-1", name = "Cristian", email = "c@test.com",
        role = UserRole.RANCHER, ranchName = "La Finca",
        location = null, permissions = null, createdAt = 0, updatedAt = 0
    )

    private val activeAnimal = Animal(
        id = "A-001",
        userId = "owner-1",
        name = "Lola",
        type = AnimalType.BOVINE,
        breed = "Holstein",
        hardiness = BreedHardiness.LOW,
        currentWeight = 300.0,
        birthDate = 1_000_000L,
        purpose = AnimalPurpose.MEAT,
        status = AnimalStatus.ACTIVE,
        nextFollowUpDate = 9_999_999L
    )

    @Before
    fun setUp() {
        animalRepository = mock(AnimalRepository::class.java)
        useCase = DeactivateAnimalUseCase(animalRepository)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-1: Solo el dueño puede desactivar
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA1 - should fail when user is ADMIN`() = runBlocking {
        val admin = mockOwner.copy(role = UserRole.ADMIN)

        val result = useCase.execute(
            DeactivateAnimalRequest(owner = admin, animalId = "A-001", reason = DeactivationReason.SOLD)
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("ganaderos") == true)
    }

    @Test
    fun `CA1 - should fail when animal does not belong to owner`() = runBlocking {
        whenever(animalRepository.getAnimalsByOwner(any())).thenReturn(emptyList())

        val result = useCase.execute(
            DeactivateAnimalRequest(owner = mockOwner, animalId = "A-999", reason = DeactivationReason.SOLD)
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("A-999") == true)
    }

    @Test
    fun `CA1 - should fail when animalId is blank`() = runBlocking {
        val result = useCase.execute(
            DeactivateAnimalRequest(owner = mockOwner, animalId = "  ", reason = DeactivationReason.SOLD)
        )
        assertTrue(result.isFailure)
        assertEquals("El ID del animal no puede estar vacío.", result.exceptionOrNull()?.message)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-2: Razón determina el nuevo status
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA2 - SOLD reason should set status to SOLD`() = runBlocking {
        givenAnimalExists()
        whenever(animalRepository.addAnimal(any(), any())).thenReturn(true)

        val result = useCase.execute(
            DeactivateAnimalRequest(owner = mockOwner, animalId = "A-001", reason = DeactivationReason.SOLD)
        )

        assertTrue(result.isSuccess)
        assertEquals(AnimalStatus.SOLD, result.getOrNull()!!.status)
    }

    @Test
    fun `CA2 - DECEASED reason should set status to INACTIVE`() = runBlocking {
        givenAnimalExists()
        whenever(animalRepository.addAnimal(any(), any())).thenReturn(true)

        val result = useCase.execute(
            DeactivateAnimalRequest(owner = mockOwner, animalId = "A-001", reason = DeactivationReason.DECEASED)
        )

        assertTrue(result.isSuccess)
        assertEquals(AnimalStatus.INACTIVE, result.getOrNull()!!.status)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-3: No se puede desactivar un animal ya inactivo
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA3 - should fail when animal is already SOLD`() = runBlocking {
        val soldAnimal = activeAnimal.copy(status = AnimalStatus.SOLD)
        whenever(animalRepository.getAnimalsByOwner(eq("owner-1"))).thenReturn(listOf(soldAnimal))

        val result = useCase.execute(
            DeactivateAnimalRequest(owner = mockOwner, animalId = "A-001", reason = DeactivationReason.SOLD)
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("SOLD") == true)
    }

    @Test
    fun `CA3 - should fail when animal is already INACTIVE`() = runBlocking {
        val inactiveAnimal = activeAnimal.copy(status = AnimalStatus.INACTIVE)
        whenever(animalRepository.getAnimalsByOwner(eq("owner-1"))).thenReturn(listOf(inactiveAnimal))

        val result = useCase.execute(
            DeactivateAnimalRequest(owner = mockOwner, animalId = "A-001", reason = DeactivationReason.DECEASED)
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("INACTIVE") == true)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-4: El historial se conserva (no se borra nada)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA4 - deactivated animal should preserve all data except status and followUpDate`() = runBlocking {
        givenAnimalExists()
        whenever(animalRepository.addAnimal(any(), any())).thenReturn(true)

        val result = useCase.execute(
            DeactivateAnimalRequest(owner = mockOwner, animalId = "A-001", reason = DeactivationReason.SOLD)
        )

        val deactivated = result.getOrNull()!!
        // Datos preservados
        assertEquals("A-001", deactivated.id)
        assertEquals("Lola", deactivated.name)
        assertEquals(AnimalType.BOVINE, deactivated.type)
        assertEquals("Holstein", deactivated.breed)
        assertEquals(300.0, deactivated.currentWeight, 0.001)
        assertEquals(1_000_000L, deactivated.birthDate)
        // nextFollowUpDate debe quedar null — ya no necesita seguimiento
        assertNull(deactivated.nextFollowUpDate)
        // Status actualizado
        assertEquals(AnimalStatus.SOLD, deactivated.status)
    }

    @Test
    fun `CA4 - should return failure when repository save fails`() = runBlocking {
        givenAnimalExists()
        whenever(animalRepository.addAnimal(any(), any())).thenReturn(false)

        val result = useCase.execute(
            DeactivateAnimalRequest(owner = mockOwner, animalId = "A-001", reason = DeactivationReason.SOLD)
        )
        assertTrue(result.isFailure)
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private suspend fun givenAnimalExists() {
        whenever(animalRepository.getAnimalsByOwner(eq("owner-1")))
            .thenReturn(listOf(activeAnimal))
    }
}