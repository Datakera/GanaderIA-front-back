package com.ganadeia.app.application

import com.ganadeia.app.domain.model.LoginCredentials
import com.ganadeia.app.domain.model.Session
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.port.driven.repository.SessionRepository
import com.ganadeia.app.domain.service.PasswordService
import com.ganadeia.app.infrastructure.monitoring.AnalyticsReporter
import java.util.UUID

/**
 * CASO DE USO: Iniciar Sesión
 *
 * Historia de Usuario:
 * "Como ganadero registrado, quiero iniciar sesión en la aplicación,
 * para acceder a mi historial y registrar nuevos datos."
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * Criterios de Aceptación implementados:
 *
 * CA-1 Validación de credenciales:
 *      El use case verifica que el email tenga formato válido y que la
 *      contraseña no esté vacía antes de consultar la BD. Si las
 *      credenciales no coinciden con ningún usuario, devuelve un error
 *      genérico (sin revelar si el email existe o no — principio de seguridad).
 *
 * CA-2 Persistencia de sesión (token básico):
 *      Tras un login exitoso, se genera y persiste un [Session] con un
 *      token único y una fecha de expiración (30 días). La próxima vez
 *      que la app arranque, el sistema puede recuperar la sesión activa
 *      mediante [SessionRepository.getActiveSession] sin pedirle al
 *      ganadero que inicie sesión de nuevo.
 * ──────────────────────────────────────────────────────────────────────────────
 */
class LoginUseCase(
    private val sessionRepository: SessionRepository,
    private val passwordService: PasswordService = PasswordService
) {

    companion object {
        // 30 días expresados en milisegundos
        private const val SESSION_DURATION_MS = 30L * 24 * 60 * 60 * 1000

        // Regex simplificado pero suficiente para una validación de dominio
        private val EMAIL_REGEX = Regex("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")
    }

    /**
     * Ejecuta el flujo completo de autenticación.
     *
     * @param credentials Email y contraseña ingresados por el ganadero.
     * @param now          Timestamp de referencia (inyectable para tests deterministas).
     *
     * @return [Result.success] con un [LoginResult] que contiene el usuario y
     *         la sesión persistida, o [Result.failure] con el motivo del error.
     */
    suspend fun execute(
        credentials: LoginCredentials,
        now: Long = System.currentTimeMillis()
    ): Result<LoginResult> {

        // ── Validaciones de entrada ────────────────────────────────────────────
        val emailTrimmed = credentials.email.trim()
        if (emailTrimmed.isBlank()) {
            return Result.failure(
                IllegalArgumentException("El correo electrónico es obligatorio.")
            )
        }
        if (!EMAIL_REGEX.matches(emailTrimmed)) {
            return Result.failure(
                IllegalArgumentException("El formato del correo electrónico no es válido.")
            )
        }
        if (credentials.password.isBlank()) {
            return Result.failure(
                IllegalArgumentException("La contraseña es obligatoria.")
            )
        }

        // ── CA-1: Verificar credenciales contra la BD ─────────────────────────
        val passwordHash = passwordService.hash(credentials.password)
        val user = sessionRepository.validateCredentials(emailTrimmed, passwordHash)
            ?: return Result.failure(
                // Mensaje genérico intencional: no revelar si el email existe
                IllegalStateException("Correo o contraseña incorrectos.")
            )

        // ── CA-2: Generar y persistir la sesión ───────────────────────────────
        val session = Session(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            token = UUID.randomUUID().toString(), // En producción: JWT firmado
            createdAt = now,
            expiresAt = now + SESSION_DURATION_MS,
            isActive = true
        )

        val saved = sessionRepository.saveSession(session)
        if (!saved) {
            return Result.failure(
                RuntimeException("No se pudo persistir la sesión. Intenta de nuevo.")
            )
        }

        AnalyticsReporter.logLogin()

        return Result.success(LoginResult(user = user, session = session))
    }
}

/**
 * Resultado devuelto a la capa de presentación (ViewModel) tras un login exitoso.
 * Contiene lo mínimo para que la UI decida a qué pantalla navegar.
 */
data class LoginResult(
    val user: User,
    val session: Session
)