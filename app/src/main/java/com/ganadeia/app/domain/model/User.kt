package com.ganadeia.app.domain.model

data class User(
    val id: String, // uuid in your ERD
    val name: String,
    val email: String,
    val role: UserRole,
    val ranchName: String?, // "finca" - null if ADMIN
    val location: String?, // null if ADMIN
    val permissions: String?, // null if RANCHER
    val createdAt: Long,
    val updatedAt: Long
)

enum class UserRole {
    RANCHER, ADMIN
}