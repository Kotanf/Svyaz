package ru.maximnosko.svyaz.data

import android.content.Context
import android.util.Base64
import android.util.Log
import java.security.SecureRandom
import javax.crypto.spec.GCMParameterSpec
import ru.maximnosko.svyaz.data.local.ComplianceLogEntity
import ru.maximnosko.svyaz.data.local.SvyazDatabase
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * ComplianceManager handles ORI (Организатор распространения информации) requirements.
 * It logs metadata and content for auditing purposes.
 * Hardened: All content is encrypted using AES-GCM before local storage.
 */
class ComplianceManager(private val context: Context) {
    private val db = SvyazDatabase.getDatabase(context)
    private val dao = db.threadDao()

    suspend fun logEvent(
        type: String,
        direction: String,
        sender: String,
        recipient: String,
        content: String?,
        status: String = "SUCCESS",
        metadata: String? = null
    ) {
        try {
            val encryptedContent = content?.let { encryptForAudit(it) }

            val logEntry = ComplianceLogEntity(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                type = type,
                direction = direction,
                senderAddress = sender,
                recipientAddress = recipient,
                content = encryptedContent,
                status = status,
                metadata = metadata
            )
            dao.insertComplianceLog(logEntry)
        } catch (_: Exception) {
            Log.e("ComplianceManager", "Failed to log compliance event")
        }
    }

    private fun encryptForAudit(text: String): String {
        var keyBytes: ByteArray? = null
        return try {
            keyBytes = SecurityManager.getDatabasePassphrase(context)
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val secureRandom = SecureRandom()
            val iv = ByteArray(12)
            secureRandom.nextBytes(iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            
            val encryptedBytes = cipher.doFinal(text.toByteArray())
            
            // KWRAP: Envelope structure [Version:1][IV:12][CipherText]
            val combined = ByteArray(1 + iv.size + encryptedBytes.size)
            combined[0] = 1 // Version byte for future-proofing
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, 1 + iv.size, encryptedBytes.size)
            
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("ComplianceManager", "Encryption failed", e)
            "ENCRYPTION_ERROR"
        } finally {
            // Memory Sanitization: Wipe key bytes after encryption
            SecurityManager.sanitize(keyBytes)
        }
    }

    fun auditContent(content: String): Boolean {
        val dangerousKeywords = listOf("бомба", "теракт", "взрыв", "оружие", "захват", "убийство")
        val lowerContent = content.lowercase()
        // Strict check to avoid false positives on normal words (e.g. 'предмет', 'взрывчатка' but not 'взрыв')
        val regexes = listOf(
            Regex("\\bбомба\\b", RegexOption.IGNORE_CASE),
            Regex("\\bтеракт\\b", RegexOption.IGNORE_CASE),
            Regex("\\bвзрывчатка\\b", RegexOption.IGNORE_CASE),
            Regex("\\bоружие\\b", RegexOption.IGNORE_CASE),
            Regex("\\bзахват\\b", RegexOption.IGNORE_CASE),
            Regex("\\bубийство\\b", RegexOption.IGNORE_CASE)
        )
        for (regex in regexes) {
            if (regex.containsMatchIn(lowerContent)) {
                Log.w("AntiTerrorGuard", "CRITICAL: Suspicious content detected")
                return false
            }
        }
        return true
    }
}
