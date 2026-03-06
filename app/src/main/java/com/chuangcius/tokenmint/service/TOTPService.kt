package com.chuangcius.tokenmint.service

import android.net.Uri
import com.chuangcius.tokenmint.data.model.TOTPAlgorithm
import com.chuangcius.tokenmint.data.model.Token
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * RFC 6238 TOTP implementation. Pure object, no side effects.
 */
object TOTPService {

    /** Generate a TOTP code for a token at a given timestamp. */
    fun generateCode(token: Token, timeMillis: Long = System.currentTimeMillis()): String {
        val keyData = base32Decode(token.normalizedSecret) ?: return "-".repeat(token.digits)
        val counter = timeMillis / 1000 / token.period
        val hmac = computeHMAC(keyData, counter, token.algorithm)
        val code = truncate(hmac, token.digits)
        return code.toString().padStart(token.digits, '0')
    }

    /** Remaining seconds in the current period. */
    fun remainingSeconds(period: Int, timeMillis: Long = System.currentTimeMillis()): Int {
        val seconds = (timeMillis / 1000).toInt()
        return period - (seconds % period)
    }

    /** Progress through the current period (0.0 = start, 1.0 = end). */
    fun progress(period: Int, timeMillis: Long = System.currentTimeMillis()): Float {
        val seconds = timeMillis / 1000.0
        val elapsed = seconds % period
        return (elapsed / period).toFloat()
    }

    /** Validate a Base32 string. */
    fun isValidBase32(string: String): Boolean {
        val cleaned = string.uppercase().replace(" ", "")
        if (cleaned.isEmpty()) return false
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567="
        return cleaned.all { it in base32Chars }
    }

    /** Parse an otpauth://totp/... URI into a Token. */
    fun parseOTPAuthURL(urlString: String): Token? {
        val uri = try {
            Uri.parse(urlString)
        } catch (_: Exception) {
            return null
        }

        if (uri.scheme != "otpauth" || uri.authority != "totp") return null

        val secret = uri.getQueryParameter("secret") ?: return null
        if (!isValidBase32(secret)) return null

        // Label from path: /Issuer:account or /account
        val label = uri.path?.trimStart('/') ?: ""
        val parts = label.split(":", limit = 2)
        val issuer = uri.getQueryParameter("issuer")
            ?: if (parts.size > 1) parts[0] else label
        val account = if (parts.size > 1) parts[1] else ""

        val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
        val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30
        val algorithm = when (uri.getQueryParameter("algorithm")?.uppercase()) {
            "SHA256" -> TOTPAlgorithm.SHA256
            "SHA512" -> TOTPAlgorithm.SHA512
            else -> TOTPAlgorithm.SHA1
        }

        return Token(
            issuer = issuer,
            account = account,
            secret = secret,
            digits = digits,
            period = period,
            algorithm = algorithm
        )
    }

    // MARK: - Base32 Decode (RFC 4648)

    fun base32Decode(input: String): ByteArray? {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val lookup = alphabet.withIndex().associate { (i, c) -> c to i }

        val cleaned = input.uppercase().replace(" ", "").replace("=", "")
        if (cleaned.isEmpty()) return null

        var bits = 0
        var buffer = 0L
        val output = mutableListOf<Byte>()

        for (char in cleaned) {
            val value = lookup[char] ?: return null
            buffer = (buffer shl 5) or value.toLong()
            bits += 5
            if (bits >= 8) {
                bits -= 8
                output.add(((buffer shr bits) and 0xFF).toByte())
            }
        }
        return output.toByteArray()
    }

    // MARK: - Private HMAC

    private fun computeHMAC(key: ByteArray, counter: Long, algorithm: TOTPAlgorithm): ByteArray {
        val message = ByteArray(8)
        var value = counter
        for (i in 7 downTo 0) {
            message[i] = (value and 0xFF).toByte()
            value = value shr 8
        }

        val mac = Mac.getInstance(algorithm.hmacAlgorithm)
        mac.init(SecretKeySpec(key, algorithm.hmacAlgorithm))
        return mac.doFinal(message)
    }

    private fun truncate(hmac: ByteArray, digits: Int): Int {
        val offset = (hmac[hmac.size - 1].toInt() and 0x0F)
        val code = (hmac[offset].toInt() and 0x7F shl 24) or
            (hmac[offset + 1].toInt() and 0xFF shl 16) or
            (hmac[offset + 2].toInt() and 0xFF shl 8) or
            (hmac[offset + 3].toInt() and 0xFF)
        val mod = 10.0.pow(digits.toDouble()).toInt()
        return code % mod
    }
}
