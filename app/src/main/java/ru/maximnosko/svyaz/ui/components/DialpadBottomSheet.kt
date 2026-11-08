package ru.maximnosko.svyaz.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DialpadKey(val main: String, val sub: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialpadBottomSheet(
    onDismiss: () -> Unit,
    onAction: (String, Boolean) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var number by remember { mutableStateOf("") }
            val haptic = LocalHapticFeedback.current

            // Display Number Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = number.ifEmpty { "Введите номер" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (number.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                AnimatedVisibility(
                    visible = number.isNotEmpty(),
                    enter = fadeIn() + scaleIn(animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            number = number.dropLast(1)
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Удалить символ",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val rows = listOf(
                listOf(DialpadKey("1", ""), DialpadKey("2", "ABC"), DialpadKey("3", "DEF")),
                listOf(DialpadKey("4", "GHI"), DialpadKey("5", "JKL"), DialpadKey("6", "MNO")),
                listOf(DialpadKey("7", "PQRS"), DialpadKey("8", "TUV"), DialpadKey("9", "WXYZ")),
                listOf(DialpadKey("*", ""), DialpadKey("0", "+"), DialpadKey("#", ""))
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                rows.forEach { rowKeys ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowKeys.forEach { key ->
                            DialpadKeyButton(
                                key = key,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    number += key.main
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons (Call / SMS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val chatInteractionSource = remember { MutableInteractionSource() }
                val isChatPressed by chatInteractionSource.collectIsPressedAsState()
                val chatScale by animateFloatAsState(
                    targetValue = if (isChatPressed) 0.92f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                    label = "chatScale"
                )

                FilledTonalButton(
                    onClick = {
                        if (number.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAction(number, false)
                        }
                    },
                    interactionSource = chatInteractionSource,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .scale(chatScale),
                    shape = RoundedCornerShape(16.dp),
                    enabled = number.isNotEmpty(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Чат", style = MaterialTheme.typography.titleSmall)
                }

                val callInteractionSource = remember { MutableInteractionSource() }
                val isCallPressed by callInteractionSource.collectIsPressedAsState()
                val callScale by animateFloatAsState(
                    targetValue = if (isCallPressed) 0.92f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                    label = "callScale"
                )

                Button(
                    onClick = {
                        if (number.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAction(number, true)
                        }
                    },
                    interactionSource = callInteractionSource,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .scale(callScale),
                    shape = RoundedCornerShape(16.dp),
                    enabled = number.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Вызов", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun DialpadKeyButton(
    key: DialpadKey,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "dialKeyScale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(68.dp)
            .scale(scale),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = key.main,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (key.sub.isNotEmpty()) {
                Text(
                    text = key.sub,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
