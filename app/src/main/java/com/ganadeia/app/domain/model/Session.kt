package com.ganadeia.app.domain.model

data class Session(
    val id: String,
    val userId: String,
    val token: String,
    val createdAt: Long,
    val expiresAt: Long,
    val isActive: Boolean
)