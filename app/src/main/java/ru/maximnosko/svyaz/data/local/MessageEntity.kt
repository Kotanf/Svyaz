package ru.maximnosko.svyaz.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val timestamp: Long,
    val content: String,
    val isFromMe: Boolean,
    val isEncrypted: Boolean,
    val isEdited: Boolean = false,
    val status: String,
    val mediaUri: String?,
    val mediaType: String?
)
