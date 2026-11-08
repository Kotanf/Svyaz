package ru.maximnosko.svyaz.data

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class CategoryFilter(val label: String) {
    ALL("Все"),
    CODES("Коды"),
    FINANCE("Финансы"),
    SERVICES("Сервисы"),
    SPAM("Спам")
}

enum class MessageStatus {
    SENDING, SENT, DELIVERED, ERROR
}

data class LinkPreview(
    val title: String,
    val description: String,
    val url: String
)

sealed class TimelineEvent {
    abstract val id: String
    abstract val timestamp: Long

    data class Message(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long,
        val content: String,
        val isFromMe: Boolean,
        val linkPreview: LinkPreview? = null,
        val isPending: Boolean = false,
        val status: MessageStatus = MessageStatus.SENT,
        val deliveryTime: Long? = null,
        val isMms: Boolean = false,
        val isEncrypted: Boolean = false,
        val isEdited: Boolean = false,
        val reactions: Map<String, Int> = emptyMap(), // Emoji to Count
        val mediaUri: String? = null,
        val mediaType: MediaType? = null
    ) : TimelineEvent()

    data class Call(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long,
        val durationSeconds: Int,
        val isIncoming: Boolean,
        val isMissed: Boolean = false
    ) : TimelineEvent()
}

enum class MediaType {
    IMAGE, VIDEO, FILE, STICKER, GIF
}

fun getAvatarColorFor(name: String, key: String = ""): Color {
    val palette = listOf(
        Color(0xFF5B5BD6), // Electric Indigo
        Color(0xFF0891B2), // Cyan
        Color(0xFF0061A4), // Sapphire Blue
        Color(0xFF006C4C), // Emerald Green
        Color(0xFF704AB2), // Amethyst Purple
        Color(0xFFB04300), // Sunset Orange
        Color(0xFF984061), // Ruby Red
        Color(0xFF2563EB), // Royal Blue
        Color(0xFF059669), // Jade
        Color(0xFFD97706), // Amber
        Color(0xFFDC2626), // Crimson
        Color(0xFF7C3AED)  // Violet
    )
    val hash = (name + key).hashCode()
    val index = (hash and 0x7FFFFFFF) % palette.size
    return palette[index]
}

data class MessageThread(
    val id: String = UUID.randomUUID().toString(),
    val contactName: String,
    val phoneNumber: String = "", // Added for explicit number tracking
    val avatarColor: Color = Color.Gray,
    val avatarUri: String? = null,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMachine: Boolean = false,
    val category: CategoryFilter = CategoryFilter.ALL,
    val folder: String? = null
)

data class OtpCodeData(
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val sender: String,
    val expireTimeMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val actualExpireTime: Long?
        get() = expireTimeMinutes?.let { createdAt + (it * 60 * 1000L) }
    val isExpired: Boolean
        get() = actualExpireTime?.let { System.currentTimeMillis() > it } ?: false
}

data class CallLogItem(
    val id: String,
    val name: String,
    val number: String,
    val type: String,
    val timestamp: Long,
    val isMissed: Boolean = false,
    val avatarColor: Color = Color.Gray
)
