package com.ganadeia.app.data.repository

import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.domain.port.driven.repository.UserRepository
import com.ganadeia.app.infrastructure.persistence.room.dao.UserDao
import com.ganadeia.app.infrastructure.persistence.room.entity.UserEntity

/**
 * Implementación concreta de [UserRepository] usando Room.
 *
 * Responsabilidades:
 * 1. Mapear [User] ↔ [UserEntity] con nombres explícitos para evitar
 *    ambigüedad de resolución (lección aprendida en RoomSessionRepository).
 * 2. Encapsular todas las excepciones de Room: el dominio nunca ve SQLiteException.
 * 3. Recibir el [passwordHash] como parámetro separado en [saveUser] para
 *    mantener el modelo de dominio [User] libre de detalles de seguridad.
 */
class RoomUserRepository(
    private val userDao: UserDao
) : UserRepository {

    // ── Lectura ───────────────────────────────────────────────────────────────

    override suspend fun getUserById(userId: String): User? {
        return try {
            userDao.findById(userId)?.toUser()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun existsByEmail(email: String): Boolean {
        return try {
            userDao.findByEmail(email) != null
        } catch (e: Exception) {
            // En caso de error de BD, preferimos bloquear el registro antes
            // que crear un duplicado silencioso.
            true
        }
    }

    // ── Escritura ─────────────────────────────────────────────────────────────

    override suspend fun saveUser(user: User, passwordHash: String): Boolean {
        return try {
            userDao.insertUser(user.toEntity(passwordHash))
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateProfile(user: User): Boolean {
        return try {
            // Para actualizar el perfil necesitamos el hash existente.
            // Lo recuperamos de la BD antes de sobrescribir.
            val existing = userDao.findById(user.id) ?: return false
            userDao.updateUser(user.toEntity(existing.passwordHash))
            true
        } catch (e: Exception) {
            false
        }
    }

    // ── Mappers privados ──────────────────────────────────────────────────────
    // Nombres explícitos para evitar "Overload resolution ambiguity".

    private fun UserEntity.toUser() = User(
        id = id,
        name = name,
        email = email,
        role = UserRole.valueOf(role),
        ranchName = ranchName,
        location = location,
        permissions = permissions,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun User.toEntity(passwordHash: String) = UserEntity(
        id = id,
        name = name,
        email = email,
        passwordHash = passwordHash,
        role = role.name,
        ranchName = ranchName,
        location = location,
        permissions = permissions,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}