package com.ganadeia.app.application

import com.ganadeia.app.domain.port.driven.repository.SessionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever

class LogoutUseCaseTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var useCase: LogoutUseCase

    @Before
    fun setUp() {
        sessionRepository = mock(SessionRepository::class.java)
        useCase = LogoutUseCase(sessionRepository)
    }

    @Test
    fun `should return success when session is cleared`(): Unit = runBlocking {
        whenever(sessionRepository.clearSession()).thenReturn(true)

        val result = useCase.execute()

        assertTrue(result.isSuccess)
        verify(sessionRepository).clearSession()
    }

    @Test
    fun `should return failure when session cannot be cleared`() = runBlocking {
        whenever(sessionRepository.clearSession()).thenReturn(false)

        val result = useCase.execute()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("cerrar la sesión") == true)
    }
}