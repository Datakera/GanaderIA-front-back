package com.ganadeia.app.data.repository

import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.infrastructure.persistence.room.dao.UserDao
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

class RoomUserRepositoryTest {

    private lateinit var userDao: UserDao
    private lateinit var repository: RoomUserRepository

    @Before
    fun setUp() {
        userDao = mock(UserDao::class.java)
        repository = RoomUserRepository(userDao)
    }

    // ── saveUser ──────────────────────────────────────────────────────────────

    @Test
    fun `saveUser - should map User domain to entity with passwordHash`() = runBlocking {
        val user = buildUser()
        val captor = argumentCaptor<UserEntity>()

        val result = repository.saveUser(user, "hash_abc123")

        verify(userDao).insertUser(captor.capture())
        assertTrue(result)

        val entity = captor.firstValue
        assertEquals("uuid-001", entity.id)
        assertEquals("Carlos", entity.name)
        assertEquals("carlos@finca.co", entity.email)
        assertEquals("hash_abc123", entity.passwordHash)
        assertEquals("RANCHER", entity.role)
    }

    @Test
    fun `saveUser - should return false when dao throws exception`() = runBlocking {
        whenever(userDao.insertUser(any())).thenThrow(RuntimeException("Unique constraint failed"))

        val result = repository.saveUser(buildUser(), "hash")

        assertFalse(result)
    }

    // ── existsByEmail ─────────────────────────────────────────────────────────

    @Test
    fun `existsByEmail - should return true when user found`() = runBlocking {
        whenever(userDao.findByEmail(eq("carlos@finca.co"))).thenReturn(buildUserEntity())

        val result = repository.existsByEmail("carlos@finca.co")

        assertTrue(result)
    }

    @Test
    fun `existsByEmail - should return false when no user found`() = runBlocking {
        whenever(userDao.findByEmail(any())).thenReturn(null)

        val result = repository.existsByEmail("nuevo@finca.co")

        assertFalse(result)
    }

    @Test
    fun `existsByEmail - should return true when dao throws exception to prevent duplicates`() =
        runBlocking {
            whenever(userDao.findByEmail(any())).thenThrow(RuntimeException("DB Error"))

            // En caso de error preferimos bloquear antes que crear duplicados
            val result = repository.existsByEmail("email@finca.co")

            assertTrue(result)
        }

    // ── getUserById ───────────────────────────────────────────────────────────

    @Test
    fun `getUserById - should map entity to User domain correctly`() = runBlocking {
        whenever(userDao.findById(eq("uuid-001"))).thenReturn(buildUserEntity())

        val result = repository.getUserById("uuid-001")

        assertNotNull(result)
        assertEquals("uuid-001", result!!.id)
        assertEquals("Carlos", result.name)
        assertEquals(UserRole.RANCHER, result.role)
    }

    @Test
    fun `getUserById - should return null when user not found`() = runBlocking {
        whenever(userDao.findById(any())).thenReturn(null)

        val result = repository.getUserById("unknown-id")

        assertNull(result)
    }

    @Test
    fun `getUserById - should return null when dao throws exception`() = runBlocking {
        whenever(userDao.findById(any())).thenThrow(RuntimeException("DB Error"))

        val result = repository.getUserById("uuid-001")

        assertNull(result)
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    fun `updateProfile - should preserve passwordHash from existing entity`() = runBlocking {
        val existingEntity = buildUserEntity(passwordHash = "original_hash")
        whenever(userDao.findById(eq("uuid-001"))).thenReturn(existingEntity)

        val updatedUser = buildUser().copy(name = "Carlos Actualizado")
        val captor = argumentCaptor<UserEntity>()

        repository.updateProfile(updatedUser)

        verify(userDao).updateUser(captor.capture())
        // El hash original debe preservarse — updateProfile no recibe nueva contraseña
        assertEquals("original_hash", captor.firstValue.passwordHash)
        assertEquals("Carlos Actualizado", captor.firstValue.name)
    }

    @Test
    fun `updateProfile - should return false when user not found`() = runBlocking {
        whenever(userDao.findById(any())).thenReturn(null)

        val result = repository.updateProfile(buildUser())

        assertFalse(result)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildUser() = User(
        id = "uuid-001",
        name = "Carlos",
        email = "carlos@finca.co",
        role = UserRole.RANCHER,
        ranchName = "Finca El Valle",
        location = null,
        permissions = null,
        createdAt = 1_000_000L,
        updatedAt = 1_000_000L
    )

    private fun buildUserEntity(passwordHash: String = "hash_xyz") = UserEntity(
        id = "uuid-001",
        name = "Carlos",
        email = "carlos@finca.co",
        passwordHash = passwordHash,
        role = "RANCHER",
        ranchName = "Finca El Valle",
        location = null,
        permissions = null,
        createdAt = 1_000_000L,
        updatedAt = 1_000_000L
    )
}