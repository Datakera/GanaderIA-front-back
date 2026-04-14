package com.ganadeia.app.application

import com.ganadeia.app.domain.port.driven.repository.SessionRepository

/**
 * CASO DE USO: Cerrar Sesión
 *
 * Invalida la sesión activa para que al próximo arranque de la app el
 * ganadero deba autenticarse de nuevo. Se invoca desde el menú de Perfil.
 */
class LogoutUseCase(
    private val sessionRepository: SessionRepository
) {
    /**
     * @return [Result.success] si la sesión fue limpiada correctamente,
     *         o [Result.failure] si ocurrió un error al limpiar la persistencia.
     */
    suspend fun execute(): Result<Unit> {
        val cleared = sessionRepository.clearSession()
        return if (cleared) Result.success(Unit)
        else Result.failure(RuntimeException("No se pudo cerrar la sesión correctamente."))
    }
}