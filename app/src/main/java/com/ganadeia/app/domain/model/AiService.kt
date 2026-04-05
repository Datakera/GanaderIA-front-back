package com.ganadeia.app.domain.model

data class AiService(
    val id: String,
    val name: String,
    val endpoint: String,
    val modelId: String, // e.g., "llama-3-70b"
    val isActive: Boolean
)