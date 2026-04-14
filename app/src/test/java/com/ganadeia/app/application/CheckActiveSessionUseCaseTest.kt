package com.ganadeia.app.application

import com.ganadeia.app.domain.model.Session
import com.ganadeia.app.domain.port.driven.repository.SessionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class CheckActiveSessionUseCaseTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var useCase: CheckActiveSessionUseCase

    private val fixedNow = 1_700_000_000_000L

    @Before
    fun setUp() {
        sessionRepository = mock(SessionRepository::class.java)
        useCase = CheckActiveSessionUseCase(sessionRepository)
    }

    @Test
    fun `should return session when active and not expired`() = runBlocking {
        val validSession = buildSession(
            isActive = true,
            expiresAt = fixedNow + 1_000L // Expira en el futuro
        )
        whenever(sessionRepository.getActiveSession()).thenReturn(validSession)

        val result = useCase.execute(now = fixedNow)

        assertNotNull(result)
        assertEquals(validSession.token, result!!.token)
    }

    @Test
    fun `should return null when no session exists`() = runBlocking {
        whenever(sessionRepository.getActiveSession()).thenReturn(null)

        val result = useCase.execute(now = fixedNow)

        assertNull(result)
    }

    @Test
    fun `should return null when session is expired`() = runBlocking {
        val expiredSession = buildSession(
            isActive = true,
            expiresAt = fixedNow - 1_000L // Expiró en el pasado
        )
        whenever(sessionRepository.getActiveSession()).thenReturn(expiredSession)

        val result = useCase.execute(now = fixedNow)

        assertNull(result)
    }

    @Test
    fun `should return null when session is inactive even if not expired`() = runBlocking {
        val inactiveSession = buildSession(
            isActive = false,
            expiresAt = fixedNow + 999_999L // Fecha válida pero sesión cerrada manualmente
        )
        whenever(sessionRepository.getActiveSession()).thenReturn(inactiveSession)

        val result = useCase.execute(now = fixedNow)

        assertNull(result)
    }

    @Test
    fun `should return null when session expires exactly at now`() = runBlocking {
        // Caso borde: expiresAt == now → la sesión ya venció
        val borderSession = buildSession(
            isActive = true,
            expiresAt = fixedNow
        )
        whenever(sessionRepository.getActiveSession()).thenReturn(borderSession)

        val result = useCase.execute(now = fixedNow)

        assertNull(result)
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun buildSession(isActive: Boolean, expiresAt: Long) = Session(
        id = "session-001",
        userId = "user-001",
        token = "token-abc-123",
        createdAt = fixedNow - 1_000L,
        expiresAt = expiresAt,
        isActive = isActive
    )
}