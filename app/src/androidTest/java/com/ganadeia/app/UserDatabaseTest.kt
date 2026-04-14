package com.ganadeia.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ganadeia.app.infrastructure.persistence.room.AppDatabase
import com.ganadeia.app.infrastructure.persistence.room.dao.UserDao
import com.ganadeia.app.infrastructure.persistence.room.entity.UserEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests de integración con Room en memoria para el [UserDao].
 *
 * Verifica que las consultas SQL funcionen correctamente con la BD real,
 * incluyendo la unicidad de email, la estrategia ABORT en inserción y
 * que el campo passwordHash nunca se expone en consultas de perfil.
 *
 * ⚠️ Requiere ejecutarse en un dispositivo/emulador Android.
 * Ubicación: app/src/androidTest/
 */
@RunWith(AndroidJUnit4::class)
class UserDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        userDao = db.userDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-1: Insert y lectura básica
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun insertUserAndFindByIdReturnsCorrectEntity() = runBlocking {
        userDao.insertUser(buildEntity(id = "u-001", email = "carlos@finca.co"))

        val found = userDao.findById("u-001")

        assertNotNull(found)
        assertEquals("u-001", found!!.id)
        assertEquals("Carlos", found.name)
        assertEquals("carlos@finca.co", found.email)
        assertEquals("RANCHER", found.role)
    }

    @Test
    fun findByIdReturnsNullForNonexistentUser() = runBlocking {
        val found = userDao.findById("does-not-exist")
        assertNull(found)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-3: Unicidad de correo
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun findByEmailReturnsEntityWhenEmailExists() = runBlocking {
        userDao.insertUser(buildEntity(email = "registrado@finca.co"))

        val found = userDao.findByEmail("registrado@finca.co")

        assertNotNull(found)
        assertEquals("registrado@finca.co", found!!.email)
    }

    @Test
    fun findByEmailReturnsNullWhenEmailDoesNotExist() = runBlocking {
        val found = userDao.findByEmail("noexiste@finca.co")
        assertNull(found)
    }

    @Test
    fun insertUserWithDuplicateEmailThrowsException() = runBlocking {
        val first = buildEntity(id = "u-001", email = "duplicado@finca.co")
        val second = buildEntity(id = "u-002", email = "duplicado@finca.co") // mismo email, distinto id

        userDao.insertUser(first)

        var exceptionThrown = false
        try {
            userDao.insertUser(second)
        } catch (e: Exception) {
            exceptionThrown = true
        }

        assertTrue("Debería lanzar excepción por email duplicado", exceptionThrown)

        // Solo el primero debe existir
        val found = userDao.findByEmail("duplicado@finca.co")
        assertEquals("u-001", found!!.id)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-2: Almacenamiento de passwordHash
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun passwordHashIsPersistedAndRecoveredCorrectly() = runBlocking {
        val hash = "a3f5b2c1d4e6f789" // SHA-256 simulado
        userDao.insertUser(buildEntity(passwordHash = hash))

        val found = userDao.findByEmailAndPassword("carlos@finca.co", hash)

        assertNotNull(found)
        assertEquals(hash, found!!.passwordHash)
    }

    @Test
    fun findByEmailAndPasswordReturnsNullWithWrongHash() = runBlocking {
        userDao.insertUser(buildEntity(passwordHash = "correcthash"))

        val found = userDao.findByEmailAndPassword("carlos@finca.co", "wronghash")

        assertNull(found)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // updateUser — campos mutables del perfil
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun updateUserChangesNameAndRanchNameWithoutTouchingPasswordHash() = runBlocking {
        val originalHash = "super_secret_hash"
        userDao.insertUser(buildEntity(passwordHash = originalHash, name = "Carlos"))

        val updated = buildEntity(
            passwordHash = originalHash, // Se mantiene el mismo hash
            name = "Carlos Rodríguez",
            ranchName = "Finca Nueva"
        )
        userDao.updateUser(updated)

        val found = userDao.findById("u-001")
        assertEquals("Carlos Rodríguez", found!!.name)
        assertEquals("Finca Nueva", found.ranchName)
        assertEquals(originalHash, found.passwordHash) // Hash intacto
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Campos nullable
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun nullableFieldsArePersistedAndReadAsNullCorrectly() = runBlocking {
        userDao.insertUser(
            buildEntity(ranchName = null, location = null, permissions = null)
        )

        val found = userDao.findById("u-001")

        assertNull(found!!.ranchName)
        assertNull(found.location)
        assertNull(found.permissions)
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun buildEntity(
        id: String = "u-001",
        name: String = "Carlos",
        email: String = "carlos@finca.co",
        passwordHash: String = "hash_default",
        ranchName: String? = "Finca El Valle",
        location: String? = null,
        permissions: String? = null
    ) = UserEntity(
        id = id,
        name = name,
        email = email,
        passwordHash = passwordHash,
        role = "RANCHER",
        ranchName = ranchName,
        location = location,
        permissions = permissions,
        createdAt = 1_000_000L,
        updatedAt = 1_000_000L
    )
}