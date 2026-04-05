package com.ganadeia.app.domain.port.driven.repository

import com.ganadeia.app.domain.model.User

interface UserRepository {
    suspend fun getUserById(userId: String): User?
    suspend fun updateProfile(user: User): Boolean
}