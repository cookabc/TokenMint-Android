package com.chuangcius.tokenmint.data.repository

import android.content.Context
import android.util.Base64
import com.chuangcius.tokenmint.data.model.EncryptedVault
import com.chuangcius.tokenmint.data.model.Vault
import com.chuangcius.tokenmint.service.KeystoreService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM encrypted vault persistence backed by Android Keystore.
 * Follows CODE_STANDARDS: Result<T> return type, IO dispatcher.
 */
open class VaultRepository(private val context: Context?) {
    private val vaultFile: File? = context?.let { File(it.filesDir, "vault.enc") }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    companion object {
        private const val GCM_TAG_LENGTH = 128
    }

    /** Load and decrypt the vault. Returns empty vault if file doesn't exist. */
    open suspend fun load(): Result<Vault> = withContext(Dispatchers.IO) {
        runCatching {
            val file = vaultFile ?: return@runCatching Vault()
            if (!file.exists()) return@runCatching Vault()

            val encryptedData = file.readText()
            val encryptedVault = json.decodeFromString<EncryptedVault>(encryptedData)

            val key = KeystoreService.getOrCreateKey()
            val iv = Base64.decode(encryptedVault.iv, Base64.NO_WRAP)
            val ciphertext = Base64.decode(encryptedVault.ciphertext, Base64.NO_WRAP)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

            val plaintext = cipher.doFinal(ciphertext)
            json.decodeFromString<Vault>(String(plaintext))
        }
    }

    /** Encrypt and save the vault atomically. */
    open suspend fun save(vault: Vault): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val key = KeystoreService.getOrCreateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val plaintext = json.encodeToString(Vault.serializer(), vault).toByteArray()
            val ciphertext = cipher.doFinal(plaintext)

            val encryptedVault = EncryptedVault(
                ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
                iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
                schemaVersion = vault.schemaVersion
            )

            // Atomic write: write to temp, then rename
            val file = vaultFile!!
            val tempFile = File(file.parent, "vault.enc.tmp")
            tempFile.writeText(json.encodeToString(EncryptedVault.serializer(), encryptedVault))
            tempFile.renameTo(file)
            Unit
        }
    }

    /** Check if a vault file exists on disk. */
    fun exists(): Boolean = vaultFile?.exists() == true
}
