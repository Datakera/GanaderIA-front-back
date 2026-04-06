package com.ganadeia.app.data.repository

import com.ganadeia.app.domain.model.HealthRecord
import com.ganadeia.app.domain.model.SyncStatus
import com.ganadeia.app.domain.model.VisibleSymptom
import com.ganadeia.app.infrastructure.persistence.room.dao.HealthCheckDao
import com.ganadeia.app.infrastructure.persistence.room.entity.HealthCheckEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class RoomHealthCheckRepositoryTest {

    private lateinit var dao: HealthCheckDao
    private lateinit var repository: RoomHealthCheckRepository

    @Before
    fun setup() {
        dao = mock(HealthCheckDao::class.java)
        repository = RoomHealthCheckRepository(dao)
    }

    @Test
    fun `saveHealthCheck - should map domain to entity and return true on success`() = runBlocking {
        // Given
        val record = createSampleRecord(
            symptoms = setOf(VisibleSymptom.FIEBRE, VisibleSymptom.COJERA)
        )
        val captor = argumentCaptor<HealthCheckEntity>()

        // When
        val result = repository.saveHealthCheck(record)

        // Then
        verify(dao).insertHealthCheck(captor.capture())
        assertTrue(result)

        val savedEntity = captor.firstValue
        assertEquals("FIEBRE,COJERA", savedEntity.symptoms) // Verifica Set -> CSV
        assertEquals(SyncStatus.PENDING_SYNC.name, savedEntity.syncStatus)
    }

    @Test
    fun `saveHealthCheck - should return false when dao throws exception`() = runBlocking {
        // Given
        whenever(dao.insertHealthCheck(any())).thenThrow(RuntimeException("DB Error"))

        // When
        val result = repository.saveHealthCheck(createSampleRecord())

        // Then
        assertFalse(result)
    }

    @Test
    fun `getHealthChecksByAnimal - should map entities to domain correctly`() = runBlocking {
        // Given
        val animalId = "VACA-123"
        val entities = listOf(
            createSampleEntity(id = "1", symptoms = "FIEBRE,DIARREA"),
            createSampleEntity(id = "2", symptoms = "") // Sin síntomas
        )
        whenever(dao.getHealthChecksByAnimal(animalId)).thenReturn(entities)

        // When
        val result = repository.getHealthChecksByAnimal(animalId)

        // Then
        assertEquals(2, result.size)
        assertTrue(result[0].symptoms.contains(VisibleSymptom.FIEBRE))
        assertTrue(result[0].symptoms.contains(VisibleSymptom.DIARREA))
        assertTrue(result[1].symptoms.isEmpty()) // Verifica manejo de CSV vacío
    }

    @Test
    fun `toDomain - should ignore unknown symptoms gracefully`() = runBlocking {
        // Given
        // Simulamos que en la DB hay un síntoma que ya no existe en el Enum (ej. por una actualización)
        val entityWithOldSymptom = createSampleEntity(symptoms = "FIEBRE,SINTOMA_ELIMINADO,COJERA")
        whenever(dao.getHealthChecksByAnimal(any())).thenReturn(listOf(entityWithOldSymptom))

        // When
        val result = repository.getHealthChecksByAnimal("any")

        // Then
        val symptoms = result[0].symptoms
        assertEquals(2, symptoms.size)
        assertTrue(symptoms.contains(VisibleSymptom.FIEBRE))
        assertTrue(symptoms.contains(VisibleSymptom.COJERA))
        // El "SINTOMA_ELIMINADO" fue ignorado por el try-catch interno del repo
    }

    @Test
    fun `updateSyncStatus - should call dao with correct name`() = runBlocking {
        // Given
        val id = "REC-99"
        val status = SyncStatus.SYNCED

        // When
        val result = repository.updateSyncStatus(id, status)

        // Then
        verify(dao).updateSyncStatus(eq(id), eq("SYNCED"))
        assertTrue(result)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun createSampleRecord(
        symptoms: Set<VisibleSymptom> = emptySet()
    ) = HealthRecord(
        id = "123",
        animalId = "VACA-001",
        date = 1000L,
        weightKg = 400.0,
        bodyConditionScore = 3,
        symptoms = symptoms,
        notes = "Test note",
        aiRecommendation = null,
        syncStatus = SyncStatus.PENDING_SYNC,
        confirmedFollowUpDate = null
    )

    private fun createSampleEntity(
        id: String = "123",
        symptoms: String = ""
    ) = HealthCheckEntity(
        id = id,
        animalId = "VACA-001",
        date = 1000L,
        weightKg = 400.0,
        bodyConditionScore = 3,
        symptoms = symptoms,
        notes = "Test note",
        aiRecommendation = null,
        syncStatus = "PENDING_SYNC",
        confirmedFollowUpDate = null
    )
}