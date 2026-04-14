package com.ganadeia.app.application

import com.ganadeia.app.domain.model.LoginCredentials
import com.ganadeia.app.domain.model.Session
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.domain.port.driven.repository.SessionRepository
import com.ganadeia.app.domain.service.PasswordService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever

class LoginUseCaseTest {

    // ── Dependencias mockeadas ─────────────────────────────────────────────────
    private lateinit var sessionRepository: SessionRepository
    private lateinit var useCase: LoginUseCase

    // Timestamp fijo para que las fechas sean deterministas
    private val fixedNow = 1_700_000_000_000L

    // Usuario que "existe" en la BD con contraseña "password123"
    private val mockUser = User(
        id = "uuid-ganadero-001",
        name = "Carlos Rodríguez",
        email = "carlos@fincaelvalle.co",
        role = UserRole.RANCHER,
        ranchName = "Finca El Valle",
        location = "Huila, Colombia",
        permissions = null,
        createdAt = 1_000_000L,
        updatedAt = 1_000_000L
    )

    @Before
    fun setUp() {
        sessionRepository = mock(SessionRepository::class.java)
        useCase = LoginUseCase(sessionRepository)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-1: Validación de credenciales — formato de entrada
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA1 - should fail when email is blank`(): Unit = runBlocking {
        val result = useCase.execute(
            LoginCredentials(email = "  ", password = "password123"),
            now = fixedNow
        )

        assertTrue(result.isFailure)
        assertEquals("El correo electrónico es obligatorio.", result.exceptionOrNull()?.message)
        verify(sessionRepository, never()).validateCredentials(any(), any())
    }

    @Test
    fun `CA1 - should fail when email has invalid format`(): Unit = runBlocking {
        val result = useCase.execute(
            LoginCredentials(email = "not-an-email", password = "password123"),
            now = fixedNow
        )

        assertTrue(result.isFailure)
        assertEquals(
            "El formato del correo electrónico no es válido.",
            result.exceptionOrNull()?.message
        )
        verify(sessionRepository, never()).validateCredentials(any(), any())
    }

    @Test
    fun `CA1 - should fail when password is blank`(): Unit = runBlocking {
        val result = useCase.execute(
            LoginCredentials(email = "carlos@fincaelvalle.co", password = ""),
            now = fixedNow
        )

        assertTrue(result.isFailure)
        assertEquals("La contraseña es obligatoria.", result.exceptionOrNull()?.message)
        verify(sessionRepository, never()).validateCredentials(any(), any())
    }

    @Test
    fun `CA1 - should trim email whitespace before validating`(): Unit = runBlocking {
        val expectedHash = PasswordService.hash("password123")
        whenever(
            sessionRepository.validateCredentials(
                eq("carlos@fincaelvalle.co"),
                eq(expectedHash)
            )
        ).thenReturn(mockUser)
        whenever(sessionRepository.saveSession(any())).thenReturn(true)

        val result = useCase.execute(
            // Email con espacios extra — deben ser eliminados
            LoginCredentials(email = "  carlos@fincaelvalle.co  ", password = "password123"),
            now = fixedNow
        )

        assertTrue(result.isSuccess)
        // Verifica que se llamó al repo con el email LIMPIO
        verify(sessionRepository).validateCredentials(eq("carlos@fincaelvalle.co"), any())
    }

    @Test
    fun `CA1 - should fail with generic message when credentials dont match`(): Unit = runBlocking {
        // El repo no encuentra al usuario (contraseña incorrecta o email no existe)
        whenever(sessionRepository.validateCredentials(any(), any())).thenReturn(null)

        val result = useCase.execute(
            LoginCredentials(email = "carlos@fincaelvalle.co", password = "wrongpassword"),
            now = fixedNow
        )

        assertTrue(result.isFailure)
        // Mensaje genérico: no revela si el email existe o no
        assertEquals("Correo o contraseña incorrectos.", result.exceptionOrNull()?.message)
        verify(sessionRepository, never()).saveSession(any())
    }

    @Test
    fun `CA1 - password should be hashed before sending to repository`(): Unit = runBlocking {
        val plainPassword = "mySecurePassword"
        val expectedHash = PasswordService.hash(plainPassword)

        whenever(
            sessionRepository.validateCredentials(any(), eq(expectedHash))
        ).thenReturn(mockUser)
        whenever(sessionRepository.saveSession(any())).thenReturn(true)

        useCase.execute(
            LoginCredentials(email = "carlos@fincaelvalle.co", password = plainPassword),
            now = fixedNow
        )

        // Verifica que NUNCA se pasa la contraseña en texto plano al repositorio
        verify(sessionRepository).validateCredentials(any(), eq(expectedHash))
        verify(sessionRepository, never()).validateCredentials(any(), eq(plainPassword))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-1: Validación de credenciales — login exitoso
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA1 - should return user on successful login`() = runBlocking {
        givenValidCredentials()

        val result = useCase.execute(
            LoginCredentials(email = "carlos@fincaelvalle.co", password = "password123"),
            now = fixedNow
        )

        assertTrue(result.isSuccess)
        val loginResult = result.getOrNull()!!
        assertEquals(mockUser.id, loginResult.user.id)
        assertEquals(mockUser.email, loginResult.user.email)
        assertEquals(mockUser.role, loginResult.user.role)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-2: Persistencia de sesión
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA2 - should persist session after successful login`(): Unit = runBlocking {
        givenValidCredentials()

        useCase.execute(
            LoginCredentials(email = "carlos@fincaelvalle.co", password = "password123"),
            now = fixedNow
        )

        // Verifica que saveSession fue invocado exactamente una vez
        verify(sessionRepository).saveSession(any())
    }

    @Test
    fun `CA2 - session should link to the authenticated user`() = runBlocking {
        givenValidCredentials()

        val result = useCase.execute(
            LoginCredentials(email = "carlos@fincaelvalle.co", password = "password123"),
            now = fixedNow
        )

        val session = result.getOrNull()!!.session
        assertEquals(mockUser.id, session.userId)
    }

    @Test
    fun `CA2 - session should be active after login`() = runBlocking {
        givenValidCredentials()

        val result = useCase.execute(
            LoginCredentials(email = "carlos@fincaelvalle.co", password = "password123"),
            now = fixedNow
        )

        val session = result.getOrNull()!!.session
        assertTrue(session.isActive)
    }

    @Test
    fun `CA2 - session should expire in 30 days`() = runBlocking {
        givenValidCredentials()

        val result = useCase.execute(
            LoginCredentials(email = "carlos@fincaelvalle.co", password = "password123"),
            now = fixedNow
        )

        val session = result.getOrNull()!!.session
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        assertEquals(fixedNow + thirtyDaysMs, session.expiresAt)
    }

    @Test
    fun `CA2 - session token should not be blank`() = runBlocking {
        givenValidCredentials()

        val result = useCase.execute(
            LoginCredentials(email = "carlos@fincaelvalle.co", password = "password123"),
            now = fixedNow
        )

        val token = result.getOrNull()!!.session.token
        assertTrue(token.isNotBlank())
    }

    @Test
    fun `CA2 - should fail if session cannot be persisted`() = runBlocking {
        val passwordHash = PasswordService.hash("password123")
        whenever(sessionRepository.validateCredentials(any(), eq(passwordHash))).thenReturn(mockUser)
        whenever(sessionRepository.saveSession(any())).thenReturn(false) // Falla la persistencia

        val result = useCase.execute(
            LoginCredentials(email = "carlos@fincaelvalle.co", password = "password123"),
            now = fixedNow
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("persistir la sesión") == true)
    }

    @Test
    fun `CA2 - should not save session when credentials are invalid`(): Unit = runBlocking {
        whenever(sessionRepository.validateCredentials(any(), any())).thenReturn(null)

        useCase.execute(
            LoginCredentials(email = "carlos@fincaelvalle.co", password = "wrongpass"),
            now = fixedNow
        )

        // Si el login falla, no se debe crear ninguna sesión
        verify(sessionRepository, never()).saveSession(any())
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private suspend fun givenValidCredentials() {
        val passwordHash = PasswordService.hash("password123")
        whenever(
            sessionRepository.validateCredentials(
                eq("carlos@fincaelvalle.co"),
                eq(passwordHash)
            )
        ).thenReturn(mockUser)
        whenever(sessionRepository.saveSession(any())).thenReturn(true)
    }
}