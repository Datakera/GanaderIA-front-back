package com.ganadeia.app.data.repository

import com.ganadeia.app.domain.model.Session
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.domain.port.driven.repository.SessionRepository
import com.ganadeia.app.infrastructure.persistence.room.dao.SessionDao
import com.ganadeia.app.infrastructure.persistence.room.dao.UserDao
import com.ganadeia.app.infrastructure.persistence.room.entity.SessionEntity
import com.ganadeia.app.infrastructure.persistence.room.entity.UserEntity

/**
 * Implementación concreta de [SessionRepository] usando Room.
 *
 * Responsabilidades:
 * 1. Delegar la consulta de credenciales al [UserDao].
 * 2. Mapear [Session] ↔ [SessionEntity] y [User] ↔ [UserEntity].
 * 3. Manejar errores de la BD sin propagar excepciones crudas al dominio.
 */
class RoomSessionRepository(
    private val userDao: UserDao,
    private val sessionDao: SessionDao
) : SessionRepository {

    // ── CA-1: Validación de credenciales ──────────────────────────────────────

    override suspend fun validateCredentials(email: String, passwordHash: String): User? {
        return try {
            userDao.findByEmailAndPassword(email, passwordHash)?.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    // ── CA-2: Persistencia de sesión ──────────────────────────────────────────

    override suspend fun saveSession(session: Session): Boolean {
        return try {
            sessionDao.insertSession(session.toEntity())
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getActiveSession(): Session? {
        return try {
            sessionDao.getActiveSession()?.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearSession(): Boolean {
        return try {
            sessionDao.deactivateAllSessions()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ── Mappers privados ──────────────────────────────────────────────────────

    private fun UserEntity.toDomain() = User(
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

    private fun Session.toEntity() = SessionEntity(
        id = id,
        userId = userId,
        token = token,
        createdAt = createdAt,
        expiresAt = expiresAt,
        isActive = isActive
    )

    private fun SessionEntity.toDomain() = Session(
        id = id,
        userId = userId,
        token = token,
        createdAt = createdAt,
        expiresAt = expiresAt,
        isActive = isActive
    )
}