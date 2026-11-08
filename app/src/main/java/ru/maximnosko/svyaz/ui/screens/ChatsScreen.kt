package ru.maximnosko.svyaz.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import ru.maximnosko.svyaz.data.MessageThread
import ru.maximnosko.svyaz.ui.components.AvatarPickerBottomSheet
import ru.maximnosko.svyaz.ui.components.DialpadBottomSheet
import ru.maximnosko.svyaz.ui.components.ThreadItemSwipeable
import ru.maximnosko.svyaz.ui.components.UserAvatar
import ru.maximnosko.svyaz.ui.theme.MessagesTheme
import ru.maximnosko.svyaz.viewmodel.MessagesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    viewModel: MessagesViewModel,
    onThreadClick: (MessageThread) -> Unit,
    onNewChatClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showDialpadSheet by remember { mutableStateOf(false) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var newChatNumberInput by remember { mutableStateOf("") }
    var showProfileDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var logoRotationTarget by remember { mutableFloatStateOf(0f) }
    val logoRotation by animateFloatAsState(
        targetValue = logoRotationTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logoRotation"
    )
    var showLogoEasterEggDialog by remember { mutableStateOf(false) }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> viewModel.syncSystemData() }

    fun launchDefaultSmsAppRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS))
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            context.startActivity(intent)
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                Spacer(modifier = Modifier.statusBarsPadding())
                
                // Google Messages Style Top Bar (Logo + Search + Profile Avatar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .rotate(logoRotation)
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                logoRotationTarget += 360f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showLogoEasterEggDialog = true
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Связь ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Сообщения",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val searchRotation by animateFloatAsState(
                            targetValue = if (isSearchActive) 90f else 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                            label = "searchRotation"
                        )
                        IconButton(onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isSearchActive = !isSearchActive 
                        }) {
                            Box(modifier = Modifier.rotate(searchRotation)) {
                                Crossfade(targetState = isSearchActive, label = "chatSearchIcon") { active ->
                                    Icon(
                                        if (active) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        UserAvatar(
                            avatarUri = uiState.userAvatarUri,
                            userName = uiState.userName,
                            size = 36.dp,
                            onClick = { showProfileDialog = true }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(animationSpec = tween(150)) + shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Поиск сообщений и контактов...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
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

                // Category folders tabs ("Все", "Архив", "Корзина", "Личные", "Служебные")
                val folders = remember(uiState.customFolders) { uiState.allFolderNames }
                val selectedIndex = remember(uiState.selectedFolder, folders) { 
                    folders.indexOf(uiState.selectedFolder).coerceAtLeast(0) 
                }

                SecondaryScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(selectedIndex),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    folders.forEachIndexed { index, folderName ->
                        val selected = selectedIndex == index
                        Tab(
                            selected = selected,
                            onClick = { viewModel.onFolderSelected(folderName) },
                            text = {
                                Text(
                                    text = folderName,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            val fabInteractionSource = remember { MutableInteractionSource() }
            val isFabPressed by fabInteractionSource.collectIsPressedAsState()
            val fabScale by animateFloatAsState(
                targetValue = if (isFabPressed) 0.88f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "fabScale"
            )
            val fabRotation by animateFloatAsState(
                targetValue = if (showNewChatDialog) 45f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "fabRotation"
            )

            FloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                interactionSource = fabInteractionSource,
                modifier = Modifier
                    .padding(bottom = 76.dp)
                    .scale(fabScale)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "New Chat",
                    modifier = Modifier.rotate(fabRotation)
                )
            }
        }
    ) { padding ->
        if (showNewChatDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showNewChatDialog = false
                    newChatNumberInput = ""
                },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Новое сообщение",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Введите номер телефона получателя:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newChatNumberInput,
                            onValueChange = { newChatNumberInput = it },
                            placeholder = { Text("+7 999 123 45 67") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (uiState.isProModeUnlocked) {
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                onClick = {
                                    showNewChatDialog = false
                                    showDialpadSheet = true
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Открыть номеронабиратель", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val target = newChatNumberInput.trim()
                            if (target.isNotBlank()) {
                                showNewChatDialog = false
                                newChatNumberInput = ""
                                onNewChatClick(target)
                            }
                        },
                        enabled = newChatNumberInput.isNotBlank()
                    ) {
                        Text("Начать чат")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showNewChatDialog = false
                            newChatNumberInput = ""
                        }
                    ) {
                        Text("Отмена")
                    }
                }
            )
        }

        if (showDialpadSheet) {
            DialpadBottomSheet(
                onDismiss = { showDialpadSheet = false },
                onAction = { number, isCall ->
                    showDialpadSheet = false
                    if (isCall) viewModel.makeCall(number) else onNewChatClick(number)
                }
            )
        }

        if (showProfileDialog) {
            GoogleMessagesProfileDialog(
                viewModel = viewModel,
                onDismiss = { showProfileDialog = false },
                onSettingsClick = { 
                    showProfileDialog = false
                    onSettingsClick()
                }
            )
        }

        if (showLogoEasterEggDialog) {
            AlertDialog(
                onDismissRequest = { showLogoEasterEggDialog = false },
                icon = { Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)) },
                title = { Text("🚀 Режим Сверхсветовой Связи", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Вы раскрутили логотип «Связь» на 360°! Активирован сверхскоростной протокол со скоростью 120 FPS. Мессенджер готов к бета-тестированию в RuStore!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(onClick = { showLogoEasterEggDialog = false }) {
                        Text("Погнали!")
                    }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column {
                if (uiState.otpCode != null) {
                    OtpBanner(
                        code = uiState.otpCode!!.code,
                        sender = uiState.otpCode!!.sender,
                        expireMinutes = uiState.otpCode!!.expireTimeMinutes,
                        onCopy = { viewModel.copyOtp() },
                        onDismiss = { viewModel.copyOtp() }
                    )
                }

                // If NOT default SMS app: Display full Google Messages style default app prompt screen
                if (!uiState.isDefaultSmsApp) {
                    GoogleMessagesDefaultAppPrompt(
                        onSetDefault = { launchDefaultSmsAppRequest() }
                    )
                } else if (!uiState.isPermissionsGranted && uiState.threads.isEmpty() && !uiState.isLoading) {
                    PermissionRequiredState(onRequestPermissions)
                } else if ((uiState.isLoading || uiState.isSyncing) && uiState.threads.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Загрузка чатов...", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    val chats by viewModel.filteredThreads.collectAsState()
                    val searchEgg = remember(uiState.searchQuery) {
                        val q = uiState.searchQuery.trim().lowercase()
                        when {
                            q == "42" -> "🌌 Ответ на главный вопрос Вселенной найден: «Связь» соединяет людей в любых условиях даже без интернета!"
                            q == "matrix" || q == "матрица" -> "🕶️ Нео, ложки нет. Есть только децентрализованный Mesh-радар и локальное P2P шифрование."
                            q == "рустор" || q == "rustore" -> "🚀 Сборка 26.10.1 Beta создана для тестирования в RuStore. Полная независимость от внешних серверов!"
                            q == "пасхалка" || q == "egg" -> "🥚 Поздравляем, вы нашли секрет! Попробуйте нажать на логотип «Связь» в шапке или 7 раз на версию в Настройках!"
                            else -> null
                        }
                    }

                    if (chats.isEmpty() && !uiState.isSyncing) {
                        if (searchEgg != null) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 96.dp)
                            ) {
                                item {
                                    EasterEggSearchCard(searchEgg)
                                }
                            }
                        } else if (uiState.selectedFolder == "Корзина") {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 96.dp)
                            ) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error)
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text("Корзина (30 дней)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text("Чаты хранятся 30 дней, после чего автоматически удаляются", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                        Text("Корзина пуста", color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        } else if (uiState.selectedFolder == "Архив") {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 96.dp)
                            ) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Archive, null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text("Архив чатов", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text("Скрытые диалоги. Свайп вправо возвращает в основной список", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                        Text("Архив пуст", color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        } else {
                            EmptyChatsState()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            if (searchEgg != null) {
                                item {
                                    EasterEggSearchCard(searchEgg)
                                }
                            }

                            if (uiState.selectedFolder == "Корзина") {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error)
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text("Корзина (30 дней)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text("Чаты хранится 30 дней, после автоматически удаляются", style = MaterialTheme.typography.labelSmall)
                                            }
                                            TextButton(onClick = { viewModel.emptyTrash() }) {
                                                Text("Очистить", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            } else if (uiState.selectedFolder == "Архив") {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Archive, null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text("Архив чатов", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text("Скрытые диалоги. Свайп вправо возвращает в основной список", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }

                            itemsIndexed(
                                items = chats,
                                key = { index, thread -> "${thread.id}_$index" }
                            ) { index, thread ->
                                Column {
                                    ThreadItemSwipeable(
                                        modifier = Modifier.animateItem(),
                                        thread = thread,
                                        onClick = { onThreadClick(thread) },
                                        onPin = { viewModel.onPinThread(thread) },
                                        onArchive = { viewModel.onArchiveThread(thread) },
                                        onDelete = { viewModel.onDeleteThread(thread) },
                                        isPrivacyShieldEnabled = uiState.isPrivacyShieldEnabled
                                    )
                                    if (uiState.selectedFolder == "Корзина") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { viewModel.onRestoreThread(thread) }) {
                                                Icon(Icons.Default.Restore, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Восстановить", style = MaterialTheme.typography.labelMedium)
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            TextButton(onClick = { viewModel.onPermanentDeleteThread(thread) }) {
                                                Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                                Spacer(Modifier.width(4.dp))
                                                Text("Удалить навсегда", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    } else if (uiState.selectedFolder == "Архив") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { viewModel.onRestoreThread(thread) }) {
                                                Icon(Icons.Default.Unarchive, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Извлечь из архива", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pixel-Perfect Google Messages Style Default SMS App Prompt Screen (Matches Screenshot 1)
 */
@Composable
fun GoogleMessagesDefaultAppPrompt(
    onSetDefault: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Graphic illustration composition
        Surface(
            modifier = Modifier.size(240.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Чтобы использовать приложение \"Связь\", сделайте его приложением для SMS по умолчанию",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onSetDefault,
            shape = CircleShape, // Pill shape like in Google Messages
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Прил. для SMS по умолч.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Pixel-Perfect Google Messages Style Account Modal Dialog (Matches Screenshot 2)
 */
@Composable
fun GoogleMessagesProfileDialog(
    viewModel: MessagesViewModel,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(uiState.userName) }
    var showDataDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAvatarSheet by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateUserAvatarFromUri(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { viewModel.updateUserAvatarFromBitmap(it) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Right Close 'X' Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // 1. Account Info Top Card - Tap to enter / edit name or avatar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            avatarUri = uiState.userAvatarUri,
                            userName = uiState.userName,
                            size = 56.dp,
                            showCameraBadge = true,
                            onClick = { showAvatarSheet = true }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.userName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.userPhone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Google Style Account Management Pill Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditNameDialog = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Управление аккаунтом «Связь»",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Grouped Menu Card 1
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column {
                        GoogleMenuItem(
                            icon = Icons.Default.Person,
                            text = "Ваш профиль",
                            onClick = { showEditNameDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        GoogleMenuItem(
                            icon = Icons.Default.Archive,
                            text = "Архив и Папки",
                            onClick = {
                                onDismiss()
                                onSettingsClick()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        GoogleMenuItem(
                            icon = Icons.Default.DoneAll,
                            text = "Отметить все как прочитанные",
                            onClick = {
                                viewModel.syncSystemData()
                                onDismiss()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Grouped Menu Card 2
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column {
                        GoogleMenuItem(
                            icon = Icons.Default.Shield,
                            text = "Ваши данные в приложении \"Связь\"",
                            onClick = { showDataDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        GoogleMenuItem(
                            icon = Icons.Default.Settings,
                            text = "Настройки приложения \"Связь\"",
                            onClick = onSettingsClick
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        GoogleMenuItem(
                            icon = Icons.AutoMirrored.Filled.Help,
                            text = "Справка / отзыв",
                            onClick = { showHelpDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5. Bottom Footer Links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Политика конфиденциальности  •  Условия использования",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Ваше имя") },
            text = {
                Column {
                    Text("Введите имя пользователя для профиля «Связь»:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Введите имя") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateUserName(nameInput)
                    showEditNameDialog = false
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showDataDialog) {
        AlertDialog(
            onDismissRequest = { showDataDialog = false },
            title = { Text("Ваши данные в «Связи»") },
            text = { Text("Все ваши сообщения, контакты и журналы звонков хранятся локально в зашифрованной базе данных SQLCipher.") },
            confirmButton = { TextButton(onClick = { showDataDialog = false }) { Text("Понятно") } }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Справка и обратная связь") },
            text = { Text("Версия приложения: ${ru.maximnosko.svyaz.BuildConfig.VERSION_NAME}. Отправьте ваши предложения или отчеты об ошибках на почту поддержки.") },
            confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Понятно") } }
        )
    }

    if (showAvatarSheet) {
        AvatarPickerBottomSheet(
            onDismiss = { showAvatarSheet = false },
            onPickGallery = { galleryLauncher.launch("image/*") },
            onTakePhoto = { cameraLauncher.launch(null) },
            onDeletePhoto = if (!uiState.userAvatarUri.isNullOrBlank()) {
                { viewModel.removeUserAvatar() }
            } else null
        )
    }
}

@Composable
fun GoogleMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun OtpBanner(
    code: String,
    sender: String,
    expireMinutes: Int? = null,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (expireMinutes != null) "Код от $sender (Истекает через $expireMinutes мин)" else "Код подтверждения от $sender",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(code, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Button(onClick = onCopy, shape = MaterialTheme.shapes.small) {
                Text("Копировать")
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null)
            }
        }
    }
}

@Composable
fun PermissionRequiredState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Security, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Требуются разрешения", style = MaterialTheme.typography.titleLarge)
        Text("Для работы нужен доступ к данным.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Предоставить доступ")
        }
    }
}

@Composable
fun EmptyChatsState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Здесь пока пусто", color = MaterialTheme.colorScheme.outline)
    }
}

@Preview(showBackground = true)
@Composable
fun ChatsScreenPreview() {
    MessagesTheme {
        ChatsScreen(
            viewModel = viewModel(), 
            onThreadClick = {}, 
            onNewChatClick = {}, 
            onSettingsClick = {},
            onRequestPermissions = {}
        )
    }
}

@Composable
fun EasterEggSearchCard(message: String) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✨",
                fontSize = 28.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Секретная находка!",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
