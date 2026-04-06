package com.ganadeia.app.infrastructure.persistence.room

import com.ganadeia.app.infrastructure.persistence.room.dao.AnimalDao
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
        VaccinationEntity::class   // ← registrada aquí
    ],
    version = 2,                   // ← incrementar versión por nueva tabla
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animalDao(): AnimalDao
    abstract fun healthCheckDao(): HealthCheckDao
    abstract fun vaccinationDao(): VaccinationDao  // ← nuevo DAO expuesto
}
