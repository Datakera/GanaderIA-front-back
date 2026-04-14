package com.ganadeia.app.application

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.AiRecommendationRecord
import com.ganadeia.app.domain.model.AiRecommendationResponse
import com.ganadeia.app.domain.model.AiRecommendationStatus
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
import com.ganadeia.app.domain.port.driven.repository.AiRecommendationRepository
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository
import com.ganadeia.app.domain.port.driven.repository.HealthCheckRepository
import com.ganadeia.app.domain.port.driven.repository.VaccinationRepository
import com.ganadeia.app.domain.port.driven.service.AiServicePort
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class GetAiRecommendationUseCaseTest {

    private lateinit var animalRepository: AnimalRepository
    private lateinit var healthCheckRepository: HealthCheckRepository
    private lateinit var vaccinationRepository: VaccinationRepository
    private lateinit var aiRecommendationRepository: AiRecommendationRepository
    private lateinit var aiServicePort: AiServicePort
    private lateinit var useCase: GetAiRecommendationUseCase

    private val fixedNow = 1_700_000_000_000L

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
        birthDate = fixedNow - (730L * 24 * 60 * 60 * 1000), // ~24 meses
        purpose = AnimalPurpose.MILK,
        status = AnimalStatus.ACTIVE,
        nextFollowUpDate = null
    )

    private val sampleHealthRecord = HealthRecord(
        id = "HR-1", animalId = "A-001",
        date = fixedNow - (7L * 24 * 60 * 60 * 1000),
        weightKg = 295.0, bodyConditionScore = 3,
        symptoms = emptySet(), notes = null,
        aiRecommendation = null, syncStatus = SyncStatus.SYNCED,
        confirmedFollowUpDate = null
    )

    private val sampleVaccination = VaccinationRecord(
        id = "VAC-1", animalId = "A-001",
        vaccineName = "Fiebre Aftosa (Ciclo I)",
        scheduledDate = fixedNow + (30L * 24 * 60 * 60 * 1000),
        appliedDate = null,
        status = VaccineStatus.PENDING,
        doseMl = null
    )

    private val mockApiResponse = AiRecommendationResponse(
        animalId                  = "A-001",
        generatedAt               = fixedNow,
        generalDiagnosis          = "El animal presenta condición corporal normal.",
        priorityAction            = "Continuar esquema de vacunación.",
        nutritionalRecommendation = "Mantener pastoreo rotacional.",
        confidenceScore           = 0.91f
    )

    @Before
    fun setUp() {
        animalRepository          = mock(AnimalRepository::class.java)
        healthCheckRepository     = mock(HealthCheckRepository::class.java)
        vaccinationRepository     = mock(VaccinationRepository::class.java)
        aiRecommendationRepository = mock(AiRecommendationRepository::class.java)
        aiServicePort             = mock(AiServicePort::class.java)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-4: Restricción de acceso
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA4 - should fail when user is ADMIN`() = runBlocking {
        val admin = mockOwner.copy(role = UserRole.ADMIN)
        val useCase = buildUseCase(networkAvailable = true)

        val result = useCase.execute(admin, "A-001", fixedNow)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("ganaderos") == true)
    }

    @Test
    fun `CA4 - should fail when animal does not belong to owner`() = runBlocking {
        whenever(animalRepository.getAnimalsByOwner(any())).thenReturn(emptyList())
        val useCase = buildUseCase(networkAvailable = true)

        val result = useCase.execute(mockOwner, "A-001", fixedNow)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("A-001") == true)
    }

    @Test
    fun `CA4 - should fail when animalId is blank`() = runBlocking {
        val useCase = buildUseCase(networkAvailable = true)

        val result = useCase.execute(mockOwner, "   ", fixedNow)

        assertTrue(result.isFailure)
        assertEquals("El ID del animal no puede estar vacío.", result.exceptionOrNull()?.message)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-2: Offline-first — se guarda PENDING antes de llamar a la API
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA2 - should save PENDING record before calling the API`() = runBlocking {
        givenFullSetupReady()
        whenever(aiServicePort.requestRecommendation(any()))
            .thenReturn(Result.success(mockApiResponse))
        val useCase = buildUseCase(networkAvailable = true)

        useCase.execute(mockOwner, "A-001", fixedNow)

        // Verificar que saveRecommendation fue llamado con status PENDING
        val captor = argumentCaptor<AiRecommendationRecord>()
        verify(aiRecommendationRepository).saveRecommendation(captor.capture())
        assertEquals(AiRecommendationStatus.PENDING, captor.firstValue.status)
        assertEquals("A-001", captor.firstValue.animalId)
    }

    @Test
    fun `CA2 - should return PENDING result when network is unavailable`(): Unit = runBlocking {
        givenFullSetupReady()
        val useCase = buildUseCase(networkAvailable = false)

        val result = useCase.execute(mockOwner, "A-001", fixedNow)

        assertTrue(result.isSuccess)
        val outcome = result.getOrNull()!!
        assertEquals(AiRecommendationStatus.PENDING, outcome.record.status)
        assertFalse(outcome.isImmediate)
        // La API nunca debe ser llamada si no hay red
        verify(aiServicePort, never()).requestRecommendation(any())
    }

    @Test
    fun `CA2 - should fail gracefully when local DB save fails`(): Unit = runBlocking {
        givenFullSetupReady()
        whenever(aiRecommendationRepository.saveRecommendation(any())).thenReturn(false)
        val useCase = buildUseCase(networkAvailable = true)

        val result = useCase.execute(mockOwner, "A-001", fixedNow)

        assertTrue(result.isFailure)
        verify(aiServicePort, never()).requestRecommendation(any())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-3: Actualización del registro con la respuesta de la API
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA3 - should update record to COMPLETED when API responds successfully`() = runBlocking {
        givenFullSetupReady()
        whenever(aiServicePort.requestRecommendation(any()))
            .thenReturn(Result.success(mockApiResponse))
        val useCase = buildUseCase(networkAvailable = true)

        val result = useCase.execute(mockOwner, "A-001", fixedNow)

        assertTrue(result.isSuccess)
        val outcome = result.getOrNull()!!
        assertEquals(AiRecommendationStatus.COMPLETED, outcome.record.status)
        assertTrue(outcome.isImmediate)

        val captor = argumentCaptor<AiRecommendationRecord>()
        verify(aiRecommendationRepository).updateRecommendation(captor.capture())
        val updated = captor.firstValue
        assertEquals(AiRecommendationStatus.COMPLETED, updated.status)
        assertEquals("El animal presenta condición corporal normal.", updated.generalDiagnosis)
        assertEquals("Continuar esquema de vacunación.", updated.priorityAction)
        assertEquals(0.91f, updated.confidenceScore!!, 0.001f)
    }

    @Test
    fun `CA3 - should update record to FAILED when API fails`() = runBlocking {
        givenFullSetupReady()
        whenever(aiServicePort.requestRecommendation(any()))
            .thenReturn(Result.failure(RuntimeException("Timeout")))
        val useCase = buildUseCase(networkAvailable = true)

        val result = useCase.execute(mockOwner, "A-001", fixedNow)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("guardada") == true)

        val captor = argumentCaptor<AiRecommendationRecord>()
        verify(aiRecommendationRepository).updateRecommendation(captor.capture())
        val failed = captor.firstValue
        assertEquals(AiRecommendationStatus.FAILED, failed.status)
        assertEquals(1, failed.retryCount)
        assertEquals("Timeout", failed.lastErrorMessage)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-1: Recopilación correcta de datos
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA1 - should include health history up to 10 records`() = runBlocking {
        // 12 registros → solo deben enviarse 10
        val bigHistory = (1..12).map { i ->
            sampleHealthRecord.copy(id = "HR-$i", date = fixedNow - (i * 1000L))
        }
        whenever(animalRepository.getAnimalsByOwner(any())).thenReturn(listOf(existingAnimal))
        whenever(healthCheckRepository.getHealthChecksByAnimal(any())).thenReturn(bigHistory)
        whenever(vaccinationRepository.getVaccinationsByAnimal(any())).thenReturn(emptyList())
        whenever(aiRecommendationRepository.saveRecommendation(any())).thenReturn(true)

        val requestCaptor = argumentCaptor<com.ganadeia.app.domain.model.AiRecommendationRequest>()
        whenever(aiServicePort.requestRecommendation(requestCaptor.capture()))
            .thenReturn(Result.success(mockApiResponse))
        val useCase = buildUseCase(networkAvailable = true)

        useCase.execute(mockOwner, "A-001", fixedNow)

        assertEquals(10, requestCaptor.firstValue.healthHistory.size)
    }

    @Test
    fun `CA1 - applied and pending vaccines are correctly separated`() = runBlocking {
        val appliedVac = sampleVaccination.copy(
            id = "VAC-APPLIED",
            status = VaccineStatus.APPLIED,
            appliedDate = fixedNow - (10L * 24 * 60 * 60 * 1000)
        )
        val pendingVac = sampleVaccination.copy(id = "VAC-PENDING", status = VaccineStatus.PENDING)

        whenever(animalRepository.getAnimalsByOwner(any())).thenReturn(listOf(existingAnimal))
        whenever(healthCheckRepository.getHealthChecksByAnimal(any())).thenReturn(emptyList())
        whenever(vaccinationRepository.getVaccinationsByAnimal(any()))
            .thenReturn(listOf(appliedVac, pendingVac))
        whenever(aiRecommendationRepository.saveRecommendation(any())).thenReturn(true)

        val requestCaptor = argumentCaptor<com.ganadeia.app.domain.model.AiRecommendationRequest>()
        whenever(aiServicePort.requestRecommendation(requestCaptor.capture()))
            .thenReturn(Result.success(mockApiResponse))
        val useCase = buildUseCase(networkAvailable = true)

        useCase.execute(mockOwner, "A-001", fixedNow)

        val sentRequest = requestCaptor.firstValue
        assertEquals(1, sentRequest.appliedVaccines.size)
        assertEquals(1, sentRequest.pendingVaccines.size)
        assertEquals("Fiebre Aftosa (Ciclo I)", sentRequest.appliedVaccines[0].vaccineName)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildUseCase(networkAvailable: Boolean) = GetAiRecommendationUseCase(
        animalRepository           = animalRepository,
        healthCheckRepository      = healthCheckRepository,
        vaccinationRepository      = vaccinationRepository,
        aiRecommendationRepository = aiRecommendationRepository,
        aiServicePort              = aiServicePort,
        isNetworkAvailable         = { networkAvailable }
    )

    private suspend fun givenFullSetupReady() {
        whenever(animalRepository.getAnimalsByOwner(any())).thenReturn(listOf(existingAnimal))
        whenever(healthCheckRepository.getHealthChecksByAnimal(any()))
            .thenReturn(listOf(sampleHealthRecord))
        whenever(vaccinationRepository.getVaccinationsByAnimal(any()))
            .thenReturn(listOf(sampleVaccination))
        whenever(aiRecommendationRepository.saveRecommendation(any())).thenReturn(true)
        whenever(aiRecommendationRepository.updateRecommendation(any())).thenReturn(true)
    }
}