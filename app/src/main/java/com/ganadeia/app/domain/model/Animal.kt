package com.ganadeia.app.domain.model

data class Animal(
    val id: String,
    val name: String,
    val breed: String,
    val currentWeight: Double,
    val birthDate: Long,
    val purpose: AnimalPurpose,
    val status: AnimalStatus,
    val nextFollowUpDate: Long?
)

enum class AnimalPurpose { MEAT, MILK, BREEDING, DUAL_PURPOSE }
enum class AnimalStatus { ACTIVE, INACTIVE, SOLD }
