package com.ganadeia.app.infrastructure.persistence.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animals")
data class AnimalEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val name: String,
    val type: String,
    val breed: String,
    val hardiness: String,
    val weight: Double,
    val birthDate: Long,
    val purpose: String,
    val status: String,
    val nextFollowUpDate: Long?,
    val photoPath: String? = null
)