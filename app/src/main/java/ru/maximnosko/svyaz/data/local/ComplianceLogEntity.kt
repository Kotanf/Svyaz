package ru.maximnosko.svyaz.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "compliance_logs")
data class ComplianceLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val type: String, // MESSAGE, CALL, MEDIA
    val direction: String, // INCOMING, OUTGOING
    val senderAddress: String,
    val recipientAddress: String,
    val content: String?, // WARNING: This content will be encrypted before storage
    // Field stores encrypted message content for compliance auditing
    val status: String,
    val metadata: String? // Additional info (e.g., duration, media info)
)
