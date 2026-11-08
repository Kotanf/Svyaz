package ru.maximnosko.svyaz.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.maximnosko.svyaz.ui.components.SvyazGlassCard
import ru.maximnosko.svyaz.ui.components.SvyazIconBadge
import ru.maximnosko.svyaz.ui.components.SvyazPillTag
import ru.maximnosko.svyaz.ui.theme.SvyazBrandColors
import ru.maximnosko.svyaz.viewmodel.MessagesViewModel
import kotlin.math.cos
import kotlin.math.sin

data class RadarNode(
    val id: String,
    val name: String,
    val distanceMeters: Int,
    val rssiDbm: Int,
    val isEmergency: Boolean = false,
    val angleDeg: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineRadarScreen(
    viewModel: MessagesViewModel,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var radarTapCount by remember { mutableIntStateOf(0) }
    var showUfoNode by remember { mutableStateOf(false) }

    val mockNodes = remember(showUfoNode) {
        val base = listOf(
            RadarNode("node_1", "Узел Связь-Альфа", 25, -62, false, 45f),
            RadarNode("node_2", "Узел Связь-Норд", 45, -76, false, 160f),
            RadarNode("node_3", "SOS-Маяк (Группа Поиск)", 85, -89, true, 280f)
        )
        if (showUfoNode) {
            base + RadarNode("ufo_99", "🛸 Неопознанный объект (433 МГц)", 12, -45, false, 210f)
        } else base
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Офлайн-радар", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Локальная связь без интернета",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Interactive Radar Canvas
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(vertical = 12.dp)
                    .clip(CircleShape)
                    .clickable {
                        radarTapCount++
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (radarTapCount >= 5 && !showUfoNode) {
                            showUfoNode = true
                            radarTapCount = 0
                            scope.launch {
                                snackbarHostState.showSnackbar("🛸 Внимание! Обнаружен внеземной радиосигнал на частоте 433 МГц!")
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outlineVariant
                val errorColor = MaterialTheme.colorScheme.error

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val maxRadius = size.minDimension / 2 - 8f

                    // Concentric Range Rings
                    for (i in 1..3) {
                        drawCircle(
                            color = outlineColor.copy(alpha = 0.35f),
                            radius = maxRadius * (i / 3f),
                            center = center,
                            style = Stroke(width = 1.5f)
                        )
                    }

                    // Expanding Pulse Wave
                    drawCircle(
                        color = primaryColor.copy(alpha = (1f - pulseRadius).coerceIn(0f, 0.4f)),
                        radius = maxRadius * pulseRadius,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )

                    // Sweeping Line
                    val sweepRad = Math.toRadians(sweepAngle.toDouble())
                    val sweepEnd = Offset(
                        x = (center.x + maxRadius * cos(sweepRad)).toFloat(),
                        y = (center.y + maxRadius * sin(sweepRad)).toFloat()
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.7f),
                        start = center,
                        end = sweepEnd,
                        strokeWidth = 2f
                    )

                    // Center Self Node
                    drawCircle(color = primaryColor, radius = 6f, center = center)

                    // Nearby Nodes blips
                    mockNodes.forEach { node ->
                        val nodeRad = Math.toRadians(node.angleDeg.toDouble())
                        val distRatio = (node.distanceMeters / 100f).coerceIn(0.2f, 0.95f)
                        val blipOffset = Offset(
                            x = (center.x + maxRadius * distRatio * cos(nodeRad)).toFloat(),
                            y = (center.y + maxRadius * distRatio * sin(nodeRad)).toFloat()
                        )
                        val nodeColor = if (node.isEmergency) errorColor else primaryColor
                        drawCircle(color = nodeColor, radius = 5.5f, center = blipOffset)
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.35f),
                            radius = 11f,
                            center = blipOffset,
                            style = Stroke(width = 1.5f)
                        )
                    }
                }
            }

            // Brandbook Feature Tags Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SvyazPillTag(
                    text = "Офлайн: P2P",
                    color = SvyazBrandColors.Amber,
                    icon = Icons.Default.WifiOff
                )
                SvyazPillTag(
                    text = "Локальная сеть",
                    color = SvyazBrandColors.Sky,
                    icon = Icons.Default.Share
                )
                SvyazPillTag(
                    text = "Шифрование",
                    color = SvyazBrandColors.Teal,
                    icon = Icons.Default.Shield
                )
                SvyazPillTag(
                    text = "Прямой чат",
                    color = SvyazBrandColors.Fuchsia,
                    icon = Icons.AutoMirrored.Filled.Chat
                )
            }

            Spacer(Modifier.height(10.dp))

            // Beacon Mode Card with Brandbook Glassmorphism
            SvyazGlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SvyazIconBadge(
                        icon = Icons.Default.WifiTethering,
                        color = SvyazBrandColors.Amber,
                        size = 44.dp
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Режим маяка (Я в сети / SOS)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Транслировать позывной через Bluetooth LE",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Switch(
                        checked = uiState.isMeshModeEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleMeshMode(it)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (it) "📡 Режим маяка активирован. Вы видны в локальной сети"
                                    else "Режим маяка отключен"
                                )
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Обнаружено поблизости (${mockNodes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                SvyazPillTag(
                    text = "Частота: 2.4 ГГц",
                    color = SvyazBrandColors.NeonCyan
                )
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(mockNodes) { node ->
                    SvyazGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = if (node.isEmergency)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        borderColor = if (node.isEmergency)
                            SvyazBrandColors.Rose.copy(alpha = 0.35f)
                        else Color.White.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SvyazIconBadge(
                                icon = if (node.isEmergency) Icons.Default.Warning else Icons.Default.WifiTethering,
                                color = if (node.isEmergency) SvyazBrandColors.Rose else SvyazBrandColors.NeonCyan,
                                size = 42.dp
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = node.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "~${node.distanceMeters} м",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SvyazBrandColors.NeonCyan,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = " • Сигнал: ${node.rssiDbm} dBm",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("⚡ Пинг до ${node.name}: 14 мс (Bluetooth LE Mesh)")
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Пинг", fontSize = 12.sp)
                            }

                            Spacer(Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onOpenChat(node.name)
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat, 
                                    contentDescription = "Чат", 
                                    tint = SvyazBrandColors.NeonCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
