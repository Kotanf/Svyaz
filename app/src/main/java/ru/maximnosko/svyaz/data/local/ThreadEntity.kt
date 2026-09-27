package ru.maximnosko.svyaz.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threads")
data class ThreadEntity(
    @PrimaryKey val id: String,
    val contactName: String,
    val phoneNumber: String = "", // Added for explicit number tracking
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int,
    val isPinned: Boolean,
    val isMachine: Boolean,
    val category: String,
    val folder: String? = null
)
