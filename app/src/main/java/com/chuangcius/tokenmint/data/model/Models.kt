package com.chuangcius.tokenmint.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

// MARK: - Serializers

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): UUID =
        UUID.fromString(decoder.decodeString())
}

object InstantSerializer : KSerializer<Instant> {
    private val formatter = DateTimeFormatter.ISO_INSTANT

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) =
        encoder.encodeString(formatter.format(value))

    override fun deserialize(decoder: Decoder): Instant =
        Instant.parse(decoder.decodeString())
}

// MARK: - TOTPAlgorithm

@Serializable
enum class TOTPAlgorithm {
    @SerialName("sha1") SHA1,
    @SerialName("sha256") SHA256,
    @SerialName("sha512") SHA512;

    val hmacAlgorithm: String
        get() = when (this) {
            SHA1 -> "HmacSHA1"
            SHA256 -> "HmacSHA256"
            SHA512 -> "HmacSHA512"
        }
}

// MARK: - Token

@Serializable
data class Token(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val issuer: String,
    val account: String = "",
    val label: String = "",
    val secret: String,
    val digits: Int = 6,
    val period: Int = 30,
    val algorithm: TOTPAlgorithm = TOTPAlgorithm.SHA1,
    val sortOrder: Int = 0,
    val isPinned: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant = Instant.now()
) {
    /** Normalize secret: uppercase, strip spaces. */
    val normalizedSecret: String
        get() = secret.uppercase().replace(" ", "")
}

// MARK: - Vault

@Serializable
data class Vault(
    val tokens: List<Token> = emptyList(),
    val vaultVersion: Int = 0,
    val schemaVersion: Int = 1,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant = Instant.now()
)

// MARK: - EncryptedVault

@Serializable
data class EncryptedVault(
    val ciphertext: String,   // Base64
    val iv: String,           // Base64 (GCM nonce)
    val schemaVersion: Int
)
