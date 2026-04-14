package com.ganadeia.app.infrastructure.persistence.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que persiste al Usuario localmente.
 *
 * ⚠️ Nota: [passwordHash] solo se usa para la validación offline.
 * En una integración con API REST, este campo puede quedar vacío o
 * eliminarse, delegando la validación al servidor y guardando solo
 * el token de respuesta.
 *
 * El campo [rol] almacena el nombre del enum [UserRole] como String
 * para legibilidad en debugging.
 */
@Entity(tableName = "users",
        indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: String,             // UserRole.name
    val ranchName: String?,
    val location: String?,
    val permissions: String?,
    val createdAt: Long,
    val updatedAt: Long
)