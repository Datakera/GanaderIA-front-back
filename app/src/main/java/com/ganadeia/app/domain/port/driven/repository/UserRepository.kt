package com.ganadeia.app.domain.port.driven.repository

import com.ganadeia.app.domain.model.User

/**
 * Puerto de salida (driven port) que define el contrato de persistencia
 * para los usuarios del sistema.
 *
 * Extiende el contrato original de [getUserById] / [updateProfile] que ya
 * existía, agregando las operaciones necesarias para el registro:
 * - [saveUser]:       persiste un usuario nuevo con su hash de contraseña.
 * - [existsByEmail]:  verifica unicidad del correo antes de crear la cuenta (CA-3).
 */
interface UserRepository {

    /** Devuelve el usuario por ID, o null si no existe. */
    suspend fun getUserById(userId: String): User?

    /** Actualiza el perfil (nombre, finca, ubicación) de un usuario existente. */
    suspend fun updateProfile(user: User): Boolean

    /**
     * Persiste un nuevo usuario junto con su contraseña hasheada.
     *
     * El hash de contraseña se recibe separado del modelo [User] de dominio
     * para mantener [User] limpio de detalles de seguridad: el dominio no
     * necesita conocer el hash, solo la capa de infraestructura lo almacena.
     *
     * @param user          Datos del nuevo usuario (sin contraseña).
     * @param passwordHash  Hash SHA-256 de la contraseña.
     * @return `true` si el guardado fue exitoso.
     */
    suspend fun saveUser(user: User, passwordHash: String): Boolean

    /**
     * Verifica si ya existe un usuario registrado con ese correo.
     * Implementa el CA-3: correo único en el sistema.
     *
     * @param email Correo ya normalizado (trimmed + lowercase).
     * @return `true` si el correo ya está en uso.
     */
    suspend fun existsByEmail(email: String): Boolean
}