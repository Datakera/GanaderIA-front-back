package com.ganadeia.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ganadeia.app.data.repository.RoomVaccinationRepository
import com.ganadeia.app.domain.model.VaccinationRecord
import com.ganadeia.app.domain.model.VaccineStatus
import com.ganadeia.app.infrastructure.persistence.room.AppDatabase
import com.ganadeia.app.infrastructure.persistence.room.dao.VaccinationDao
import com.ganadeia.app.infrastructure.persistence.room.entity.AnimalEntity
import com.ganadeia.app.infrastructure.persistence.room.entity.VaccinationEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados para la capa de persistencia de vacunaciones.
 *
 * Se dividen en dos niveles:
 *
 * 1. [VaccinationDaoTest] — Valida el contrato SQL del DAO directamente:
 *    queries, FK, CASCADE DELETE, índices y conflictos REPLACE.
 *
 * 2. [RoomVaccinationRepositoryTest] — Valida los mappers
 *    Domain ↔ Entity dentro del repositorio usando la misma DB en memoria.
 *
 * Ambos usan una base de datos en MEMORIA para que cada test
 * parta de un estado limpio sin efectos colaterales.
 */

// ══════════════════════════════════════════════════════════════════════════════
// 1. DAO Tests — Contrato SQL
// ══════════════════════════════════════════════════════════════════════════════

@RunWith(AndroidJUnit4::class)
class VaccinationDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: VaccinationDao

    // Animal padre requerido por la FK antes de insertar vacunas
    private val parentAnimal = AnimalEntity(
        id = "VACA-001",
        ownerId = "owner-1",
        name = "Lola",
        type = "BOVINE",
        breed = "Holstein",
        hardiness = "LOW",
        weight = 350.0,
        birthDate = 1_000_000L,
        purpose = "MILK",
        status = "ACTIVE",
        nextFollowUpDate = null
    )

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .build()
        dao = db.vaccinationDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun insertParentAnimal() {
        db.animalDao().insertAnimal(parentAnimal)
    }

    private fun buildEntity(
        id: String,
        animalId: String = "VACA-001",
        vaccineName: String = "Fiebre Aftosa (Ciclo I)",
        scheduledDate: Long = 2_000_000L,
        appliedDate: Long? = null,
        status: String = "PENDING",
        doseMl: Double? = null
    ) = VaccinationEntity(
        id = id,
        animalId = animalId,
        vaccineName = vaccineName,
        scheduledDate = scheduledDate,
        appliedDate = appliedDate,
        status = status,
        doseMl = doseMl
    )

    // ── insertAll + getVaccinationsByAnimal ───────────────────────────────────

    @Test
    fun insertAll_andReadByAnimal_returnsCorrectRecords() = runBlocking {
        insertParentAnimal()

        val entities = listOf(
            buildEntity(id = "VAC-1", vaccineName = "Fiebre Aftosa (Ciclo I)", scheduledDate = 1_000L),
            buildEntity(id = "VAC-2", vaccineName = "Brucelosis B19", scheduledDate = 2_000L),
            buildEntity(id = "VAC-3", vaccineName = "Rabia Silvestre", scheduledDate = 3_000L)
        )

        dao.insertAll(entities)
        val result = dao.getVaccinationsByAnimal("VACA-001")

        assertEquals(3, result.size)
        // Verifica orden ascendente por scheduledDate
        assertEquals("Fiebre Aftosa (Ciclo I)", result[0].vaccineName)
        assertEquals("Brucelosis B19", result[1].vaccineName)
        assertEquals("Rabia Silvestre", result[2].vaccineName)
    }

    @Test
    fun getVaccinationsByAnimal_returnsOnlyRecordsForThatAnimal() = runBlocking {
        // Insertar segundo animal
        val secondAnimal = parentAnimal.copy(id = "VACA-002", name = "Rosa")
        db.animalDao().insertAnimal(parentAnimal)
        db.animalDao().insertAnimal(secondAnimal)

        dao.insertAll(listOf(
            buildEntity(id = "VAC-1", animalId = "VACA-001"),
            buildEntity(id = "VAC-2", animalId = "VACA-001"),
            buildEntity(id = "VAC-3", animalId = "VACA-002") // Pertenece a otro animal
        ))

        val resultAnimal1 = dao.getVaccinationsByAnimal("VACA-001")
        val resultAnimal2 = dao.getVaccinationsByAnimal("VACA-002")

        assertEquals(2, resultAnimal1.size)
        assertEquals(1, resultAnimal2.size)
    }

    @Test
    fun getVaccinationsByAnimal_returnsEmptyList_whenAnimalHasNoVaccinations() = runBlocking {
        insertParentAnimal()

        val result = dao.getVaccinationsByAnimal("VACA-001")

        assertTrue(result.isEmpty())
    }

    // ── getPendingVaccinations ────────────────────────────────────────────────

    @Test
    fun getPendingVaccinations_returnsOnlyPendingAndOverdue() = runBlocking {
        insertParentAnimal()

        dao.insertAll(listOf(
            buildEntity(id = "VAC-1", status = "PENDING"),
            buildEntity(id = "VAC-2", status = "OVERDUE"),
            buildEntity(id = "VAC-3", status = "APPLIED",
                appliedDate = 900_000L) // No debe aparecer
        ))

        val result = dao.getPendingVaccinations()

        assertEquals(2, result.size)
        assertTrue(result.all { it.status == "PENDING" || it.status == "OVERDUE" })
        assertTrue(result.none { it.status == "APPLIED" })
    }

    @Test
    fun getPendingVaccinations_returnsEmpty_whenAllAreApplied() = runBlocking {
        insertParentAnimal()

        dao.insertAll(listOf(
            buildEntity(id = "VAC-1", status = "APPLIED", appliedDate = 900_000L),
            buildEntity(id = "VAC-2", status = "APPLIED", appliedDate = 950_000L)
        ))

        val result = dao.getPendingVaccinations()

        assertTrue(result.isEmpty())
    }

    // ── markAsApplied ─────────────────────────────────────────────────────────

    @Test
    fun markAsApplied_updatesStatusAndAppliedDate() = runBlocking {
        insertParentAnimal()
        dao.insertAll(listOf(buildEntity(id = "VAC-1", status = "PENDING")))

        dao.markAsApplied(id = "VAC-1", appliedDate = 9_000_000L, doseMl = 5.0)

        val result = dao.getVaccinationsByAnimal("VACA-001")
        val updated = result.first()

        assertEquals("APPLIED", updated.status)
        assertEquals(9_000_000L, updated.appliedDate)
        assertEquals(5.0, updated.doseMl)
    }

    @Test
    fun markAsApplied_withNullDose_updatesCorrectly() = runBlocking {
        insertParentAnimal()
        dao.insertAll(listOf(buildEntity(id = "VAC-1", status = "PENDING")))

        dao.markAsApplied(id = "VAC-1", appliedDate = 9_000_000L, doseMl = null)

        val result = dao.getVaccinationsByAnimal("VACA-001").first()

        assertEquals("APPLIED", result.status)
        assertNull(result.doseMl)
    }

    // ── Conflicto REPLACE ─────────────────────────────────────────────────────

    @Test
    fun insertAll_withReplace_overwritesExistingRecord() = runBlocking {
        insertParentAnimal()
        dao.insertAll(listOf(buildEntity(id = "VAC-1", vaccineName = "Nombre Original")))

        // Misma ID, nombre diferente → debe reemplazar
        dao.insertAll(listOf(buildEntity(id = "VAC-1", vaccineName = "Nombre Actualizado")))

        val result = dao.getVaccinationsByAnimal("VACA-001")

        assertEquals(1, result.size) // No duplicó
        assertEquals("Nombre Actualizado", result.first().vaccineName)
    }

    // ── Integridad referencial (FK + CASCADE DELETE) ──────────────────────────

    @Test
    fun cascadeDelete_removesVaccinations_whenParentAnimalIsDeleted() = runBlocking {
        insertParentAnimal()
        dao.insertAll(listOf(
            buildEntity(id = "VAC-1"),
            buildEntity(id = "VAC-2")
        ))

        // Verificamos que existen antes del borrado
        assertEquals(2, dao.getVaccinationsByAnimal("VACA-001").size)

        // Borramos el animal padre
        db.animalDao().deleteAnimal("VACA-001")

        // Las vacunas deben haberse borrado en cascada
        val result = dao.getVaccinationsByAnimal("VACA-001")
        assertTrue(result.isEmpty())
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 2. Repository Tests — Mappers Domain ↔ Entity
// ══════════════════════════════════════════════════════════════════════════════

@RunWith(AndroidJUnit4::class)
class RoomVaccinationRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: RoomVaccinationRepository

    private val parentAnimal = AnimalEntity(
        id = "VACA-001",
        ownerId = "owner-1",
        name = "Lola",
        type = "BOVINE",
        breed = "Holstein",
        hardiness = "LOW",
        weight = 350.0,
        birthDate = 1_000_000L,
        purpose = "MILK",
        status = "ACTIVE",
        nextFollowUpDate = null
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = RoomVaccinationRepository(db.vaccinationDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun insertParentAnimal() {
        db.animalDao().insertAnimal(parentAnimal)
    }

    private fun buildRecord(
        id: String,
        animalId: String = "VACA-001",
        vaccineName: String = "Fiebre Aftosa (Ciclo I)",
        scheduledDate: Long = 2_000_000L,
        appliedDate: Long? = null,
        status: VaccineStatus = VaccineStatus.PENDING,
        doseMl: Double? = null
    ) = VaccinationRecord(
        id = id,
        animalId = animalId,
        vaccineName = vaccineName,
        scheduledDate = scheduledDate,
        appliedDate = appliedDate,
        status = status,
        doseMl = doseMl
    )

    // ── saveAll ───────────────────────────────────────────────────────────────

    @Test
    fun saveAll_returnsTrueAndPersistsRecords() = runBlocking {
        insertParentAnimal()

        val records = listOf(
            buildRecord("VAC-1", vaccineName = "Fiebre Aftosa (Ciclo I)"),
            buildRecord("VAC-2", vaccineName = "Brucelosis B19"),
            buildRecord("VAC-3", vaccineName = "Rabia Silvestre")
        )

        val result = repository.saveAll(records)

        assertTrue(result)
        val saved = repository.getVaccinationsByAnimal("VACA-001")
        assertEquals(3, saved.size)
    }

    @Test
    fun saveAll_mapsStatusEnum_toStringAndBack() = runBlocking {
        insertParentAnimal()

        val pendingRecord = buildRecord("VAC-1", status = VaccineStatus.PENDING)
        val appliedRecord = buildRecord(
            "VAC-2",
            status = VaccineStatus.APPLIED,
            appliedDate = 9_000_000L,
            scheduledDate = 8_000_000L
        )

        repository.saveAll(listOf(pendingRecord, appliedRecord))
        val result = repository.getVaccinationsByAnimal("VACA-001")

        val pending = result.first { it.id == "VAC-1" }
        val applied = result.first { it.id == "VAC-2" }

        assertEquals(VaccineStatus.PENDING, pending.status)
        assertEquals(VaccineStatus.APPLIED, applied.status)
    }

    @Test
    fun saveAll_preservesAllFields_withoutDataLoss() = runBlocking {
        insertParentAnimal()

        val original = buildRecord(
            id = "VAC-1",
            animalId = "VACA-001",
            vaccineName = "Fiebre Aftosa (Ciclo I)",
            scheduledDate = 5_000_000L,
            appliedDate = 4_800_000L,
            status = VaccineStatus.APPLIED,
            doseMl = 2.5
        )

        repository.saveAll(listOf(original))
        val saved = repository.getVaccinationsByAnimal("VACA-001").first()

        assertEquals(original.id, saved.id)
        assertEquals(original.animalId, saved.animalId)
        assertEquals(original.vaccineName, saved.vaccineName)
        assertEquals(original.scheduledDate, saved.scheduledDate)
        assertEquals(original.appliedDate, saved.appliedDate)
        assertEquals(original.status, saved.status)
        assertEquals(original.doseMl, saved.doseMl)
    }

    @Test
    fun saveAll_withNullableFields_persistsAndReadsNullsCorrectly() = runBlocking {
        insertParentAnimal()

        val record = buildRecord(
            "VAC-1",
            appliedDate = null,
            doseMl = null,
            status = VaccineStatus.PENDING
        )

        repository.saveAll(listOf(record))
        val saved = repository.getVaccinationsByAnimal("VACA-001").first()

        assertNull(saved.appliedDate)
        assertNull(saved.doseMl)
    }

    // ── getVaccinationsByAnimal ───────────────────────────────────────────────

    @Test
    fun getVaccinationsByAnimal_returnsMappedDomainObjects() = runBlocking {
        insertParentAnimal()

        repository.saveAll(listOf(buildRecord("VAC-1")))
        val result = repository.getVaccinationsByAnimal("VACA-001")

        assertEquals(1, result.size)
        // Verifica que es un objeto de dominio, no una entidad de Room
        assertTrue(result.first() is VaccinationRecord)
    }

    @Test
    fun getVaccinationsByAnimal_returnsEmptyList_forUnknownAnimal() = runBlocking {
        val result = repository.getVaccinationsByAnimal("ANIMAL-INEXISTENTE")
        assertTrue(result.isEmpty())
    }
}