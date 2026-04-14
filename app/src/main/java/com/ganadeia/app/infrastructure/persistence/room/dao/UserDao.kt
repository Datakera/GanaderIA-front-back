package com.ganadeia.app.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ganadeia.app.infrastructure.persistence.room.entity.UserEntity

@Dao
interface UserDao {

    /**
     * Inserta un usuario nuevo. ABORT lanza excepción si el email ya existe,
     * lo que protege la unicidad a nivel de BD (refuerza el CA-3 del use case).
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    /**
     * Actualiza los campos mutables del perfil (nombre, finca, ubicación).
     * La contraseña y el email no se actualizan por esta vía.
     */
    @Update
    suspend fun updateUser(user: UserEntity)

    /**
     * Consulta de autenticación: email + hash deben coincidir exactamente.
     * Implementa el CA-1 del LoginUseCase.
     */
    @Query("SELECT * FROM users WHERE email = :email AND passwordHash = :passwordHash LIMIT 1")
    suspend fun findByEmailAndPassword(email: String, passwordHash: String): UserEntity?

    /** Búsqueda por email para verificar unicidad antes del registro (CA-3). */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    /** Búsqueda por ID para casos de uso de perfil. */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): UserEntity?
}