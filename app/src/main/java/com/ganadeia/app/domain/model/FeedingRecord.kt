package com.ganadeia.app.domain.model

enum class FoodType { FORAGE, CONCENTRATE, SILAGE, SUPPLEMENT, MINERAL_SALT }

data class FeedingRecord(
    val id: String,
    val animalId: String,
    val date: Long,
    val foodType: FoodType,
    val quantityKg: Double,
    val observations: String?
)