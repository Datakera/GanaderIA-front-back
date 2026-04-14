package com.ganadeia.app.application

import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.domain.port.driven.repository.UserRepository
import com.ganadeia.app.domain.port.driven.repository.SessionRepository
import com.ganadeia.app.domain.service.PasswordService
import java.util.UUID

/**
 * CASO DE USO: Registrar nuevo ganadero
 *
 * Historia de Usuario:
 * "Como pequeño productor ganadero, quiero crear una cuenta con mi correo
 * electrónico y una contraseña, para tener un perfil único donde se guarden
 * los datos de mis animales."
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * Criterios de Aceptación implementados:
 *
 * CA-1 Campos obligatorios:
 *      El formulario debe pedir Nombre, Correo y Contraseña. Ninguno puede
 *      estar vacío. El nombre y el correo se normalizan (trim) antes de
 *      procesarse.
 *
 * CA-2 Contraseña segura:
 *      La contraseña debe tener al menos 8 caracteres. Se hashea con
 *      [PasswordService] antes de persistirse — nunca se guarda en texto plano.
 *
 * CA-3 Correo único:
 *      Si el correo ya está registrado en la BD local, se devuelve un error
 *      descriptivo. No se crea un usuario duplicado.
 * ──────────────────────────────────────────────────────────────────────────────
 */
class RegisterUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val passwordService: PasswordService = PasswordService
) {

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private val EMAIL_REGEX = Regex("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")
        private const val SESSION_DURATION_MS = 30L * 24 * 60 * 60 * 1000
    }

    /**
     * Ejecuta el registro completo: valida, persiste el usuario y abre
     * sesión automáticamente para que el ganadero no tenga que hacer
     * login justo después de registrarse.
     *
     * @param request  Datos ingresados en el formulario de registro.
     * @param now      Timestamp inyectable para tests deterministas.
     *
     * @return [Result.success] con [RegisterResult] (usuario + sesión activa),
     *         o [Result.failure] con el primer error de validación encontrado.
     */
    suspend fun execute(
        request: RegisterRequest,
        now: Long = System.currentTimeMillis()
    ): Result<RegisterResult> {

        // ── CA-1: Validar campos obligatorios ─────────────────────────────────
        val nameTrimmed = request.name.trim()
        if (nameTrimmed.isBlank()) {
            return Result.failure(
                IllegalArgumentException("El nombre es obligatorio.")
            )
        }

        val emailTrimmed = request.email.trim().lowercase()
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

        // ── CA-2: Validar contraseña ──────────────────────────────────────────
        if (request.password.isBlank()) {
            return Result.failure(
                IllegalArgumentException("La contraseña es obligatoria.")
            )
        }
        if (request.password.length < MIN_PASSWORD_LENGTH) {
            return Result.failure(
                IllegalArgumentException(
                    "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres."
                )
            )
        }

        // ── CA-3: Verificar que el correo no esté registrado ──────────────────
        val emailAlreadyExists = userRepository.existsByEmail(emailTrimmed)
        if (emailAlreadyExists) {
            return Result.failure(
                IllegalStateException(
                    "El correo '$emailTrimmed' ya está registrado. " +
                            "Inicia sesión o usa otro correo."
                )
            )
        }

        // ── Crear y persistir el nuevo usuario ────────────────────────────────
        val newUser = User(
            id = UUID.randomUUID().toString(),
            name = nameTrimmed,
            email = emailTrimmed,
            role = UserRole.RANCHER, // El registro público siempre crea un GANADERO
            ranchName = request.ranchName?.trim()?.ifBlank { null },
            location = null,
            permissions = null,
            createdAt = now,
            updatedAt = now
        )

        val passwordHash = passwordService.hash(request.password)
        val saved = userRepository.saveUser(newUser, passwordHash)
        if (!saved) {
            return Result.failure(
                RuntimeException("No se pudo guardar el usuario. Intenta de nuevo.")
            )
        }

        // ── Abrir sesión automáticamente tras el registro ─────────────────────
        // Evita que el ganadero tenga que hacer login justo después de registrarse
        val session = com.ganadeia.app.domain.model.Session(
            id = UUID.randomUUID().toString(),
            userId = newUser.id,
            token = UUID.randomUUID().toString(),
            createdAt = now,
            expiresAt = now + SESSION_DURATION_MS,
            isActive = true
        )

        val sessionSaved = sessionRepository.saveSession(session)
        if (!sessionSaved) {
            // El usuario ya fue creado; la sesión fallida no debe revertirlo.
            // Devolvemos un error específico para que la UI lo redirija al Login.
            return Result.failure(
                RuntimeException(
                    "Cuenta creada, pero no se pudo iniciar sesión automáticamente. " +
                            "Por favor inicia sesión manualmente."
                )
            )
        }

        return Result.success(RegisterResult(user = newUser, session = session))
    }
}

/**
 * Datos del formulario de registro ingresados por el ganadero.
 *
 * @param name       Nombre completo. Obligatorio (CA-1).
 * @param email      Correo electrónico. Obligatorio y único (CA-1, CA-3).
 * @param password   Contraseña en texto plano. Mínimo 8 caracteres (CA-2).
 *                   El use case se encarga de hashearla antes de persistirla.
 * @param ranchName  Nombre de la finca. Opcional — puede completarse después
 *                   desde el perfil.
 */
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val ranchName: String? = null
)

/**
 * Resultado devuelto al ViewModel tras un registro exitoso.
 * Incluye la sesión para que la UI navegue directamente al Dashboard.
 */
data class RegisterResult(
    val user: User,
    val session: com.ganadeia.app.domain.model.Session
)