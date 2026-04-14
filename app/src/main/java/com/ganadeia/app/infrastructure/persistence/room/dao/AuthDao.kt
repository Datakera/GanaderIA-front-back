package com.ganadeia.app.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ganadeia.app.infrastructure.persistence.room.entity.SessionEntity

@Dao
interface SessionDao {

    /**
     * REPLACE garantiza que solo exista una sesión activa a la vez.
     * Al hacer login de nuevo, la sesión anterior se sobreescribe.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    /**
     * Recupera la sesión activa más reciente.
     * ORDER BY createdAt DESC + LIMIT 1 asegura que devolvemos la última.
     */
    @Query("SELECT * FROM sessions WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    /**
     * "Cierra sesión" marcando la sesión como inactiva (soft-delete).
     * El registro permanece para auditoría futura.
     */
    @Query("UPDATE sessions SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAllSessions()
}