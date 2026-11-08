package ru.maximnosko.svyaz.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import ru.maximnosko.svyaz.data.MessageThread
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PinnedContactsRow(
    threads: List<MessageThread>,
    onClick: (MessageThread) -> Unit
) {
    if (threads.isEmpty()) return
    
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(threads) { thread ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onClick(thread) }
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    if (thread.avatarUri != null) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
                        ) {
                            AsyncImage(
                                model = thread.avatarUri,
                                contentDescription = thread.contactName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = thread.avatarColor,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = thread.contactName.take(1),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    if (thread.unreadCount > 0) {
                        Surface(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = thread.unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = thread.contactName.split(" ").first(),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun maskSensitiveText(text: String, isEnabled: Boolean): String {
    if (!isEnabled || text.isBlank()) return text
    val codeRegex = Regex("""\b\d{4,6}\b""")
    val moneyRegex = Regex("""\b\d+[\s\d]*([₽$€]|руб|RUB)\b""", RegexOption.IGNORE_CASE)
    var result = text.replace(codeRegex, "••••")
    result = result.replace(moneyRegex, "•••• ₽")
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadItemSwipeable(
    thread: MessageThread,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onArchive: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isPrivacyShieldEnabled: Boolean = false
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                if (onArchive != null) onArchive() else onPin()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete()
            }
            else -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> if (onArchive != null) Color(0xFF3B82F6) else Color(0xFF5C6BC0)
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF5350)
                else -> Color.Transparent
            }
            val icon = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> if (onArchive != null) Icons.Default.Archive else Icons.Default.PushPin
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> Icons.Default.Archive
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        }
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
            targetValue = if (isPressed) 0.98f else 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "threadPressScale"
        )
        val itemBgColor = if (thread.isPinned) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surface
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .scale(pressScale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick
                ),
            color = itemBgColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (thread.avatarUri != null) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
                    ) {
                        AsyncImage(
                            model = thread.avatarUri,
                            contentDescription = thread.contactName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = thread.avatarColor,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = thread.contactName.take(1),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(
                                text = thread.contactName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (thread.unreadCount > 0) FontWeight.ExtraBold else FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (thread.isPinned) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        val timeStr = remember(thread.lastMessageTime) {
                            formatChatDate(thread.lastMessageTime)
                        }
                        
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (thread.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    val displayMessage = remember(thread.lastMessage, isPrivacyShieldEnabled) {
                        maskSensitiveText(thread.lastMessage, isPrivacyShieldEnabled)
                    }
                    Text(
                        text = displayMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (thread.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (thread.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

fun formatChatDate(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return when {
        now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) -> {
            SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
        }
        else -> {
            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
