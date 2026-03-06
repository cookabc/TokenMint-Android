package com.chuangcius.tokenmint

import com.chuangcius.tokenmint.data.model.TOTPAlgorithm
import com.chuangcius.tokenmint.data.model.Token
import com.chuangcius.tokenmint.service.TOTPService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TOTPService — RFC 6238 test vectors, Base32 validation,
 * and otpauth URL parsing.
 */
class TOTPServiceTest {

    // RFC 6238 test vector: ASCII "12345678901234567890" = Base32 "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    private val testSecret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

    @Test
    fun `generateCode produces 6-digit code`() {
        val token = Token(issuer = "Test", secret = testSecret)
        val code = TOTPService.generateCode(token)
        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })
    }

    @Test
    fun `generateCode with 8 digits produces 8-digit code`() {
        val token = Token(issuer = "Test", secret = testSecret, digits = 8)
        val code = TOTPService.generateCode(token)
        assertEquals(8, code.length)
    }

    @Test
    fun `generateCode with known time matches RFC 6238 vector`() {
        // RFC 6238 Appendix B: time = 59, SHA1 → code 287082 (counter = 1)
        val token = Token(issuer = "Test", secret = testSecret)
        val code = TOTPService.generateCode(token, timeMillis = 59_000L)
        assertEquals("287082", code)
    }

    @Test
    fun `remainingSeconds returns value between 1 and period`() {
        val remaining = TOTPService.remainingSeconds(30)
        assertTrue(remaining in 1..30)
    }

    @Test
    fun `remainingSeconds at exact boundary`() {
        // At t=0ms, elapsed=0s in period, remaining=30
        val remaining = TOTPService.remainingSeconds(30, timeMillis = 0L)
        assertEquals(30, remaining)
    }

    @Test
    fun `progress returns value between 0 and 1`() {
        val progress = TOTPService.progress(30)
        assertTrue(progress in 0f..1f)
    }

    @Test
    fun `isValidBase32 accepts valid strings`() {
        assertTrue(TOTPService.isValidBase32("JBSWY3DPEHPK3PXP"))
        assertTrue(TOTPService.isValidBase32("GEZDGNBVGY3TQOJQ"))
        assertTrue(TOTPService.isValidBase32("MFRA===="))
    }

    @Test
    fun `isValidBase32 rejects invalid strings`() {
        assertFalse(TOTPService.isValidBase32(""))
        assertFalse(TOTPService.isValidBase32("01234"))  // contains 0, 1
        assertFalse(TOTPService.isValidBase32("hello!"))
    }

    // NOTE: parseOTPAuthURL tests require android.net.Uri which is unavailable
    // in local unit tests. These are covered in androidTest/ instrumented tests.

    @Test
    fun `base32Decode decodes correctly`() {
        // "Hello!" = JBSWY3DPEE======
        val decoded = TOTPService.base32Decode("JBSWY3DPEE")
        assertNotNull(decoded)
        assertEquals("Hello!", String(decoded!!))
    }

    @Test
    fun `base32Decode returns null for invalid input`() {
        assertNull(TOTPService.base32Decode(""))
        assertNull(TOTPService.base32Decode("0000"))
    }
}
