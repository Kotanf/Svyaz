package ru.maximnosko.svyaz.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import androidx.core.content.edit

/**
 * SecurityManager handles hardware-backed key generation and secure passphrase storage.
 * It uses Android KeyStore to encrypt/decrypt a random passphrase used by SQLCipher.
 * Enhanced with memory sanitization logic.
 */
object SecurityManager {
    private const val TAG = "SecurityManager"
    private const val KEY_ALIAS = "svyaz_db_key_v2" // Incremented version to ensure fresh start
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val PREFS_NAME = "security_prefs"
    private const val ENCRYPTED_PASSPHRASE_KEY = "encrypted_passphrase"
    private const val IV_KEY = "passphrase_iv"

    fun getDatabasePassphrase(context: Context): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        try {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
                Log.i(TAG, "Generated new hardware-backed key")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate key", e)
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedPassphraseBase64 = prefs.getString(ENCRYPTED_PASSPHRASE_KEY, null)
        val ivBase64 = prefs.getString(IV_KEY, null)

        val keyEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        val key = keyEntry?.secretKey

        if (key == null) {
            Log.e(TAG, "Key not found in KeyStore after generation attempt")
            throw IllegalStateException("Hardware security module is unavailable")
        }

        return if (encryptedPassphraseBase64 == null || ivBase64 == null) {
            // Generate a random stable passphrase
            val random = SecureRandom()
            val passphrase = ByteArray(32)
            random.nextBytes(passphrase)

            try {
                // Encrypt the passphrase using the KeyStore key
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, key)
                val iv = cipher.iv
                val encryptedPassphrase = cipher.doFinal(passphrase)

                // Store encrypted version
                prefs.edit {
                    putString(
                        ENCRYPTED_PASSPHRASE_KEY,
                        Base64.encodeToString(encryptedPassphrase, Base64.NO_WRAP)
                    )
                        .putString(IV_KEY, Base64.encodeToString(iv, Base64.NO_WRAP))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Encryption of passphrase failed", e)
            }

            passphrase
        } else {
            try {
                // Decrypt the stored passphrase
                val encryptedPassphrase = Base64.decode(encryptedPassphraseBase64, Base64.DEFAULT)
                val iv = Base64.decode(ivBase64, Base64.DEFAULT)

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)
                
                cipher.doFinal(encryptedPassphrase)
            } catch (e: Exception) {
                Log.e(TAG, "Decryption of passphrase failed, regenerating new secure passphrase", e)
                // Remove invalid corrupted entries and generate fresh secure key
                prefs.edit {
                    remove(ENCRYPTED_PASSPHRASE_KEY)
                    remove(IV_KEY)
                }
                val random = SecureRandom()
                val newPassphrase = ByteArray(32)
                random.nextBytes(newPassphrase)
                try {
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.ENCRYPT_MODE, key)
                    val iv = cipher.iv
                    val encryptedPassphrase = cipher.doFinal(newPassphrase)
                    prefs.edit {
                        putString(ENCRYPTED_PASSPHRASE_KEY, Base64.encodeToString(encryptedPassphrase, Base64.NO_WRAP))
                        putString(IV_KEY, Base64.encodeToString(iv, Base64.NO_WRAP))
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to store re-generated passphrase", ex)
                }
                newPassphrase
            }
        }
    }

    /**
     * Wipes a byte array from memory by filling it with zeros.
     */
    fun sanitize(bytes: ByteArray?) {
        if (bytes != null) {
            Arrays.fill(bytes, 0.toByte())
        }
    }
}
