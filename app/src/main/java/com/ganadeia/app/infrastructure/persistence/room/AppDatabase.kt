package com.ganadeia.app.infrastructure.persistence.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ganadeia.app.infrastructure.persistence.room.dao.AnimalDao
import com.ganadeia.app.infrastructure.persistence.room.entity.AnimalEntity

@Database(entities = [AnimalEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animalDao(): AnimalDao
}