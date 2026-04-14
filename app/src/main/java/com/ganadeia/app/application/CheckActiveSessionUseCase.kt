package com.ganadeia.app.application

import com.ganadeia.app.domain.model.Session
import com.ganadeia.app.domain.port.driven.repository.SessionRepository

/**
 * CASO DE USO: Verificar Sesión Activa al Arranque
 *
 * Este use case es el responsable de implementar el CA-2 desde la perspectiva
 * del arranque de la app: "La sesión debe mantenerse aunque se cierre la app."
 *
 * Se invoca desde el [LoginViewModel] (o un SplashViewModel) cuando la
 * aplicación inicia, antes de mostrar cualquier pantalla. Si hay una sesión
 * válida y no expirada, la app navega directamente al Dashboard; si no,
 * muestra la pantalla de Login.
 *
 * Separar esta lógica del [LoginUseCase] respeta el Principio de
 * Responsabilidad Única: uno maneja el flujo activo de login, el otro
 * el chequeo pasivo de persistencia.
 */
class CheckActiveSessionUseCase(
    private val sessionRepository: SessionRepository
) {
    /**
     * @param now Timestamp actual (inyectable para tests deterministas).
     * @return [Session] si hay una sesión vigente, null en caso contrario.
     */
    suspend fun execute(now: Long = System.currentTimeMillis()): Session? {
        val session = sessionRepository.getActiveSession() ?: return null

        // Doble validación: la BD dice que está activa Y el token no expiró
        return if (session.isActive && session.expiresAt > now) session else null
    }
}