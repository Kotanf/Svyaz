package ru.maximnosko.svyaz.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import ru.maximnosko.svyaz.data.TimelineEvent
import ru.maximnosko.svyaz.ui.theme.MessagesTheme
import ru.maximnosko.svyaz.viewmodel.MessagesViewModel
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
    
    val timeline = remember(uiState.timelineByThread, thread.id, thread.phoneNumber, uiState.activeThreadId) {
        uiState.timelineByThread[thread.id]
            ?: (if (thread.phoneNumber.isNotBlank()) uiState.timelineByThread[thread.phoneNumber] else null)
            ?: (if (uiState.activeThreadId != null) uiState.timelineByThread[uiState.activeThreadId] else null)
            ?: emptyList()
    }
    
    var showAiSummary by remember { mutableStateOf(false) }
    var showContactInfo by remember { mutableStateOf(false) }
    var showMediaPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

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

    LaunchedEffect(timeline.size) {
        if (timeline.isNotEmpty()) {
            listState.animateScrollToItem(timeline.size - 1)
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
                            .clickable { showContactInfo = true }
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = thread.avatarColor.copy(alpha = 0.8f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(thread.contactName.take(1), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(thread.contactName, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                            Text(thread.phoneNumber.ifEmpty { "СМС" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showAiSummary = !showAiSummary }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Summary", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.makeCall(thread.phoneNumber.ifEmpty { thread.id }) }) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                AnimatedVisibility(
                    visible = showMediaPicker,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    MediaPickerRow(
                        onPhotoPick = { photoPicker.launch("image/*") },
                        onCameraPick = { cameraLauncher.launch(null) },
                        onFilePick = { documentPicker.launch("*/*") },
                        onStickerPick = { viewModel.sendMessage(thread.id, "😊 [Стикер]", mediaUri = "sticker_mock", mediaType = MediaType.STICKER) },
                        onGifPick = { viewModel.sendMessage(thread.id, "🎬 [GIF]", mediaUri = "gif_mock", mediaType = MediaType.GIF) },
                        onMapPick = { 
                            val intent = Intent(Intent.ACTION_VIEW, "yandexmaps://?pt=37.6,55.7&z=12&l=map".toUri())
                            try { context.startActivity(intent) } catch(_: Exception) {
                                viewModel.sendMessage(thread.id, "📍 Моё местоположение: 55.75, 37.62")
                            }
                        }
                    )
                }
                
                UnifiedMessageInput(
                    text = inputText,
                    onTextChange = { inputText = it },
                    selectedSimSlot = uiState.selectedSimSlot,
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
                    onAttachClick = { showMediaPicker = !showMediaPicker },
                    editingMessage = if (editingMessageId != null) "Редактирование" else null,
                    onCancelEdit = {
                        editingMessageId = null
                        inputText = ""
                    }
                )
            }
        }
    ) { padding ->
        if (showContactInfo) {
            ContactInfoDialog(
                thread = thread, 
                onDismiss = { showContactInfo = false },
                onSaveContact = {
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        type = ContactsContract.Contacts.CONTENT_TYPE
                        putExtra(ContactsContract.Intents.Insert.PHONE, thread.phoneNumber.ifEmpty { thread.id })
                        putExtra(ContactsContract.Intents.Insert.NAME, thread.contactName)
                    }
                    context.startActivity(intent)
                },
                onSaveToSim = {
                    viewModel.saveToSim(thread.contactName, thread.phoneNumber.ifEmpty { thread.id })
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
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    if (showAiSummary) { 
                        AiSummaryCard(timeline) 
                    }
                }
                
                items(timeline, key = { it.id }) { event ->
                    when (event) {
                        is TimelineEvent.Message -> MessageBubble(
                            message = event,
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedMessageForMenu = event
                            },
                            onRetry = { viewModel.sendMessage(thread.id, event.content) }
                        )
                        is TimelineEvent.Call -> CallEventCard(event, onRedial = { viewModel.makeCall(thread.phoneNumber.ifEmpty { thread.id }) })
                    }
                }
                
                if (uiState.pendingMessage != null) {
                    item { MessageBubble(uiState.pendingMessage!!, isPending = true) }
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
    onFilePick: () -> Unit,
    onStickerPick: () -> Unit,
    onGifPick: () -> Unit,
    onMapPick: () -> Unit
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
        MediaItem(Icons.AutoMirrored.Filled.StickyNote2, "Стикеры", onStickerPick)
        MediaItem(Icons.Default.Gif, "GIF", onGifPick)
        MediaItem(Icons.Default.Map, "Карты", onMapPick)
    }
}

@Composable
fun MediaItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun MessageBubble(
    message: TimelineEvent.Message, 
    isPending: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    val isMe = message.isFromMe
    val alignment = if (isMe) Alignment.End else Alignment.Start
    
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primary 
                     else MaterialTheme.colorScheme.surfaceVariant
    
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary 
                    else MaterialTheme.colorScheme.onSurfaceVariant
    
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
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (isMe) 20.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 20.dp
                ),
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .alpha(if (isPending) 0.6f else 1f)
                    .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress?.invoke() }) }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (message.mediaUri != null) { MediaContent(message) }
                    
                    if (message.content.isNotEmpty()) {
                        Text(
                            text = message.content, 
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 15.sp
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                    ) {
                        if (message.isEdited) {
                            Text("изм. ", style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.5f))
                        }
                        Text(
                            text = timeFormat.format(Date(message.timestamp)), 
                            style = MaterialTheme.typography.labelSmall, 
                            color = textColor.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = when (message.status) {
                                    MessageStatus.SENDING -> Icons.Default.Schedule
                                    MessageStatus.SENT -> Icons.Default.Check
                                    MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                    MessageStatus.ERROR -> Icons.Default.Error
                                },
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = textColor.copy(alpha = 0.6f)
                            )
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
fun AiSummaryCard(timeline: List<TimelineEvent>) {
    val messages = remember(timeline) { timeline.filterIsInstance<TimelineEvent.Message>() }
    val lastMessageText = remember(messages) { messages.lastOrNull()?.content ?: "Нет сообщений" }
    val summaryText = remember(messages) {
        if (messages.isEmpty()) {
            "В диалоге пока нет текстовых сообщений."
        } else {
            "Диалог содержит ${messages.size} сообщ. Последнее: \"${lastMessageText.take(60)}${if (lastMessageText.length > 60) "..." else ""}\""
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("ИИ Краткая сводка чата", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun UnifiedMessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    selectedSimSlot: Int = 1,
    onToggleSim: (Int) -> Unit = {},
    onSend: () -> Unit,
    isUndoVisible: Boolean,
    undoProgress: Float,
    onUndo: () -> Unit,
    onAttachClick: () -> Unit,
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
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAttachClick) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
            }
            
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Сообщение") },
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Surface(
                onClick = { onToggleSim(if (selectedSimSlot == 1) 2 else 1) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Text(
                    text = "SIM $selectedSimSlot",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.width(4.dp))
            
            IconButton(
                onClick = onSend,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
            }
        }
    }
}

@Composable
fun ContactInfoDialog(
    thread: MessageThread,
    onDismiss: () -> Unit,
    onSaveContact: () -> Unit,
    onSaveToSim: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(thread.contactName) },
        text = {
            Column {
                Text("Номер: ${thread.phoneNumber.ifEmpty { thread.id }}")
                Spacer(Modifier.height(16.dp))
                Button(onClick = onSaveContact, modifier = Modifier.fillMaxWidth()) {
                    Text("Сохранить в телефонную книгу")
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
