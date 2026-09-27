package ru.maximnosko.svyaz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.maximnosko.svyaz.ui.components.DialpadBottomSheet
import ru.maximnosko.svyaz.ui.theme.MessagesTheme
import java.util.Locale

class CallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MessagesTheme {
                CallScreen(
                    contactName = intent.getStringExtra("name") ?: "Неизвестный номер",
                    phoneNumber = intent.getStringExtra("number") ?: "+7 (000) 000-00-00",
                    onHangup = { finish() }
                )
            }
        }
    }
}

@Composable
fun CallScreen(
    contactName: String,
    phoneNumber: String,
    onHangup: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isOnHold by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    val isAiEnhanced by remember { mutableStateOf(true) }
    var showDialpadSheet by remember { mutableStateOf(false) }
    var showAddParticipantDialog by remember { mutableStateOf(false) }

    var callDurationSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isOnHold) {
        while (!isOnHold) {
            delay(1000L)
            callDurationSeconds++
        }
    }

    val formattedDuration = remember(callDurationSeconds) {
        val mins = callDurationSeconds / 60
        val secs = callDurationSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Contact Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 64.dp)
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = contactName.take(1),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = contactName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isOnHold) "Удержание..." else formattedDuration,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isOnHold) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
                
                // AI Quality Badge
                if (isAiEnhanced) {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome, 
                                null, 
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "ИИ Улучшение качества", 
                                color = Color(0xFF4CAF50),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Controls
            Column(
                modifier = Modifier.padding(bottom = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallControlButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = "Звук",
                        isActive = isMuted,
                        onClick = { isMuted = !isMuted }
                    )
                    CallControlButton(
                        icon = Icons.Default.Apps,
                        label = "Клавиши",
                        isActive = showDialpadSheet,
                        onClick = { showDialpadSheet = true }
                    )
                    CallControlButton(
                        icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                        label = "Динамик",
                        isActive = isSpeakerOn,
                        onClick = { isSpeakerOn = !isSpeakerOn }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallControlButton(
                        icon = Icons.Default.PersonAdd,
                        label = "Добавить",
                        onClick = { showAddParticipantDialog = true }
                    )
                    CallControlButton(
                        icon = if (isRecording) Icons.Default.RadioButtonChecked else Icons.Default.FiberManualRecord,
                        label = "Запись",
                        isActive = isRecording,
                        activeColor = Color.Red,
                        onClick = { isRecording = !isRecording }
                    )
                    CallControlButton(
                        icon = if (isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                        label = "Удержание",
                        isActive = isOnHold,
                        onClick = { isOnHold = !isOnHold }
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Hangup Button
                FloatingActionButton(
                    onClick = onHangup,
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Hangup",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }

    if (showDialpadSheet) {
        DialpadBottomSheet(
            onDismiss = { showDialpadSheet = false },
            onAction = { _, _ -> showDialpadSheet = false }
        )
    }

    if (showAddParticipantDialog) {
        AlertDialog(
            onDismissRequest = { showAddParticipantDialog = false },
            title = { Text("Добавить участника") },
            text = { Text("Конференц-связь: выберите участника для объединения вызова.") },
            confirmButton = {
                TextButton(onClick = { showAddParticipantDialog = false }) { Text("Понятно") }
            }
        )
    }
}

@Composable
fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = if (isActive) activeColor else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
