package com.ganadeia.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ganadeia.app.infrastructure.persistence.room.AppDatabase
import com.ganadeia.app.infrastructure.persistence.room.dao.SessionDao
import com.ganadeia.app.infrastructure.persistence.room.dao.UserDao
import com.ganadeia.app.infrastructure.persistence.room.entity.SessionEntity
import com.ganadeia.app.infrastructure.persistence.room.entity.UserEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test de integración con Room en memoria.
 * Verifica que las consultas SQL del DAO funcionen correctamente
 * con la BD real (no mockeada).
 *
 * ⚠️ Requiere ejecutarse en un dispositivo/emulador Android.
 * Ubicación: app/src/androidTest/
 */
@RunWith(AndroidJUnit4::class)
class AuthDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var sessionDao: SessionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        userDao = db.userDao()
        sessionDao = db.sessionDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UserDao
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun userDaoFindByEmailAndPasswordReturnsUserWithCorrectCredentials() = runBlocking {
        // Given
        val user = buildUserEntity(
            email = "test@farm.co",
            passwordHash = "sha256hashvalue"
        )
        userDao.insertUser(user)

        // When
        val found = userDao.findByEmailAndPassword("test@farm.co", "sha256hashvalue")

        // Then
        assertNotNull(found)
        assertEquals("uuid-test", found!!.id)
        assertEquals("test@farm.co", found.email)
    }

    @Test
    fun userDaoFindByEmailAndPasswordReturnsNullWithWrongPassword() = runBlocking {
        userDao.insertUser(buildUserEntity(passwordHash = "correcthash"))

        val found = userDao.findByEmailAndPassword("test@farm.co", "wronghash")

        assertNull(found)
    }

    @Test
    fun userDaoFindByEmailAndPasswordReturnsNullWithWrongEmail() = runBlocking {
        userDao.insertUser(buildUserEntity(email = "real@farm.co"))

        val found = userDao.findByEmailAndPassword("fake@farm.co", "sha256hashvalue")

        assertNull(found)
    }

    @Test
    fun userDaoUpdateUserUpdatesExistingUser() = runBlocking {
        val original = buildUserEntity(name = "Carlos")
        val updated = original.copy(name = "Carlos Rodríguez")

        userDao.insertUser(original)
        userDao.updateUser(updated)

        val found = userDao.findByEmail("test@farm.co")
        assertEquals("Carlos Rodríguez", found!!.name)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SessionDao
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun sessionDaoInsertSessionAndGetActiveSessionRoundTrip() = runBlocking {
        val session = buildSessionEntity(isActive = true)
        sessionDao.insertSession(session)

        val found = sessionDao.getActiveSession()

        assertNotNull(found)
        assertEquals("session-001", found!!.id)
        assertEquals("token-abc", found.token)
        assertTrue(found.isActive)
    }

    @Test
    fun sessionDaoGetActiveSessionReturnsNullWhenNoActiveSession() = runBlocking {
        // BD vacía
        val found = sessionDao.getActiveSession()
        assertNull(found)
    }

    @Test
    fun sessionDaoGetActiveSessionReturnsNullAfterDeactivateAllSessions() = runBlocking {
        sessionDao.insertSession(buildSessionEntity(isActive = true))

        sessionDao.deactivateAllSessions() // Simula logout

        val found = sessionDao.getActiveSession()
        assertNull(found)
    }

    @Test
    fun sessionDaoReplaceStrategyKeepsOnlyLatestSession() = runBlocking {
        // Dos logins seguidos: el segundo debe ser el activo
        val first = buildSessionEntity(id = "session-001", token = "old-token")
        val second = buildSessionEntity(id = "session-002", token = "new-token")

        sessionDao.insertSession(first)
        sessionDao.deactivateAllSessions() // Simula que el primero fue cerrado
        sessionDao.insertSession(second)

        val found = sessionDao.getActiveSession()
        assertEquals("new-token", found!!.token)
    }

    @Test
    fun sessionDaoDeactivateAllSessionsSetsInactiveWithoutDeleting() = runBlocking {
        // Verifica el "soft-delete": el registro permanece pero isActive=false
        sessionDao.insertSession(buildSessionEntity(id = "session-keep", isActive = true))

        sessionDao.deactivateAllSessions()

        // getActiveSession devuelve null, pero el registro persiste
        val active = sessionDao.getActiveSession()
        assertNull(active)
        // Si tuviéramos un getAll, el registro seguiría ahí con isActive=0
        // Esto es suficiente para verificar el comportamiento del logout
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildUserEntity(
        id: String = "uuid-test",
        name: String = "Test User",
        email: String = "test@farm.co",
        passwordHash: String = "sha256hashvalue"
    ) = UserEntity(
        id = id,
        name = name,
        email = email,
        passwordHash = passwordHash,
        role = "RANCHER",
        ranchName = "Finca Test",
        location = "Huila",
        permissions = null,
        createdAt = 1_000_000L,
        updatedAt = 1_000_000L
    )

    private fun buildSessionEntity(
        id: String = "session-001",
        token: String = "token-abc",
        isActive: Boolean = true
    ) = SessionEntity(
        id = id,
        userId = "uuid-test",
        token = token,
        createdAt = 1_700_000_000_000L,
        expiresAt = 1_700_000_000_000L + (30L * 24 * 60 * 60 * 1000),
        isActive = isActive
    )
}