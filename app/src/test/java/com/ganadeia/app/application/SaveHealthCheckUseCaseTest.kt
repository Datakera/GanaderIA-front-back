package com.ganadeia.app

import com.ganadeia.app.application.SaveHealthCheckUseCase
import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.AnimalPurpose
import com.ganadeia.app.domain.model.AnimalStatus
import com.ganadeia.app.domain.model.AnimalType
import com.ganadeia.app.domain.model.BreedHardiness
import com.ganadeia.app.domain.model.SyncStatus
import com.ganadeia.app.domain.model.VisibleSymptom
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository
import com.ganadeia.app.domain.port.driven.repository.HealthCheckRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class SaveHealthCheckUseCaseTest {

    // ── Dependencias mockeadas ─────────────────────────────────────────────────
    private lateinit var healthCheckRepo: HealthCheckRepository
    private lateinit var animalRepo: AnimalRepository
    private lateinit var useCase: SaveHealthCheckUseCase

    // Animal de prueba que "existe" en la BD
    private val existingAnimal = Animal(
        id = "A-043",
        userId = "001",
        name = "Lucero",
        type = AnimalType.BOVINE,
        breed = "Cebú",
        hardiness = BreedHardiness.LOW,
        currentWeight = 350.0,
        birthDate = 1_000_000L,
        purpose = AnimalPurpose.MEAT,
        status = AnimalStatus.ACTIVE,
        nextFollowUpDate = null
    )

    // Timestamp fijo para que las fechas sean deterministas en los tests
    private val fixedNow = 1_700_000_000_000L   // ~2023-11-14 en epoch ms

    @Before
    fun setUp() {
        healthCheckRepo = mock(HealthCheckRepository::class.java)
        animalRepo = mock(AnimalRepository::class.java)
        useCase = SaveHealthCheckUseCase(healthCheckRepo, animalRepo)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-1: Almacenamiento local
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA1 - should persist record in local DB on execute`(): Unit = runBlocking {
        givenAnimalExists()
        givenSaveSucceeds()

        val result = executeValidUseCase(isNetworkAvailable = false)

        assertTrue(result.isSuccess)
        // Verifica que el DAO fue llamado con algún registro
        verify(healthCheckRepo).saveHealthCheck(any())
    }

    @Test
    fun `CA1 - saved record contains correct weight, symptoms, bodyCondition and date`() =
        runBlocking {
            givenAnimalExists()
            givenSaveSucceeds()

            val symptoms = setOf(VisibleSymptom.COJERA, VisibleSymptom.FIEBRE)
            val result = useCase.execute(
                animalId = "A-043",
                weightKg = 370.0,
                bodyConditionScore = 2,
                symptoms = symptoms,
                isNetworkAvailable = false,
                now = fixedNow
            )

            assertTrue(result.isSuccess)
            val record = result.getOrNull()!!.savedRecord
            assertEquals(370.0, record.weightKg, 0.001)
            assertEquals(2, record.bodyConditionScore)
            assertEquals(symptoms, record.symptoms)
            assertEquals(fixedNow, record.date)
        }

    @Test
    fun `CA1 - should fail gracefully when DB save fails`() = runBlocking {
        givenAnimalExists()
        whenever(healthCheckRepo.saveHealthCheck(any())).thenReturn(false)

        val result = executeValidUseCase(isNetworkAvailable = false)

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("base de datos") == true
        )
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-2: Vinculación rígida al animal
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA2 - should fail when animalId does not exist in the repository`() = runBlocking {
        // Animal NO existe
        whenever(animalRepo.getAnimalsByOwner(any())).thenReturn(emptyList())

        val result = executeValidUseCase(isNetworkAvailable = false)

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("A-043") == true
        )
    }

    @Test
    fun `CA2 - saved record has the correct animalId FK`() = runBlocking {
        givenAnimalExists()
        givenSaveSucceeds()

        val result = executeValidUseCase(isNetworkAvailable = false)

        assertEquals("A-043", result.getOrNull()!!.savedRecord.animalId)
    }

    @Test
    fun `CA2 - should fail when animalId is blank`() = runBlocking {
        val result = useCase.execute(
            animalId = "   ",
            weightKg = 350.0,
            bodyConditionScore = 3,
            symptoms = emptySet(),
            isNetworkAvailable = true
        )
        assertTrue(result.isFailure)
        assertEquals("El ID del animal no puede estar vacío.", result.exceptionOrNull()?.message)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-3: Estado de sincronización
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA3 - record is PENDING_SYNC when network is unavailable`() = runBlocking {
        givenAnimalExists()
        givenSaveSucceeds()

        val result = executeValidUseCase(isNetworkAvailable = false)

        assertEquals(SyncStatus.PENDING_SYNC, result.getOrNull()!!.savedRecord.syncStatus)
    }

    @Test
    fun `CA3 - record is SYNCED when network is available`() = runBlocking {
        givenAnimalExists()
        givenSaveSucceeds()

        val result = executeValidUseCase(isNetworkAvailable = true)

        assertEquals(SyncStatus.SYNCED, result.getOrNull()!!.savedRecord.syncStatus)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-4: Fecha de seguimiento propuesta
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA4 - proposes follow-up in 3 days for critical symptom`() = runBlocking {
        givenAnimalExists()
        givenSaveSucceeds()

        val result = useCase.execute(
            animalId = "A-043",
            weightKg = 350.0,
            bodyConditionScore = 3,
            symptoms = setOf(VisibleSymptom.DIFICULTAD_RESPIRATORIA), // crítico
            isNetworkAvailable = false,
            now = fixedNow
        )

        assertTrue(result.isSuccess)
        val proposed = result.getOrNull()!!.proposedFollowUpDate
        val diffDays = (proposed - fixedNow) / (1000 * 60 * 60 * 24)
        assertEquals(3L, diffDays)
    }

    @Test
    fun `CA4 - proposes follow-up in 7 days for moderate symptom`() = runBlocking {
        givenAnimalExists()
        givenSaveSucceeds()

        val result = useCase.execute(
            animalId = "A-043",
            weightKg = 350.0,
            bodyConditionScore = 3,
            symptoms = setOf(VisibleSymptom.FIEBRE),  // moderado
            isNetworkAvailable = false,
            now = fixedNow
        )

        val proposed = result.getOrNull()!!.proposedFollowUpDate
        val diffDays = (proposed - fixedNow) / (1000 * 60 * 60 * 24)
        assertEquals(7L, diffDays)
    }

    @Test
    fun `CA4 - proposes follow-up in 30 days when no symptoms and good condition`() = runBlocking {
        givenAnimalExists()
        givenSaveSucceeds()

        val result = useCase.execute(
            animalId = "A-043",
            weightKg = 350.0,
            bodyConditionScore = 4,
            symptoms = emptySet(),
            isNetworkAvailable = false,
            now = fixedNow
        )

        val proposed = result.getOrNull()!!.proposedFollowUpDate
        val diffDays = (proposed - fixedNow) / (1000 * 60 * 60 * 24)
        assertEquals(40L, diffDays)
    }

    @Test
    fun `CA4 - confirmedFollowUpDate is null until ganadero confirms`() = runBlocking {
        givenAnimalExists()
        givenSaveSucceeds()

        val result = executeValidUseCase(isNetworkAvailable = false)

        assertNull(result.getOrNull()!!.savedRecord.confirmedFollowUpDate)
    }

    @Test
    fun `CA4 - confirmFollowUpDate updates animal nextFollowUpDate`(): Unit = runBlocking {
        givenAnimalExists()
        whenever(animalRepo.addAnimal(any(), any())).thenReturn(true)

        val confirmedDate = fixedNow + (15L * 24 * 60 * 60 * 1000)
        val result = useCase.confirmFollowUpDate(
            recordId = "record-1",
            animalId = "A-043",
            confirmedDate = confirmedDate
        )

        assertTrue(result.isSuccess)
        // Verificar que el animal fue actualizado con la nueva fecha
        verify(animalRepo).addAnimal(eq("A-043"), any())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Validaciones de entrada generales
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `should fail when weight is zero`() = runBlocking {
        val result = useCase.execute(
            animalId = "A-043", weightKg = 0.0, bodyConditionScore = 3,
            symptoms = emptySet(), isNetworkAvailable = false
        )
        assertTrue(result.isFailure)
        assertEquals("El peso debe ser mayor a cero.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should fail when bodyConditionScore is out of range`() = runBlocking {
        val result = useCase.execute(
            animalId = "A-043", weightKg = 300.0, bodyConditionScore = 6,
            symptoms = emptySet(), isNetworkAvailable = false
        )
        assertTrue(result.isFailure)
        assertEquals(
            "La condición corporal debe estar entre 1 y 5.",
            result.exceptionOrNull()?.message
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun givenAnimalExists() {
        whenever(animalRepo.getAnimalsByOwner(any()))
            .thenReturn(listOf(existingAnimal))
    }

    private suspend fun givenSaveSucceeds() {
        whenever(healthCheckRepo.saveHealthCheck(any())).thenReturn(true)
    }

    private suspend fun executeValidUseCase(isNetworkAvailable: Boolean) =
        useCase.execute(
            animalId = "A-043",
            weightKg = 350.0,
            bodyConditionScore = 3,
            symptoms = setOf(VisibleSymptom.DECAIMIENTO),
            isNetworkAvailable = isNetworkAvailable,
            now = fixedNow
        )
}