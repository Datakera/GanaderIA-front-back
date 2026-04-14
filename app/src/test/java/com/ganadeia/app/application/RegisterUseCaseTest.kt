package com.ganadeia.app.application

import com.ganadeia.app.domain.model.Session
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.domain.port.driven.repository.SessionRepository
import com.ganadeia.app.domain.port.driven.repository.UserRepository
import com.ganadeia.app.domain.service.PasswordService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class RegisterUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var useCase: RegisterUseCase

    private val fixedNow = 1_700_000_000_000L

    @Before
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        sessionRepository = mock(SessionRepository::class.java)
        useCase = RegisterUseCase(userRepository, sessionRepository)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-1: Campos obligatorios — Nombre
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA1 - should fail when name is blank`(): Unit = runBlocking {
        val result = useCase.execute(
            validRequest().copy(name = "   "),
            now = fixedNow
        )

        assertTrue(result.isFailure)
        assertEquals("El nombre es obligatorio.", result.exceptionOrNull()?.message)
        verify(userRepository, never()).saveUser(any(), any())
    }

    @Test
    fun `CA1 - should fail when name is empty`() = runBlocking {
        val result = useCase.execute(validRequest().copy(name = ""), now = fixedNow)

        assertTrue(result.isFailure)
        assertEquals("El nombre es obligatorio.", result.exceptionOrNull()?.message)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-1: Campos obligatorios — Correo
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA1 - should fail when email is blank`() = runBlocking {
        val result = useCase.execute(validRequest().copy(email = "  "), now = fixedNow)

        assertTrue(result.isFailure)
        assertEquals("El correo electrónico es obligatorio.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `CA1 - should fail when email has invalid format`() = runBlocking {
        val result = useCase.execute(validRequest().copy(email = "not-an-email"), now = fixedNow)

        assertTrue(result.isFailure)
        assertEquals(
            "El formato del correo electrónico no es válido.",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `CA1 - email should be normalized to lowercase before saving`() = runBlocking {
        givenEmailIsAvailable()
        givenSaveSucceeds()
        givenSessionSaves()

        useCase.execute(validRequest().copy(email = "CARLOS@FincaElValle.CO"), now = fixedNow)

        val captor = argumentCaptor<User>()
        verify(userRepository).saveUser(captor.capture(), any())
        assertEquals("carlos@fincaelvalle.co", captor.firstValue.email)
    }

    @Test
    fun `CA1 - name should be trimmed before saving`() = runBlocking {
        givenEmailIsAvailable()
        givenSaveSucceeds()
        givenSessionSaves()

        useCase.execute(validRequest().copy(name = "  Carlos Rodríguez  "), now = fixedNow)

        val captor = argumentCaptor<User>()
        verify(userRepository).saveUser(captor.capture(), any())
        assertEquals("Carlos Rodríguez", captor.firstValue.name)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-1: Campos obligatorios — Contraseña (obligatoriedad básica)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA1 - should fail when password is blank`() = runBlocking {
        val result = useCase.execute(validRequest().copy(password = ""), now = fixedNow)

        assertTrue(result.isFailure)
        assertEquals("La contraseña es obligatoria.", result.exceptionOrNull()?.message)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-2: Contraseña mínimo 8 caracteres
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA2 - should fail when password has less than 8 characters`(): Unit = runBlocking {
        val result = useCase.execute(validRequest().copy(password = "1234567"), now = fixedNow)

        assertTrue(result.isFailure)
        assertEquals(
            "La contraseña debe tener al menos 8 caracteres.",
            result.exceptionOrNull()?.message
        )
        verify(userRepository, never()).saveUser(any(), any())
    }

    @Test
    fun `CA2 - should fail when password has exactly 7 characters`() = runBlocking {
        val result = useCase.execute(validRequest().copy(password = "Abc1234"), now = fixedNow)

        assertTrue(result.isFailure)
    }

    @Test
    fun `CA2 - should succeed when password has exactly 8 characters`() = runBlocking {
        givenEmailIsAvailable()
        givenSaveSucceeds()
        givenSessionSaves()

        val result = useCase.execute(validRequest().copy(password = "Abc12345"), now = fixedNow)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `CA2 - password should be hashed before being saved`(): Unit = runBlocking {
        givenEmailIsAvailable()
        givenSaveSucceeds()
        givenSessionSaves()

        val plainPassword = "securePass99"
        val expectedHash = PasswordService.hash(plainPassword)

        useCase.execute(validRequest().copy(password = plainPassword), now = fixedNow)

        // El repositorio debe recibir el HASH, nunca la contraseña en texto plano
        verify(userRepository).saveUser(any(), eq(expectedHash))
        verify(userRepository, never()).saveUser(any(), eq(plainPassword))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CA-3: Correo ya registrado
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `CA3 - should fail when email is already registered`() = runBlocking {
        whenever(userRepository.existsByEmail(any())).thenReturn(true)

        val result = useCase.execute(validRequest(), now = fixedNow)

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("ya está registrado") == true
        )
    }

    @Test
    fun `CA3 - error message should include the duplicate email`() = runBlocking {
        whenever(userRepository.existsByEmail(any())).thenReturn(true)

        val result = useCase.execute(
            validRequest().copy(email = "duplicado@finca.co"),
            now = fixedNow
        )

        assertTrue(result.exceptionOrNull()?.message?.contains("duplicado@finca.co") == true)
    }

    @Test
    fun `CA3 - should not save user when email already exists`(): Unit = runBlocking {
        whenever(userRepository.existsByEmail(any())).thenReturn(true)

        useCase.execute(validRequest(), now = fixedNow)

        verify(userRepository, never()).saveUser(any(), any())
        verify(sessionRepository, never()).saveSession(any())
    }

    @Test
    fun `CA3 - email uniqueness check uses normalized email`(): Unit = runBlocking {
        givenEmailIsAvailable()
        givenSaveSucceeds()
        givenSessionSaves()

        useCase.execute(validRequest().copy(email = "CARLOS@FINCA.CO"), now = fixedNow)

        // La verificación de unicidad debe hacerse con el email en minúsculas
        verify(userRepository).existsByEmail(eq("carlos@finca.co"))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Registro exitoso — comportamiento esperado
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `should return user with RANCHER role on success`() = runBlocking {
        givenEmailIsAvailable()
        givenSaveSucceeds()
        givenSessionSaves()

        val result = useCase.execute(validRequest(), now = fixedNow)

        assertTrue(result.isSuccess)
        assertEquals(UserRole.RANCHER, result.getOrNull()!!.user.role)
    }

    @Test
    fun `should auto-open session after successful registration`() = runBlocking {
        givenEmailIsAvailable()
        givenSaveSucceeds()
        givenSessionSaves()

        val result = useCase.execute(validRequest(), now = fixedNow)

        assertTrue(result.isSuccess)
        val session = result.getOrNull()!!.session
        assertTrue(session.isActive)
        assertEquals(fixedNow + 30L * 24 * 60 * 60 * 1000, session.expiresAt)
    }

    @Test
    fun `should link session to the new user id`() = runBlocking {
        givenEmailIsAvailable()
        givenSaveSucceeds()
        givenSessionSaves()

        val result = useCase.execute(validRequest(), now = fixedNow)

        val user = result.getOrNull()!!.user
        val session = result.getOrNull()!!.session
        assertEquals(user.id, session.userId)
    }

    @Test
    fun `should fail gracefully when user save fails`(): Unit = runBlocking {
        givenEmailIsAvailable()
        whenever(userRepository.saveUser(any(), any())).thenReturn(false)

        val result = useCase.execute(validRequest(), now = fixedNow)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("guardar el usuario") == true)
        verify(sessionRepository, never()).saveSession(any())
    }

    @Test
    fun `should return specific message when session save fails after user created`() = runBlocking {
        givenEmailIsAvailable()
        givenSaveSucceeds()
        whenever(sessionRepository.saveSession(any())).thenReturn(false)

        val result = useCase.execute(validRequest(), now = fixedNow)

        assertTrue(result.isFailure)
        // El usuario YA fue creado; el mensaje debe indicar que puede hacer login
        assertTrue(
            result.exceptionOrNull()?.message?.contains("inicia sesión manualmente") == true
        )
    }

    @Test
    fun `optional ranchName should be saved when provided`() = runBlocking {
        givenEmailIsAvailable()
        givenSaveSucceeds()
        givenSessionSaves()

        useCase.execute(
            validRequest().copy(ranchName = "Finca El Valle"),
            now = fixedNow
        )

        val captor = argumentCaptor<User>()
        verify(userRepository).saveUser(captor.capture(), any())
        assertEquals("Finca El Valle", captor.firstValue.ranchName)
    }

    @Test
    fun `optional ranchName should be null when not provided`() = runBlocking {
        givenEmailIsAvailable()
        givenSaveSucceeds()
        givenSessionSaves()

        useCase.execute(validRequest().copy(ranchName = null), now = fixedNow)

        val captor = argumentCaptor<User>()
        verify(userRepository).saveUser(captor.capture(), any())
        assertNull(captor.firstValue.ranchName)
    }

    @Test
    fun `ranchName with only whitespace should be saved as null`() = runBlocking {
        givenEmailIsAvailable()
        givenSaveSucceeds()
        givenSessionSaves()

        useCase.execute(validRequest().copy(ranchName = "   "), now = fixedNow)

        val captor = argumentCaptor<User>()
        verify(userRepository).saveUser(captor.capture(), any())
        assertNull(captor.firstValue.ranchName)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun validRequest() = RegisterRequest(
        name = "Carlos Rodríguez",
        email = "carlos@fincaelvalle.co",
        password = "securePass99",
        ranchName = null
    )

    private suspend fun givenEmailIsAvailable() {
        whenever(userRepository.existsByEmail(any())).thenReturn(false)
    }

    private suspend fun givenSaveSucceeds() {
        whenever(userRepository.saveUser(any(), any())).thenReturn(true)
    }

    private suspend fun givenSessionSaves() {
        whenever(sessionRepository.saveSession(any())).thenReturn(true)
    }
}