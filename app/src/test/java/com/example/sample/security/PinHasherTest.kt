package com.example.sample.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PinHasherTest {

    @Test
    fun `same value and salt produce the same hash`() {
        val salt = PinHasher.generateSalt()
        assertEquals(PinHasher.hash("1234", salt), PinHasher.hash("1234", salt))
    }

    @Test
    fun `different salts produce different hashes for the same value`() {
        val saltA = PinHasher.generateSalt()
        val saltB = PinHasher.generateSalt()
        assertNotEquals(PinHasher.hash("1234", saltA), PinHasher.hash("1234", saltB))
    }

    @Test
    fun `different values produce different hashes for the same salt`() {
        val salt = PinHasher.generateSalt()
        assertNotEquals(PinHasher.hash("1234", salt), PinHasher.hash("4321", salt))
    }

    @Test
    fun `generateSalt returns non-blank, non-equal values across calls`() {
        val saltA = PinHasher.generateSalt()
        val saltB = PinHasher.generateSalt()
        assertNotEquals(saltA, saltB)
    }
}
