package com.ganadeia.app.domain.service

import java.security.MessageDigest

/**
 * Servicio de Dominio para operaciones criptográficas de contraseñas.
 *
 * ⚠️ NOTA DE ARQUITECTURA para el equipo:
 * SHA-256 es adecuado para una v1 offline-first donde la BD local almacena
 * el hash. En una integración con backend real, este servicio debe ser
 * reemplazado por Bcrypt o Argon2 (a través de un puerto de infraestructura),
 * ya que SHA-256 no usa salt y es vulnerable a rainbow tables.
 *
 * El servicio vive en el dominio porque la REGLA de cómo se hashea es
 * una política del negocio, pero la IMPLEMENTACIÓN de la librería
 * criptográfica puede cambiar sin afectar al Use Case.
 */
object PasswordService {

    /**
     * Genera el hash SHA-256 de una contraseña en texto plano.
     *
     * @param plainPassword La contraseña tal como la escribe el usuario.
     * @return El hash en formato hexadecimal (64 caracteres).
     */
    fun hash(plainPassword: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(plainPassword.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifica si una contraseña en texto plano coincide con su hash guardado.
     * Ayuda a que el use case exprese su intención sin operar sobre strings crudos.
     */
    fun verify(plainPassword: String, storedHash: String): Boolean =
        hash(plainPassword) == storedHash
}