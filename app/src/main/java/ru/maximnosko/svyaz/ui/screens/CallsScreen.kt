package ru.maximnosko.svyaz.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.maximnosko.svyaz.data.CallLogItem
import ru.maximnosko.svyaz.ui.components.DialpadBottomSheet
import ru.maximnosko.svyaz.ui.theme.SvyazBrandColors
import ru.maximnosko.svyaz.viewmodel.MessagesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    viewModel: MessagesViewModel,
    onCallClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var showDialpadSheet by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCallForInfo by remember { mutableStateOf<CallLogItem?>(null) }

    val filters = listOf("Все вызовы", "Пропущенные")

    val filteredCalls = remember(uiState.callLogs, selectedFilterIndex, searchQuery) {
        val base = if (selectedFilterIndex == 1) {
            uiState.callLogs.filter { it.isMissed }
        } else {
            uiState.callLogs
        }
        if (searchQuery.isBlank()) base else base.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.number.contains(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Вызовы", style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        val searchRotation by animateFloatAsState(
                            targetValue = if (isSearchActive) 90f else 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                            label = "callSearchRotation"
                        )
                        IconButton(onClick = { isSearchActive = !isSearchActive }) {
                            Box(modifier = Modifier.rotate(searchRotation)) {
                                Crossfade(targetState = isSearchActive, label = "callSearchCrossfade") { active ->
                                    Icon(
                                        if (active) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = "Поиск"
                                    )
                                }
                            }
                        }
                    }
                )

                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(animationSpec = tween(150)) + shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Поиск звонков по имени или номеру...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    filters.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = selectedFilterIndex == index,
                            onClick = { selectedFilterIndex = index },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = filters.size)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            val haptic = LocalHapticFeedback.current
            val fabInteractionSource = remember { MutableInteractionSource() }
            val isFabPressed by fabInteractionSource.collectIsPressedAsState()
            val fabScale by animateFloatAsState(
                targetValue = if (isFabPressed) 0.88f else 1.0f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                label = "callFabScale"
            )

            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showDialpadSheet = true
                },
                interactionSource = fabInteractionSource,
                modifier = Modifier
                    .padding(bottom = 76.dp)
                    .scale(fabScale),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Call, contentDescription = "Новый вызов")
            }
        }
    ) { padding ->
        if (showDialpadSheet) {
            DialpadBottomSheet(
                onDismiss = { showDialpadSheet = false },
                onAction = { number, isCall ->
                    showDialpadSheet = false
                    if (isCall) viewModel.makeCall(number) else onCallClick(number)
                }
            )
        }

        if (selectedCallForInfo != null) {
            AlertDialog(
                onDismissRequest = { selectedCallForInfo = null },
                title = { Text(selectedCallForInfo!!.name) },
                text = {
                    Column {
                        Text("Номер: ${selectedCallForInfo!!.number.ifEmpty { "Неизвестен" }}")
                        Text("Тип: ${selectedCallForInfo!!.type}")
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val num = selectedCallForInfo!!.number
                                    selectedCallForInfo = null
                                    viewModel.makeCall(num)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Позвонить")
                            }
                            OutlinedButton(
                                onClick = {
                                    val num = selectedCallForInfo!!.number
                                    selectedCallForInfo = null
                                    onCallClick(num)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Чат")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedCallForInfo = null }) { Text("Закрыть") }
                }
            )
        }

        if (uiState.isLoading && uiState.callLogs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredCalls.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("История вызовов пуста", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(filteredCalls, key = { it.id }) { call ->
                    CallListItem(
                        call = call,
                        timeStr = ru.maximnosko.svyaz.ui.components.formatChatDate(call.timestamp),
                        onClick = { onCallClick(call.number) },
                        onInfoClick = { selectedCallForInfo = call }
                    )
                }
            }
        }
    }
}

@Composable
fun CallListItem(
    call: CallLogItem,
    timeStr: String,
    onClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    ListItem(
        headlineContent = { 
            Text(
                text = call.name, 
                fontWeight = FontWeight.Bold,
                color = if (call.isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            ) 
        },
        supportingContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (call.isMissed) Icons.AutoMirrored.Filled.CallMissed 
                                  else Icons.AutoMirrored.Filled.CallReceived, 
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (call.isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(call.type, style = MaterialTheme.typography.bodySmall) 
            }
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = call.avatarColor,
                border = BorderStroke(1.5.dp, Color(0xFF7DD3FC).copy(alpha = 0.35f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(call.name.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(timeStr, style = MaterialTheme.typography.labelSmall, color = Color(0xFFA5B4FC).copy(alpha = 0.7f))
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onInfoClick) {
                    Icon(Icons.Default.Info, contentDescription = "Инфо", tint = SvyazBrandColors.NeonCyan)
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}
