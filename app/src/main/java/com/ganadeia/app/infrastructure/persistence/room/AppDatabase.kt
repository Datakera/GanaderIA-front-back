package com.ganadeia.app.infrastructure.persistence.room

import com.ganadeia.app.infrastructure.persistence.room.dao.AiRecommendationDao
import com.ganadeia.app.infrastructure.persistence.room.dao.AnimalDao
import com.ganadeia.app.infrastructure.persistence.room.entity.AiRecommendationEntity
import com.ganadeia.app.infrastructure.persistence.room.entity.AnimalEntity
import androidx.room.Database
import androidx.room.RoomDatabase
import com.ganadeia.app.infrastructure.persistence.room.dao.HealthCheckDao
import com.ganadeia.app.infrastructure.persistence.room.dao.SessionDao
import com.ganadeia.app.infrastructure.persistence.room.dao.UserDao
import com.ganadeia.app.infrastructure.persistence.room.dao.VaccinationDao
import com.ganadeia.app.infrastructure.persistence.room.entity.HealthCheckEntity
import com.ganadeia.app.infrastructure.persistence.room.entity.SessionEntity
import com.ganadeia.app.infrastructure.persistence.room.entity.UserEntity
import com.ganadeia.app.infrastructure.persistence.room.entity.VaccinationEntity

/**
 * Base de datos Room centralizada.
 *
 * ⚠️ REGLA DE EQUIPO: Cada vez que se agregue o modifique una Entity,
 * se debe incrementar [version] Y proveer una [Migration] para no perder
 * datos de usuarios en producción. Para desarrollo local puedes usar
 * fallbackToDestructiveMigration() en el Builder, pero NUNCA en release.
 *
 * Versión actual: 2
 * Cambios v1 → v2: Se agregan tablas `users` y `sessions` para autenticación.
 */

@Database(
    entities = [
        AnimalEntity::class,
        HealthCheckEntity::class,
        VaccinationEntity::class,
        AiRecommendationEntity::class,
        UserEntity::class,       // ← nueva tabla
        SessionEntity::class    // ← nueva tabla
    ],
    version = 5,                        // ← v5: add medicalRecommendation + vaccineRecommendation columns
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animalDao(): AnimalDao
    abstract fun healthCheckDao(): HealthCheckDao
    abstract fun vaccinationDao(): VaccinationDao
    abstract fun aiRecommendationDao(): AiRecommendationDao
    abstract fun userDao(): UserDao // ← nuevo DAO
    abstract fun sessionDao(): SessionDao // ← nuevo DAO
}