package com.ganadeia.app.infrastructure.persistence.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que persiste localmente la sesión del usuario.
 *
 * Decisiones de diseño:
 * - Solo se guarda UNA sesión a la vez (la activa). La estrategia REPLACE
 *   en el DAO se encarga de reemplazar la anterior en caso de un nuevo login.
 * - El token se guarda como String. En producción, considera cifrar este
 *   campo usando SQLCipher o EncryptedSharedPreferences para mayor seguridad.
 * - [isActive] permite hacer "soft-delete" de la sesión (logout) sin
 *   eliminar el registro, lo que facilita auditorías futuras.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val token: String,
    val createdAt: Long,
    val expiresAt: Long,
    val isActive: Boolean
)