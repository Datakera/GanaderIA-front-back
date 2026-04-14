package com.ganadeia.app.domain.port.driven.repository

import com.ganadeia.app.domain.model.Session
import com.ganadeia.app.domain.model.User

/**
 * Puerto de salida (driven port) que define el contrato de persistencia
 * y autenticación de sesiones de usuario.
 *
 * Responsabilidades separadas por intención:
 * - [validateCredentials]: delega la verificación del hash de contraseña
 *   contra la BD local. En producción, esto puede ser reemplazado por
 *   una llamada a una API REST sin cambiar el contrato.
 * - [saveSession]: persiste el token JWT para que sobreviva al cierre de la app.
 * - [getActiveSession]: permite al sistema recuperar la sesión al arranque
 *   sin que el ganadero tenga que volver a ingresar sus datos.
 */
interface SessionRepository {

    /**
     * Verifica que el email y password_hash correspondan a un usuario registrado.
     *
     * @return El [User] autenticado, o null si las credenciales no son válidas.
     */
    suspend fun validateCredentials(email: String, passwordHash: String): User?

    /**
     * Persiste la sesión activa localmente (SharedPreferences / DataStore).
     *
     * @return `true` si el guardado fue exitoso.
     */
    suspend fun saveSession(session: Session): Boolean

    /**
     * Devuelve la sesión activa si existe y no ha expirado.
     * Retorna null si no hay sesión o si el token ya expiró.
     */
    suspend fun getActiveSession(): Session?

    /**
     * Invalida la sesión activa (cierre de sesión manual).
     *
     * @return `true` si la invalidación fue exitosa.
     */
    suspend fun clearSession(): Boolean
}