package ru.maximnosko.svyaz.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import ru.maximnosko.svyaz.ui.components.TelegramWallpaper
import ru.maximnosko.svyaz.ui.components.maskSensitiveText
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import ru.maximnosko.svyaz.data.MediaType
import ru.maximnosko.svyaz.data.MessageStatus
import ru.maximnosko.svyaz.data.MessageThread
import ru.maximnosko.svyaz.data.SystemDataProvider
import ru.maximnosko.svyaz.data.TimelineEvent
import ru.maximnosko.svyaz.ui.theme.MessagesTheme
import ru.maximnosko.svyaz.ui.theme.SvyazBrandColors
import ru.maximnosko.svyaz.viewmodel.MessagesViewModel
import ru.maximnosko.svyaz.viewmodel.UiEvent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    thread: MessageThread,
    viewModel: MessagesViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    val timeline = remember(uiState.timelineByThread, thread.id, thread.phoneNumber) {
        val key = if (thread.phoneNumber.isNotBlank()) thread.phoneNumber else thread.id
        uiState.timelineByThread[key] ?: uiState.timelineByThread[thread.id] ?: emptyList()
    }
    
    var showContactInfo by remember { mutableStateOf(false) }
    var showMediaPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val systemDataProvider = remember { SystemDataProvider(context) }
    val targetNumber = remember(thread) { thread.phoneNumber.ifEmpty { thread.id } }
    val contactUri = remember(targetNumber) { systemDataProvider.getContactUri(targetNumber) }

    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var selectedMessageForMenu by remember { mutableStateOf<TimelineEvent.Message?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.sendMessage(thread.id, "", mediaUri = it.toString(), mediaType = MediaType.IMAGE)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.sendMessage(thread.id, "[Фото с камеры]", mediaType = MediaType.IMAGE)
        }
    }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment ?: "Документ"
            viewModel.sendMessage(thread.id, "📎 Документ: $fileName", mediaUri = it.toString(), mediaType = MediaType.FILE)
        }
    }

    LaunchedEffect(thread, uiState.isLoading) {
        if (thread.phoneNumber.isNotBlank()) {
            viewModel.loadTimeline(thread.phoneNumber)
        } else {
            viewModel.loadTimeline(thread.id)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (contactUri != null) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, contactUri)
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        showContactInfo = true
                                    }
                                } else {
                                    showContactInfo = true
                                }
                            }
                    ) {
                        if (thread.avatarUri != null) {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                border = BorderStroke(1.5.dp, Color(0xFF7DD3FC).copy(alpha = 0.35f))
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
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                color = thread.avatarColor.copy(alpha = 0.85f),
                                border = BorderStroke(1.5.dp, Color(0xFF7DD3FC).copy(alpha = 0.35f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(thread.contactName.take(1), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(thread.contactName, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = SvyazBrandColors.Emerald,
                                    modifier = Modifier.size(5.dp)
                                ) {}
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "онлайн",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SvyazBrandColors.Emerald,
                                    fontWeight = FontWeight.Medium
                                )
                                if (thread.phoneNumber.isNotBlank()) {
                                    Text(
                                        " • ${thread.phoneNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    val showAdvancedActions = uiState.isProModeUnlocked
                    if (showAdvancedActions) {
                        val eyeRotation by animateFloatAsState(
                            targetValue = if (uiState.isPrivacyShieldEnabled) 180f else 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                            label = "eyeRotation"
                        )
                        IconButton(
                            onClick = { viewModel.togglePrivacyShield(!uiState.isPrivacyShieldEnabled) }
                        ) {
                            Icon(
                                imageVector = if (uiState.isPrivacyShieldEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Анти-взгляд",
                                tint = if (uiState.isPrivacyShieldEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.rotate(eyeRotation)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            if (thread.phoneNumber.isNotBlank()) {
                                viewModel.makeCall(thread.phoneNumber)
                            } else {
                                viewModel.showSnackBar("Номер не сохранен")
                            }
                        },
                        enabled = thread.phoneNumber.isNotBlank()
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (showAdvancedActions) {
                        IconButton(
                            onClick = {
                                if (thread.phoneNumber.isNotBlank()) {
                                    viewModel.makeCall(thread.phoneNumber)
                                } else {
                                    viewModel.showSnackBar("Номер не сохранен")
                                }
                            },
                            enabled = thread.phoneNumber.isNotBlank()
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = SvyazBrandColors.NeonCyan)
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = showMediaPicker,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        MediaPickerRow(
                            onPhotoPick = { photoPicker.launch("image/*") },
                            onCameraPick = { cameraLauncher.launch(null) },
                            onFilePick = { documentPicker.launch("*/*") }
                        )
                    }
                    
                    val lastMessageContent = timeline.filterIsInstance<TimelineEvent.Message>().lastOrNull()?.content
                    SmartActionChipsRow(
                        lastMessage = lastMessageContent,
                        onChipClick = { chipText ->
                            inputText = chipText
                        },
                        onCallClick = { phone ->
                            viewModel.makeCall(phone)
                        },
                        onCopyCode = { code ->
                            viewModel.copyMessageText(code)
                            viewModel.showSnackBar("Код $code скопирован")
                        }
                    )

                    UnifiedMessageInput(
                        text = inputText,
                        onTextChange = { inputText = it },
                        selectedSimSlot = uiState.selectedSimSlot,
                        showSimButton = remember { systemDataProvider.hasMultipleSims },
                        onToggleSim = { viewModel.toggleSimSlot(it) },
                        onSend = {
                            if (inputText.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (editingMessageId != null) {
                                    viewModel.onEditMessage(thread.id, editingMessageId!!, inputText)
                                    editingMessageId = null
                                } else {
                                    viewModel.sendMessage(thread.id, inputText)
                                }
                                inputText = ""
                                showMediaPicker = false
                            }
                        },
                        isUndoVisible = uiState.isUndoSendVisible,
                        undoProgress = uiState.undoProgress,
                        onUndo = { viewModel.cancelSend() },
                        isAttachExpanded = showMediaPicker,
                        onAttachClick = { showMediaPicker = !showMediaPicker },
                        editingMessage = if (editingMessageId != null) "Редактирование" else null,
                        onCancelEdit = {
                            editingMessageId = null
                            inputText = ""
                        }
                    )
                }
            }
        }
    ) { padding ->
        if (showContactInfo) {
            ContactInfoDialog(
                thread = thread,
                contactUri = contactUri,
                onDismiss = { showContactInfo = false },
                onViewContact = {
                    showContactInfo = false
                    contactUri?.let { uri ->
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (_: Exception) {}
                    }
                },
                onSaveContact = {
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        type = ContactsContract.Contacts.CONTENT_TYPE
                        putExtra(ContactsContract.Intents.Insert.PHONE, thread.phoneNumber.ifEmpty { thread.id })
                        putExtra(ContactsContract.Intents.Insert.NAME, thread.contactName)
                    }
                    context.startActivity(intent)
                    showContactInfo = false
                },
                onSaveToSim = {
                    viewModel.saveToSim(thread.contactName, thread.phoneNumber.ifEmpty { thread.id })
                    showContactInfo = false
                }
            )
        }

        if (selectedMessageForMenu != null) {
            MessageActionsDialog(
                message = selectedMessageForMenu!!,
                viewModel = viewModel,
                onDismiss = { selectedMessageForMenu = null },
                onDelete = { 
                    viewModel.onDeleteMessage(thread.id, selectedMessageForMenu!!.id)
                    selectedMessageForMenu = null
                },
                onEdit = { 
                    editingMessageId = selectedMessageForMenu!!.id
                    inputText = selectedMessageForMenu!!.content
                    selectedMessageForMenu = null
                },
                onReaction = { emoji ->
                    viewModel.onToggleReaction(thread.id, selectedMessageForMenu!!.id, emoji)
                    selectedMessageForMenu = null
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TelegramWallpaper(themePreset = uiState.themePreset)

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(timeline, key = { index, event -> "${event.id}_$index" }) { index, event ->
                    when (event) {
                        is TimelineEvent.Message -> MessageBubble(
                            message = event,
                            isPrivacyShieldEnabled = uiState.isPrivacyShieldEnabled,
                            onLongPress = {
                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                selectedMessageForMenu = event
                            },
                            onRetry = { viewModel.sendMessage(thread.id, event.content) }
                        )
                        is TimelineEvent.Call -> CallEventCard(event, onRedial = { viewModel.makeCall(thread.phoneNumber.ifEmpty { thread.id }) })
                    }
                }
                
                val pending = uiState.pendingMessage
                if (pending != null) {
                    item(key = "pending_msg") {
                        MessageBubble(pending, isPending = true, isPrivacyShieldEnabled = uiState.isPrivacyShieldEnabled)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageActionsDialog(
    message: TimelineEvent.Message,
    viewModel: MessagesViewModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onReaction: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("❤️", "👍", "🔥", "😂", "😮").forEach { emoji ->
                    Surface(
                        onClick = { onReaction(emoji) },
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (message.isFromMe) {
                    ListItem(
                        headlineContent = { Text("Редактировать") },
                        leadingContent = { Icon(Icons.Default.Edit, null) },
                        modifier = Modifier.clickable { onEdit() }
                    )
                }
                ListItem(
                    headlineContent = { Text("Копировать") },
                    leadingContent = { Icon(Icons.Default.ContentCopy, null) },
                    modifier = Modifier.clickable { 
                        viewModel.copyMessageText(message.content)
                        onDismiss() 
                    }
                )
                ListItem(
                    headlineContent = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        }
    )
}

@Composable
fun MediaPickerRow(
    onPhotoPick: () -> Unit,
    onCameraPick: () -> Unit,
    onFilePick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MediaItem(Icons.Default.Photo, "Фото", onPhotoPick)
        MediaItem(Icons.Default.PhotoCamera, "Камера", onCameraPick)
        MediaItem(Icons.Default.Description, "Файл", onFilePick)
    }
}

@Composable
fun MediaItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mediaScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clip(MaterialTheme.shapes.medium)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun MessageBubble(
    message: TimelineEvent.Message, 
    isPending: Boolean = false,
    isPrivacyShieldEnabled: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    val isMe = message.isFromMe
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val isDark = isSystemInDarkTheme()
    
    // 100% Solid, ultra-high contrast bubble colors (no transparency blur that degrades legibility)
    val bubbleColor = if (isMe) {
        if (isDark) {
            Color(0xFF3B3B98) // Solid vibrant Cosmic Indigo in dark mode
        } else {
            Color(0xFF4F46E5) // Solid rich Indigo/Blue in light mode
        }
    } else {
        if (isDark) {
            Color(0xFF1E293B) // Solid Dark Slate
        } else {
            Color(0xFFF1F5F9) // Solid Light Slate
        }
    }

    val bubbleBorder = if (isMe) {
        if (isDark) {
            BorderStroke(1.dp, Color(0xFF67E8F9).copy(alpha = 0.40f))
        } else {
            BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
        }
    } else {
        if (isDark) {
            BorderStroke(1.dp, Color(0xFF334155))
        } else {
            BorderStroke(1.dp, Color(0xFFE2E8F0))
        }
    }
    
    // Message text colors - 100% SOLID
    val textColor = if (isMe) {
        Color(0xFFFFFFFF) // Pure Solid White for outgoing
    } else {
        if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    }
    
    // Timestamp colors - 100% SOLID & BOLD
    val timeColor = if (isMe) {
        Color(0xFFE0E7FF) // Crisp Light Lavender-White for outgoing time
    } else {
        if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), 
        horizontalAlignment = alignment
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (isMe && message.status == MessageStatus.ERROR) {
                IconButton(onClick = { onRetry?.invoke() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                }
            }
            
            Surface(
                color = bubbleColor,
                border = bubbleBorder,
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isMe) 18.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 18.dp
                ),
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .alpha(if (isPending) 0.6f else 1f)
                    .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress?.invoke() }) }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (message.mediaUri != null) { MediaContent(message) }
                    
                    if (message.content.isNotEmpty()) {
                        val displayContent = remember(message.content, isPrivacyShieldEnabled) {
                            maskSensitiveText(message.content, isPrivacyShieldEnabled)
                        }
                        Text(
                            text = displayContent, 
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    ) {
                        if (message.isEdited) {
                            Text(
                                "изм. ", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = timeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = timeFormat.format(Date(message.timestamp)), 
                            style = MaterialTheme.typography.labelSmall, 
                            color = timeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            AnimatedContent(
                                targetState = message.status,
                                transitionSpec = {
                                    (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn())
                                        .togetherWith(scaleOut() + fadeOut())
                                },
                                label = "statusAnim"
                            ) { status ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val iconTint = when (status) {
                                        MessageStatus.DELIVERED -> Color(0xFF34D399) // Bright Emerald Green (#34D399)
                                        MessageStatus.ERROR -> Color(0xFFF87171)     // Bright Red (#F87171)
                                        MessageStatus.SENT -> Color(0xFF67E8F9)      // Bright Neon Cyan (#67E8F9)
                                        else -> Color(0xFFE0E7FF)
                                    }
                                    Icon(
                                        imageVector = when (status) {
                                            MessageStatus.SENDING -> Icons.Default.Schedule
                                            MessageStatus.SENT -> Icons.Default.Check
                                            MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                            MessageStatus.ERROR -> Icons.Default.Error
                                        },
                                        contentDescription = "Статус сообщения",
                                        modifier = Modifier.size(16.dp),
                                        tint = iconTint
                                    )
                                    if (status == MessageStatus.DELIVERED) {
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            "доставлено",
                                            fontSize = 10.sp,
                                            color = Color(0xFF34D399),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (message.reactions.isNotEmpty()) {
            Row(modifier = Modifier.padding(top = 2.dp)) {
                message.reactions.forEach { (emoji, count) ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = "$emoji $count", 
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MediaContent(message: TimelineEvent.Message) {
    val modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 240.dp)
        .clip(RoundedCornerShape(16.dp))
        .padding(bottom = 4.dp)
    
    when (message.mediaType) {
        MediaType.IMAGE -> {
            if (!message.mediaUri.isNullOrBlank()) {
                AsyncImage(
                    model = message.mediaUri,
                    contentDescription = "Фото",
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            } else {
                Box(modifier.background(Color.Gray.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Image, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                }
            }
        }
        MediaType.STICKER -> {
            Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Face, 
                    contentDescription = null, 
                    modifier = Modifier.size(100.dp), 
                    tint = Color(0xFFFFA000)
                )
            }
        }
        MediaType.GIF -> {
            Box(
                modifier = modifier
                    .background(Color.DarkGray)
                    .padding(24.dp), 
                contentAlignment = Alignment.Center
            ) {
                Text("GIF", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
        }
        else -> {}
    }
}

@Composable
fun CallEventCard(call: TimelineEvent.Call, onRedial: () -> Unit) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (call.isMissed) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (call.isMissed) Icons.AutoMirrored.Filled.CallMissed else Icons.AutoMirrored.Filled.CallReceived,
                contentDescription = null,
                tint = if (call.isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (call.isMissed) "Пропущенный звонок" else if (call.isIncoming) "Входящий звонок" else "Исходящий звонок",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${timeFormat.format(Date(call.timestamp))} • ${call.durationSeconds} сек",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onRedial) {
                Icon(Icons.Default.Call, contentDescription = "Перезвонить", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}



@Composable
fun UnifiedMessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    selectedSimSlot: Int = 1,
    showSimButton: Boolean = false,
    onToggleSim: (Int) -> Unit = {},
    onSend: () -> Unit,
    isUndoVisible: Boolean,
    undoProgress: Float,
    onUndo: () -> Unit,
    onAttachClick: () -> Unit,
    isAttachExpanded: Boolean = false,
    editingMessage: String?,
    onCancelEdit: () -> Unit
) {
    Column {
        if (editingMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(editingMessage, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onCancelEdit, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            }
        }
        
        if (isUndoVisible) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Отправка сообщения...", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = onUndo) {
                        Text("Отменить")
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val attachRotation by animateFloatAsState(
                targetValue = if (isAttachExpanded) 45f else 0f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
                label = "attachRotation"
            )
            Surface(
                onClick = onAttachClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = if (isAttachExpanded) "Закрыть вложения" else "Прикрепить вложение",
                        tint = SvyazBrandColors.Indigo,
                        modifier = Modifier.size(20.dp).rotate(attachRotation)
                    )
                }
            }

            Spacer(Modifier.width(6.dp))
            
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Сообщение...") },
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                    focusedBorderColor = SvyazBrandColors.NeonCyan.copy(alpha = 0.5f)
                )
            )
            
            if (showSimButton) {
                Spacer(Modifier.width(4.dp))
                Surface(
                    onClick = { onToggleSim(if (selectedSimSlot == 1) 2 else 1) },
                    shape = CircleShape,
                    color = SvyazBrandColors.NeonCyan.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SvyazBrandColors.NeonCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = "SIM $selectedSimSlot",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        color = SvyazBrandColors.NeonCyan
                    )
                }
            }

            Spacer(Modifier.width(6.dp))
            
            val isTyping = text.isNotBlank()
            val sendScale by animateFloatAsState(
                targetValue = if (isTyping) 1.05f else 0.95f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                label = "sendScale"
            )
            val sendInteractionSource = remember { MutableInteractionSource() }
            val isSendPressed by sendInteractionSource.collectIsPressedAsState()
            val sendPressScale by animateFloatAsState(
                targetValue = if (isSendPressed) 0.88f else sendScale,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                label = "sendPressScale"
            )

            val sendIconRotation by animateFloatAsState(
                targetValue = if (isTyping) 0f else -15f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
                label = "sendIconRotation"
            )

            Surface(
                onClick = onSend,
                shape = CircleShape,
                color = if (isTyping) SvyazBrandColors.NeonCyan.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (isTyping) SvyazBrandColors.NeonCyan.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .size(44.dp)
                    .scale(sendPressScale)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Отправить",
                        tint = if (isTyping) SvyazBrandColors.NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp).rotate(sendIconRotation)
                    )
                }
            }
        }
    }
}

@Composable
fun ContactInfoDialog(
    thread: MessageThread,
    contactUri: Uri?,
    onDismiss: () -> Unit,
    onSaveContact: () -> Unit,
    onViewContact: () -> Unit,
    onSaveToSim: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(thread.contactName) },
        text = {
            Column {
                Text("Номер: ${thread.phoneNumber.ifEmpty { thread.id }}")
                Spacer(Modifier.height(16.dp))
                if (contactUri != null) {
                    Button(onClick = onViewContact, modifier = Modifier.fillMaxWidth()) {
                        Text("Посмотреть в контактах")
                    }
                } else {
                    Button(onClick = onSaveContact, modifier = Modifier.fillMaxWidth()) {
                        Text("Сохранить в телефонную книгу")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onSaveToSim, modifier = Modifier.fillMaxWidth()) {
                    Text("Сохранить на SIM")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

@Composable
fun SmartActionChipsRow(
    lastMessage: String?,
    onChipClick: (String) -> Unit,
    onCallClick: (String) -> Unit,
    onCopyCode: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val chips = remember {
        listOf("⚡ Договорились", "⚡ Буду через 15 мин", "⚡ Перезвоню позже", "⚡ Спасибо!", "⚡ Да", "⚡ Нет")
    }

    val extractedCode = remember(lastMessage) {
        if (lastMessage == null) null
        else Regex("""\b\d{4,6}\b""").find(lastMessage)?.value
    }
    val extractedPhone = remember(lastMessage) {
        if (lastMessage == null) null
        else Regex("""\+?[78]\d{10}""").find(lastMessage)?.value
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (extractedCode != null) {
            item {
                SuggestionChip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCopyCode(extractedCode)
                    },
                    label = { Text("📋 Скопировать код $extractedCode", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = CircleShape
                )
            }
        }

        if (extractedPhone != null) {
            item {
                SuggestionChip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCallClick(extractedPhone)
                    },
                    label = { Text("📞 Позвонить $extractedPhone", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = CircleShape
                )
            }
        }

        items(chips) { chipText ->
            SuggestionChip(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onChipClick(chipText.removePrefix("⚡ "))
                },
                label = { Text(chipText, fontSize = 12.sp) },
                shape = CircleShape,
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
                )
            )
        }
    }
}
