package com.ganadeia.app.domain.model

enum class AnimalType { BOVINE, SWINE, POULTRY, CAPRINE, EQUINE }

enum class BreedHardiness { LOW, MEDIUM, HIGH } // Para el FollowUpService

data class Animal(
    val id: String,
    val userId: String,
    val name: String,
    val type: AnimalType,
    val breed: String,
    val hardiness: BreedHardiness,
    val currentWeight: Double,
    val birthDate: Long,
    val purpose: AnimalPurpose,
    val status: AnimalStatus,
    val nextFollowUpDate: Long?,
    val photoPath: String? = null
)

enum class AnimalPurpose { MEAT, MILK, BREEDING, DUAL_PURPOSE }
enum class AnimalStatus { ACTIVE, INACTIVE, SOLD }
