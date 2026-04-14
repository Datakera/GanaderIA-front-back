package com.ganadeia.app.domain.model

/**
 * Objeto de valor (Value Object) que representa las credenciales
 * ingresadas por el ganadero en el formulario de inicio de sesión.
 *
 * No tiene identidad propia — su igualdad se basa en sus valores.
 * La validación de formato se aplica en el Use Case, no aquí.
 */
data class LoginCredentials(
    val email: String,
    val password: String
)