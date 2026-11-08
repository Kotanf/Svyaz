package ru.maximnosko.svyaz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.maximnosko.svyaz.ui.components.DialpadBottomSheet
import ru.maximnosko.svyaz.ui.components.SvyazCallStatusBadge
import ru.maximnosko.svyaz.ui.theme.MessagesTheme
import ru.maximnosko.svyaz.ui.theme.SvyazBrandColors
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
                    onHangup = {
                        // Actually end the telecom call, then close the UI.
                        CallService.hangUp()
                        finish()
                    }
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
    val isAiEnhanced = true
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

    // Cosmic gradient background matching Brandbook cosmic-bg
    val cosmicBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF161832),
            Color(0xFF101222),
            Color(0xFF0B0C16)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cosmicBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Contact Info with Sky Avatar Ring & Call Status Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                // Sky outer glowing avatar ring (bg-sky-400/15 ring-1 ring-sky-300/30)
                Surface(
                    modifier = Modifier.size(114.dp),
                    shape = CircleShape,
                    color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                    border = BorderStroke(2.dp, Color(0xFF7DD3FC).copy(alpha = 0.35f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = contactName.take(1).uppercase(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7DD3FC)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = contactName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA5B4FC).copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Brandbook Call Status Badge
                SvyazCallStatusBadge(
                    text = if (isOnHold) "Удержание..." else "В вызове"
                )

                // Monospace Call Timer (call-time 02:47)
                Text(
                    text = formattedDuration,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOnHold) SvyazBrandColors.Rose else Color(0xFFA5B4FC),
                    modifier = Modifier.padding(top = 10.dp)
                )
                
                // AI Quality Badge
                if (isAiEnhanced) {
                    Surface(
                        color = SvyazBrandColors.Teal.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, SvyazBrandColors.Teal.copy(alpha = 0.25f)),
                        shape = CircleShape,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome, 
                                contentDescription = null, 
                                tint = SvyazBrandColors.Teal,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "ИИ Улучшение голоса HD", 
                                color = SvyazBrandColors.Teal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Controls & Hangup
            Column(
                modifier = Modifier.padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
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
                        activeColor = SvyazBrandColors.RoseDanger,
                        onClick = { isRecording = !isRecording }
                    )
                    CallControlButton(
                        icon = if (isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                        label = "Удержание",
                        isActive = isOnHold,
                        activeColor = SvyazBrandColors.Amber,
                        onClick = { isOnHold = !isOnHold }
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Brandbook Rose Hangup Button with Glow
                Surface(
                    onClick = onHangup,
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    color = SvyazBrandColors.RoseDanger.copy(alpha = 0.22f),
                    border = BorderStroke(1.5.dp, SvyazBrandColors.Rose.copy(alpha = 0.45f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Hangup",
                            tint = SvyazBrandColors.Rose,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Bottom security and audio status bar (from Brandbook mockup)
                Row(
                    modifier = Modifier
                        .padding(top = 28.dp)
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "HD-аудио",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA5B4FC).copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.width(24.dp))
                    Text(
                        "•",
                        color = Color(0xFFA5B4FC).copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.width(24.dp))
                    Text(
                        "Шифрование: ✓",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA5B4FC).copy(alpha = 0.65f),
                        fontSize = 12.sp
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
    activeColor: Color = SvyazBrandColors.NeonCyan,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(62.dp),
            shape = CircleShape,
            color = if (isActive) activeColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, if (isActive) activeColor.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.14f)),
            contentColor = if (isActive) activeColor else Color(0xFFA5B4FC)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) activeColor else Color(0xFFA5B4FC).copy(alpha = 0.85f),
            fontSize = 11.sp
        )
    }
}
