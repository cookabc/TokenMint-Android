package com.chuangcius.tokenmint.util

/** Unified error types for TokenMint. */
sealed class AppError(override val message: String) : Exception(message) {
    class VaultLoadFailed(val underlying: String) : AppError("Failed to load vault: $underlying")
    class VaultSaveFailed(val underlying: String) : AppError("Failed to save vault: $underlying")
    class EncryptionFailed(val underlying: String) : AppError("Encryption failed: $underlying")
    class DecryptionFailed(val underlying: String) : AppError("Decryption failed: $underlying")
    class InvalidBase32 : AppError("Invalid Base32 secret key")
    class InvalidOTPAuthURL : AppError("Invalid otpauth:// URL format")
    class BiometricFailed(val underlying: String) : AppError("Biometric authentication failed: $underlying")
    class CameraDenied : AppError("Camera access denied")
    class Unknown(val underlying: String) : AppError("An unexpected error occurred: $underlying")
}

/**
 * Humanize error message for user display (CODE_STANDARDS requirement).
 * Raw error is shown as secondary text; this produces the primary friendly message.
 */
fun humanizeError(message: String): String = when {
    message.contains("UnknownHostException", ignoreCase = true) ||
        message.contains("No address associated", ignoreCase = true) ->
        "No internet connection"
    message.contains("SocketTimeoutException", ignoreCase = true) ||
        message.contains("timeout", ignoreCase = true) ->
        "Request timed out — try again"
    message.contains("KeyStoreException", ignoreCase = true) ||
        message.contains("KeyStore", ignoreCase = true) ->
        "Security key error — try again"
    message.contains("AEADBadTagException", ignoreCase = true) ||
        message.contains("decrypt", ignoreCase = true) ->
        "Vault decryption failed — data may be corrupted"
    message.contains("Biometric", ignoreCase = true) ||
        message.contains("authenticate", ignoreCase = true) ->
        "Authentication failed"
    message.contains("camera", ignoreCase = true) ->
        "Camera access denied"
    else -> "Something went wrong"
}
