package com.ganadeia.app.domain.service

import org.junit.Assert.*
import org.junit.Test

class PasswordServiceTest {

    @Test
    fun `hash should return 64 character hex string`() {
        val hash = PasswordService.hash("password123")
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("[0-9a-f]+")))
    }

    @Test
    fun `same password should always produce same hash`() {
        val hash1 = PasswordService.hash("myPassword")
        val hash2 = PasswordService.hash("myPassword")
        assertEquals(hash1, hash2)
    }

    @Test
    fun `different passwords should produce different hashes`() {
        val hash1 = PasswordService.hash("password1")
        val hash2 = PasswordService.hash("password2")
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `verify should return true for matching password and hash`() {
        val plain = "securePassword"
        val storedHash = PasswordService.hash(plain)
        assertTrue(PasswordService.verify(plain, storedHash))
    }

    @Test
    fun `verify should return false for wrong password`() {
        val storedHash = PasswordService.hash("correctPassword")
        assertFalse(PasswordService.verify("wrongPassword", storedHash))
    }

    @Test
    fun `hash should be case sensitive`() {
        val lower = PasswordService.hash("password")
        val upper = PasswordService.hash("Password")
        assertNotEquals(lower, upper)
    }

    @Test
    fun `hash should handle empty string without crashing`() {
        val hash = PasswordService.hash("")
        assertEquals(64, hash.length) // SHA-256 de string vacío es válido
    }
}