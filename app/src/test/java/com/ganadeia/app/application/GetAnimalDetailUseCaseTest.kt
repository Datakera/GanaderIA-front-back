package com.ganadeia.app.application

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.AnimalPurpose
import com.ganadeia.app.domain.model.AnimalStatus
import com.ganadeia.app.domain.model.AnimalType
import com.ganadeia.app.domain.model.BreedHardiness
import com.ganadeia.app.domain.model.HealthRecord
import com.ganadeia.app.domain.model.SyncStatus
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.domain.model.VaccinationRecord
import com.ganadeia.app.domain.model.VaccineStatus
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository
import com.ganadeia.app.domain.port.driven.repository.HealthCheckRepository
import com.ganadeia.app.domain.port.driven.repository.VaccinationRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class GetAnimalDetailUseCaseTest {

    private lateinit var animalRepository: AnimalRepository
    private lateinit var healthCheckRepository: HealthCheckRepository
    private lateinit var vaccinationRepository: VaccinationRepository
    private lateinit var useCase: GetAnimalDetailUseCase

    private val mockOwner = User(
        id = "owner-1", name = "Cristian", email = "c@test.com",
        role = UserRole.RANCHER, ranchName = "La Finca",
        location = null, permissions = null, createdAt = 0, updatedAt = 0
    )

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

    private val sampleHealthRecord = HealthRecord(
        id = "HR-1", animalId = "A-001", date = 2_000_000L,
        weightKg = 300.0, bodyConditionScore = 3,
        symptoms = emptySet(), notes = null,
        aiRecommendation = null, syncStatus = SyncStatus.SYNCED,
        confirmedFollowUpDate = null
    )

    private val sampleVaccinationRecord = VaccinationRecord(
        id = "VAC-1", animalId = "A-001",
        vaccineName = "Fiebre Aftosa (Ciclo I)",
        scheduledDate = 3_000_000L,
        appliedDate = null,
        status = VaccineStatus.PENDING,
        doseMl = null
    )

    @Before
    fun setUp() {
        animalRepository = mock(AnimalRepository::class.java)
        healthCheckRepository = mock(HealthCheckRepository::class.java)
        vaccinationRepository = mock(VaccinationRepository::class.java)
        useCase = GetAnimalDetailUseCase(animalRepository, healthCheckRepository, vaccinationRepository)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-1: Acceso restringido al dueño
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA1 - should fail when user is ADMIN`() = runBlocking {
        val admin = mockOwner.copy(role = UserRole.ADMIN)

        val result = useCase.execute(admin, "A-001")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("ganaderos") == true)
    }

    @Test
    fun `CA1 - should fail when animal does not belong to owner`() = runBlocking {
        whenever(animalRepository.getAnimalsByOwner(any())).thenReturn(emptyList())

        val result = useCase.execute(mockOwner, "A-001")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("A-001") == true)
    }

    @Test
    fun `CA1 - should fail when animalId is blank`() = runBlocking {
        val result = useCase.execute(mockOwner, "   ")

        assertTrue(result.isFailure)
        assertEquals("El ID del animal no puede estar vacío.", result.exceptionOrNull()?.message)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-2: Datos consolidados
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA2 - should return consolidated animal detail on success`() = runBlocking {
        givenAnimalExists()
        givenHealthHistoryExists(listOf(sampleHealthRecord))
        givenVaccinationsExist(listOf(sampleVaccinationRecord))

        val result = useCase.execute(mockOwner, "A-001")

        assertTrue(result.isSuccess)
        val detail = result.getOrNull()!!
        assertEquals(existingAnimal, detail.animal)
        assertEquals(1, detail.healthHistory.size)
        assertEquals(1, detail.vaccinationHistory.size)
    }

    @Test
    fun `CA2 - lastHealthCheck should be the first element of healthHistory`() = runBlocking {
        val newerRecord = sampleHealthRecord.copy(id = "HR-2", date = 5_000_000L)
        val olderRecord = sampleHealthRecord.copy(id = "HR-1", date = 2_000_000L)

        givenAnimalExists()
        // El DAO devuelve del más reciente al más antiguo
        givenHealthHistoryExists(listOf(newerRecord, olderRecord))
        givenVaccinationsExist(emptyList())

        val result = useCase.execute(mockOwner, "A-001")

        val detail = result.getOrNull()!!
        assertEquals("HR-2", detail.lastHealthCheck?.id)
    }

    @Test
    fun `CA2 - lastHealthCheck should be null when animal has no health records`() = runBlocking {
        givenAnimalExists()
        givenHealthHistoryExists(emptyList())
        givenVaccinationsExist(emptyList())

        val result = useCase.execute(mockOwner, "A-001")

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull()!!.lastHealthCheck)
    }

    @Test
    fun `CA2 - should return empty lists when animal has no history`() = runBlocking {
        givenAnimalExists()
        givenHealthHistoryExists(emptyList())
        givenVaccinationsExist(emptyList())

        val result = useCase.execute(mockOwner, "A-001")

        assertTrue(result.isSuccess)
        val detail = result.getOrNull()!!
        assertTrue(detail.healthHistory.isEmpty())
        assertTrue(detail.vaccinationHistory.isEmpty())
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun givenAnimalExists() {
        whenever(animalRepository.getAnimalsByOwner(eq("owner-1")))
            .thenReturn(listOf(existingAnimal))
    }

    private suspend fun givenHealthHistoryExists(records: List<HealthRecord>) {
        whenever(healthCheckRepository.getHealthChecksByAnimal(eq("A-001")))
            .thenReturn(records)
    }

    private suspend fun givenVaccinationsExist(records: List<VaccinationRecord>) {
        whenever(vaccinationRepository.getVaccinationsByAnimal(eq("A-001")))
            .thenReturn(records)
    }
}