package com.ganadeia.app.data.repository

import com.ganadeia.app.domain.model.Session
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.infrastructure.persistence.room.dao.SessionDao
import com.ganadeia.app.infrastructure.persistence.room.dao.UserDao
import com.ganadeia.app.infrastructure.persistence.room.entity.SessionEntity
import com.ganadeia.app.infrastructure.persistence.room.entity.UserEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class RoomSessionRepositoryTest {

    private lateinit var userDao: UserDao
    private lateinit var sessionDao: SessionDao
    private lateinit var repository: RoomSessionRepository

    @Before
    fun setUp() {
        userDao = mock(UserDao::class.java)
        sessionDao = mock(SessionDao::class.java)
        repository = RoomSessionRepository(userDao, sessionDao)
    }

    // ── validateCredentials ───────────────────────────────────────────────────

    @Test
    fun `validateCredentials - should return User when entity found`() = runBlocking {
        val entity = buildUserEntity()
        whenever(userDao.findByEmailAndPassword(eq("carlos@fincaelvalle.co"), any()))
            .thenReturn(entity)

        val result = repository.validateCredentials("carlos@fincaelvalle.co", "hash123")

        assertNotNull(result)
        assertEquals("uuid-001", result!!.id)
        assertEquals("Carlos Rodríguez", result.name)
        assertEquals(UserRole.RANCHER, result.role)
    }

    @Test
    fun `validateCredentials - should return null when no user found`() = runBlocking {
        whenever(userDao.findByEmailAndPassword(any(), any())).thenReturn(null)

        val result = repository.validateCredentials("wrong@email.co", "badhash")

        assertNull(result)
    }

    @Test
    fun `validateCredentials - should return null when dao throws exception`() = runBlocking {
        whenever(userDao.findByEmailAndPassword(any(), any()))
            .thenThrow(RuntimeException("DB Error"))

        val result = repository.validateCredentials("email@test.co", "hash")

        assertNull(result) // Error manejado, no propaga excepción
    }

    // ── saveSession ───────────────────────────────────────────────────────────

    @Test
    fun `saveSession - should map domain to entity correctly`() = runBlocking {
        val session = buildSession()
        val captor = argumentCaptor<SessionEntity>()

        val result = repository.saveSession(session)

        verify(sessionDao).insertSession(captor.capture())
        assertTrue(result)

        val savedEntity = captor.firstValue
        assertEquals(session.id, savedEntity.id)
        assertEquals(session.userId, savedEntity.userId)
        assertEquals(session.token, savedEntity.token)
        assertEquals(session.expiresAt, savedEntity.expiresAt)
        assertTrue(savedEntity.isActive)
    }

    @Test
    fun `saveSession - should return false when dao throws exception`() = runBlocking {
        whenever(sessionDao.insertSession(any())).thenThrow(RuntimeException("DB Error"))

        val result = repository.saveSession(buildSession())

        assertFalse(result)
    }

    // ── getActiveSession ──────────────────────────────────────────────────────

    @Test
    fun `getActiveSession - should map entity to domain correctly`() = runBlocking {
        val entity = buildSessionEntity()
        whenever(sessionDao.getActiveSession()).thenReturn(entity)

        val result = repository.getActiveSession()

        assertNotNull(result)
        assertEquals("session-001", result!!.id)
        assertEquals("user-001", result.userId)
        assertEquals("token-xyz", result.token)
        assertTrue(result.isActive)
    }

    @Test
    fun `getActiveSession - should return null when no session exists`() = runBlocking {
        whenever(sessionDao.getActiveSession()).thenReturn(null)

        val result = repository.getActiveSession()

        assertNull(result)
    }

    // ── clearSession ──────────────────────────────────────────────────────────

    @Test
    fun `clearSession - should call deactivateAllSessions and return true`() = runBlocking {
        val result = repository.clearSession()

        verify(sessionDao).deactivateAllSessions()
        assertTrue(result)
    }

    @Test
    fun `clearSession - should return false when dao throws exception`() = runBlocking {
        whenever(sessionDao.deactivateAllSessions()).thenThrow(RuntimeException("DB Error"))

        val result = repository.clearSession()

        assertFalse(result)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildUserEntity() = UserEntity(
        id = "uuid-001",
        name = "Carlos Rodríguez",
        email = "carlos@fincaelvalle.co",
        passwordHash = "hash123",
        role = "RANCHER",
        ranchName = "Finca El Valle",
        location = "Huila, Colombia",
        permissions = null,
        createdAt = 1_000_000L,
        updatedAt = 1_000_000L
    )

    private fun buildSession() = Session(
        id = "session-001",
        userId = "user-001",
        token = "token-xyz",
        createdAt = 1_700_000_000_000L,
        expiresAt = 1_700_000_000_000L + (30L * 24 * 60 * 60 * 1000),
        isActive = true
    )

    private fun buildSessionEntity() = SessionEntity(
        id = "session-001",
        userId = "user-001",
        token = "token-xyz",
        createdAt = 1_700_000_000_000L,
        expiresAt = 1_700_000_000_000L + (30L * 24 * 60 * 60 * 1000),
        isActive = true
    )
}