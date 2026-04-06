package com.ganadeia.app.application

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.AnimalPurpose
import com.ganadeia.app.domain.model.AnimalStatus
import com.ganadeia.app.domain.model.AnimalType
import com.ganadeia.app.domain.model.BreedHardiness
import com.ganadeia.app.domain.model.ProcessedVaccinationInput
import com.ganadeia.app.domain.model.VaccineStatus
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository
import com.ganadeia.app.domain.port.driven.repository.VaccinationRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever

class ProcessVaccinationPlanUseCaseTest {

    private lateinit var animalRepository: AnimalRepository
    private lateinit var vaccinationRepository: VaccinationRepository
    private lateinit var useCase: ProcessVaccinationPlanUseCase

    private val existingAnimal = Animal(
        id = "A-042",
        userId = "owner-1",
        name = "Lola",
        type = AnimalType.BOVINE,
        breed = "Holstein",
        hardiness = BreedHardiness.LOW,
        currentWeight = 350.0,
        birthDate = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000 * 2), // ~2 años
        purpose = AnimalPurpose.MILK,
        status = AnimalStatus.ACTIVE,
        nextFollowUpDate = null
    )

    private val futureDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
    private val pastDate = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)

    @Before
    fun setUp() {
        animalRepository = mock(AnimalRepository::class.java)
        vaccinationRepository = mock(VaccinationRepository::class.java)
        useCase = ProcessVaccinationPlanUseCase(animalRepository, vaccinationRepository)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-1: Provisión de Catálogo Dinámico
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA1 - getCatalog returns options with name, description and calculated date`() {
        val catalog = useCase.getCatalog(existingAnimal)

        assertTrue("El catálogo no puede estar vacío", catalog.isNotEmpty())
        catalog.forEach { option ->
            assertTrue("El nombre no puede estar vacío", option.vaccineName.isNotBlank())
            assertTrue("La descripción no puede estar vacía", option.educationalDescription.isNotBlank())
            assertTrue("La fecha sugerida debe ser positiva", option.suggestedDate > 0)
        }
    }

    @Test
    fun `CA1 - getCatalog for BOVINE returns exactly 3 vaccines`() {
        val catalog = useCase.getCatalog(existingAnimal)
        assertEquals(3, catalog.size)
    }

    @Test
    fun `CA1 - getCatalog for SWINE returns exactly 1 vaccine`() {
        val swine = existingAnimal.copy(type = AnimalType.SWINE)
        val catalog = useCase.getCatalog(swine)
        assertEquals(1, catalog.size)
        assertEquals("Peste Porcina Clásica", catalog[0].vaccineName)
    }

    @Test
    fun `CA1 - catalog dates are in the future relative to birth date`() {
        val catalog = useCase.getCatalog(existingAnimal)
        catalog.forEach { option ->
            assertTrue(
                "La fecha sugerida (${option.suggestedDate}) debe ser posterior al nacimiento (${existingAnimal.birthDate})",
                option.suggestedDate > existingAnimal.birthDate
            )
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-2: Filtrado de Selección
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA2 - only selected vaccines are persisted`() = runBlocking {
        givenAnimalExists()
        givenSaveSucceeds()

        val inputs = listOf(
            buildPendingInput("Fiebre Aftosa (Ciclo I)", isSelected = true),
            buildPendingInput("Brucelosis B19", isSelected = false), // No debe persistirse
            buildPendingInput("Rabia Silvestre", isSelected = true)
        )

        val result = useCase.execute("A-042", inputs)

        // Verificamos que el resultado es exitoso y contiene solo las 2 seleccionadas
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()!!.size)

        // Verificamos que saveAll fue llamado exactamente una vez con cualquier lista
        verify(vaccinationRepository).saveAll(any())

        // Verificamos que las vacunas persistidas son exactamente las seleccionadas
        val persistedNames = result.getOrNull()!!.map { it.vaccineName }
        assertTrue(persistedNames.contains("Fiebre Aftosa (Ciclo I)"))
        assertTrue(persistedNames.contains("Rabia Silvestre"))
        assertFalse(persistedNames.contains("Brucelosis B19"))
    }

    @Test
    fun `CA2 - returns failure when no vaccine is selected`(): Unit = runBlocking {
        givenAnimalExists()

        val inputs = listOf(
            buildPendingInput("Fiebre Aftosa (Ciclo I)", isSelected = false),
            buildPendingInput("Brucelosis B19", isSelected = false)
        )

        val result = useCase.execute("A-042", inputs)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("seleccionar") == true)
        verify(vaccinationRepository, never()).saveAll(any())
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-3: Procesamiento de Fechas Editadas
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA3 - custom scheduledDate is persisted as-is`() = runBlocking {
        givenAnimalExists()
        givenSaveSucceeds()

        val customDate = futureDate + (10L * 24 * 60 * 60 * 1000) // +10 días al futuro
        val inputs = listOf(
            buildPendingInput("Fiebre Aftosa (Ciclo I)", scheduledDate = customDate)
        )

        val result = useCase.execute("A-042", inputs)

        assertTrue(result.isSuccess)
        assertEquals(customDate, result.getOrNull()!!.first().scheduledDate)
    }

    @Test
    fun `CA3 - returns failure when scheduledDate is zero`() = runBlocking {
        givenAnimalExists()

        val inputs = listOf(
            buildPendingInput("Fiebre Aftosa (Ciclo I)", scheduledDate = 0L)
        )

        val result = useCase.execute("A-042", inputs)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("fecha programada") == true)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-4: Diferenciación de Estado Inicial
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA4 - record is saved with PENDING status when initialStatus is PENDING`() = runBlocking {
        givenAnimalExists()
        givenSaveSucceeds()

        val inputs = listOf(buildPendingInput("Fiebre Aftosa (Ciclo I)"))
        val result = useCase.execute("A-042", inputs)

        assertEquals(VaccineStatus.PENDING, result.getOrNull()!!.first().status)
    }

    @Test
    fun `CA4 - record is saved with APPLIED status when initialStatus is APPLIED`() = runBlocking {
        givenAnimalExists()
        givenSaveSucceeds()

        val inputs = listOf(buildAppliedInput("Fiebre Aftosa (Ciclo I)", appliedDate = pastDate))
        val result = useCase.execute("A-042", inputs)

        assertEquals(VaccineStatus.APPLIED, result.getOrNull()!!.first().status)
    }

    @Test
    fun `CA4 - returns failure when OVERDUE is sent as initialStatus`() = runBlocking {
        givenAnimalExists()

        val inputs = listOf(
            buildPendingInput("Fiebre Aftosa (Ciclo I)").copy(initialStatus = VaccineStatus.OVERDUE)
        )

        val result = useCase.execute("A-042", inputs)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("OVERDUE") == true)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-5: Integridad de Relación
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA5 - all persisted records have the correct animalId`() = runBlocking {
        givenAnimalExists()
        givenSaveSucceeds()

        val inputs = listOf(
            buildPendingInput("Fiebre Aftosa (Ciclo I)"),
            buildPendingInput("Rabia Silvestre")
        )

        val result = useCase.execute("A-042", inputs)

        assertTrue(result.isSuccess)
        result.getOrNull()!!.forEach { record ->
            assertEquals("A-042", record.animalId)
        }
    }

    @Test
    fun `CA5 - returns failure when animal does not exist`(): Unit = runBlocking {
        whenever(animalRepository.getAnimalsByOwner(any())).thenReturn(emptyList())

        val inputs = listOf(buildPendingInput("Fiebre Aftosa (Ciclo I)"))
        val result = useCase.execute("NO-EXISTE", inputs)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("NO-EXISTE") == true)
        verify(vaccinationRepository, never()).saveAll(any())
    }

    @Test
    fun `CA5 - returns failure when animalId is blank`() = runBlocking {
        val result = useCase.execute("  ", listOf(buildPendingInput("X")))

        assertTrue(result.isFailure)
        assertEquals("El ID del animal no puede estar vacío.", result.exceptionOrNull()?.message)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-6: Cálculo de Aplicación
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA6 - APPLIED vaccine requires appliedDate`() = runBlocking {
        givenAnimalExists()

        val inputs = listOf(
            buildAppliedInput("Fiebre Aftosa (Ciclo I)", appliedDate = null) // Sin fecha → Error
        )

        val result = useCase.execute("A-042", inputs)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("APLICADA") == true)
        assertTrue(result.exceptionOrNull()?.message?.contains("fecha de aplicación") == true)
    }

    @Test
    fun `CA6 - APPLIED vaccine with valid appliedDate is saved correctly`() = runBlocking {
        givenAnimalExists()
        givenSaveSucceeds()

        val inputs = listOf(
            buildAppliedInput("Fiebre Aftosa (Ciclo I)", appliedDate = pastDate)
        )

        val result = useCase.execute("A-042", inputs)

        assertTrue(result.isSuccess)
        assertEquals(pastDate, result.getOrNull()!!.first().appliedDate)
    }

    @Test
    fun `CA6 - PENDING vaccine must not have appliedDate`() = runBlocking {
        givenAnimalExists()

        val inputs = listOf(
            buildPendingInput("Fiebre Aftosa (Ciclo I)").copy(appliedDate = pastDate) // Inconsistente
        )

        val result = useCase.execute("A-042", inputs)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("PENDIENTE") == true)
    }

    @Test
    fun `CA6 - PENDING vaccine has null appliedDate in persisted record`() = runBlocking {
        givenAnimalExists()
        givenSaveSucceeds()

        val inputs = listOf(buildPendingInput("Fiebre Aftosa (Ciclo I)"))
        val result = useCase.execute("A-042", inputs)

        assertNull(result.getOrNull()!!.first().appliedDate)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private suspend fun givenAnimalExists() {
        whenever(animalRepository.getAnimalsByOwner(any())).thenReturn(listOf(existingAnimal))
    }

    private suspend fun givenSaveSucceeds() {
        whenever(vaccinationRepository.saveAll(any())).thenReturn(true)
    }

    private fun buildPendingInput(
        name: String,
        isSelected: Boolean = true,
        scheduledDate: Long = futureDate
    ) = ProcessedVaccinationInput(
        vaccineName = name,
        isSelected = isSelected,
        scheduledDate = scheduledDate,
        initialStatus = VaccineStatus.PENDING,
        appliedDate = null,
        doseMl = null
    )

    private fun buildAppliedInput(
        name: String,
        appliedDate: Long?,
        doseMl: Double? = 5.0
    ) = ProcessedVaccinationInput(
        vaccineName = name,
        isSelected = true,
        scheduledDate = pastDate,
        initialStatus = VaccineStatus.APPLIED,
        appliedDate = appliedDate,
        doseMl = doseMl
    )
}