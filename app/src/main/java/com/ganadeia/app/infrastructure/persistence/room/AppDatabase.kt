package com.ganadeia.app.infrastructure.persistence.room

import com.ganadeia.app.infrastructure.persistence.room.dao.AiRecommendationDao
import com.ganadeia.app.infrastructure.persistence.room.dao.AnimalDao
import com.ganadeia.app.infrastructure.persistence.room.entity.AiRecommendationEntity
import com.ganadeia.app.infrastructure.persistence.room.entity.AnimalEntity
import androidx.room.Database
import androidx.room.RoomDatabase
import com.ganadeia.app.infrastructure.persistence.room.dao.HealthCheckDao
import com.ganadeia.app.infrastructure.persistence.room.dao.VaccinationDao
import com.ganadeia.app.infrastructure.persistence.room.entity.HealthCheckEntity
import com.ganadeia.app.infrastructure.persistence.room.entity.VaccinationEntity

@Database(
    entities = [
        AnimalEntity::class,
        HealthCheckEntity::class,
        VaccinationEntity::class,
        AiRecommendationEntity::class   // ← nueva tabla
    ],
    version = 3,                        // ← incrementar por nueva tabla ai_recommendations
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animalDao(): AnimalDao
    abstract fun healthCheckDao(): HealthCheckDao
    abstract fun vaccinationDao(): VaccinationDao
    abstract fun aiRecommendationDao(): AiRecommendationDao  // ← nuevo DAO
}