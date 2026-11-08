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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import ru.maximnosko.svyaz.ui.components.AvatarPickerBottomSheet
import ru.maximnosko.svyaz.ui.components.SvyazIconBadge
import ru.maximnosko.svyaz.ui.components.UserAvatar
import ru.maximnosko.svyaz.ui.theme.AppFontScale
import ru.maximnosko.svyaz.ui.theme.AppThemePreset
import ru.maximnosko.svyaz.ui.theme.SvyazBrandColors
import ru.maximnosko.svyaz.ui.theme.ThemeMode
import ru.maximnosko.svyaz.ui.theme.MessagesTheme
import ru.maximnosko.svyaz.viewmodel.MessagesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MessagesViewModel,
    onBack: () -> Unit,
    onNavigateToFolders: () -> Unit = {},
    onNavigateToNetwork: () -> Unit = {},
    onNavigateToServices: () -> Unit = {},
    onNavigateToRadar: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showComplianceDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(uiState.userName) }
    var showAvatarSheet by remember { mutableStateOf(false) }
    var versionTapCount by remember { mutableIntStateOf(0) }
    var showDeveloperEasterEgg by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

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

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> viewModel.syncSystemData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .padding(16.dp)
            ) {
                UserAvatar(
                    avatarUri = uiState.userAvatarUri,
                    userName = uiState.userName,
                    size = 88.dp,
                    showCameraBadge = true,
                    onClick = { showAvatarSheet = true }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { showEditNameDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        uiState.userName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    uiState.userPhone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            // Чит-код режим: функции активируются через 5 тапов по версии приложения
            if (uiState.isProModeUnlocked) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Секретный режим «Связь Pro»",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Все расширенные функции активны",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        TextButton(onClick = { viewModel.toggleProMode() }) {
                            Text("Скрыть")
                        }
                    }
                }
            }

            val showExtraFeatures = uiState.isProModeUnlocked
            if (showExtraFeatures) {
                Spacer(modifier = Modifier.height(16.dp))
                
                SettingsActionCard(
                    title = "Папки чатов (Автосортировка)",
                    icon = Icons.Default.FolderSpecial,
                    iconColor = SvyazBrandColors.Fuchsia,
                    onClick = onNavigateToFolders
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                SettingsActionCard(
                    title = "СМС-сервисы и таймер парковки",
                    icon = Icons.Default.LocalParking,
                    iconColor = SvyazBrandColors.NeonCyan,
                    onClick = onNavigateToServices
                )

                Spacer(modifier = Modifier.height(10.dp))
                
                SettingsActionCard(
                    title = "Данные о сотовой вышке и сигнале",
                    icon = Icons.Default.CellTower,
                    iconColor = SvyazBrandColors.Sky,
                    onClick = onNavigateToNetwork
                )

                Spacer(modifier = Modifier.height(10.dp))

                SettingsActionCard(
                    title = "Офлайн-радар: Ближняя связь (Bluetooth / Wi-Fi)",
                    icon = Icons.Default.WifiTethering,
                    iconColor = SvyazBrandColors.Amber,
                    onClick = onNavigateToRadar
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader("Безопасность: Деньги и Коды")

                ListItem(
                    headlineContent = { Text("Режим «Анти-взгляд» (Маскировка)") },
                    supportingContent = { Text("Скрывать коды и баланс в чатах от посторонних глаз") },
                    leadingContent = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.isPrivacyShieldEnabled,
                            onCheckedChange = { viewModel.togglePrivacyShield(it) }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text("Скрывать финансы в уведомлениях") },
                    supportingContent = { Text("Не показывать суммы и баланс на экране блокировки") },
                    leadingContent = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.hideFinanceInNotifications,
                            onCheckedChange = { viewModel.toggleHideFinance(it) }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text("Скрывать коды в уведомлениях") },
                    supportingContent = { Text("Не показывать SMS OTP-коды без разблокировки") },
                    leadingContent = { Icon(Icons.Default.Password, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.hideCodesInNotifications,
                            onCheckedChange = { viewModel.toggleHideCodes(it) }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text("Защита биометрией (Отпечаток/Face)") },
                    supportingContent = { Text("Доступ к важным чатам и кодам по биометрии") },
                    leadingContent = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.biometricAuthEnabled,
                            onCheckedChange = { viewModel.toggleBiometricAuth(it) }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text("Запрет скриншотов (FLAG_SECURE)") },
                    supportingContent = { Text("Запретить снимок экрана банковских и код-чатов") },
                    leadingContent = { Icon(Icons.Default.Security, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = uiState.screenshotBlockEnabled,
                            onCheckedChange = { viewModel.toggleScreenshotBlock(it) }
                        )
                    }
                )
            }



            SectionHeader("Сообщения и Настройки")
            
            ListItem(
                headlineContent = { Text("Отчёты о доставке") },
                supportingContent = { Text("Запрашивать подтверждение получения") },
                leadingContent = { Icon(Icons.Default.DoneAll, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = uiState.deliveryReportsEnabled,
                        onCheckedChange = { viewModel.toggleDeliveryReports(it) }
                    )
                }
            )
            
            ListItem(
                headlineContent = { Text("Период отмены отправки") },
                supportingContent = { Text("${uiState.undoSendPeriodMs / 1000} сек") },
                leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                trailingContent = {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf(0L, 3000L, 5000L, 10000L).forEach { ms ->
                                DropdownMenuItem(
                                    text = { Text("${ms / 1000} сек") },
                                    onClick = {
                                        viewModel.setUndoSendPeriod(ms)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )

            SectionHeader("Право и Кэш")
            
            ProfileMenuItem(
                icon = Icons.Default.DeleteForever,
                title = "Очистить локальный кэш",
                subtitle = "Удалить временные данные и пересинхронизировать",
                onClick = { viewModel.clearCacheAndResync() }
            )

            SectionHeader("Оформление и Темы")

            // 1. Theme Mode: System / Light / Dark
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Режим отображения",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        val isSelected = uiState.themeMode == mode
                        SegmentedButton(
                            selected = isSelected,
                            onClick = {
                                try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                viewModel.setThemeMode(mode)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                            icon = {
                                val iconVector = when (mode) {
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                }
                                Icon(iconVector, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        ) {
                            Text(mode.title, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. 6 Theme Presets Swatches
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Цветовая палитра («Связь» Брендбук & Темы)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppThemePreset.entries.forEach { preset ->
                        val isSelected = uiState.themePreset == preset && !uiState.isDynamicColorEnabled
                        val ringBorderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                    viewModel.setThemePreset(preset)
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(preset.primaryPreviewColor)
                                    .then(
                                        if (isSelected) {
                                            Modifier.padding(3.dp).clip(CircleShape)
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Выбрано",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Font Size Scaling (Segmented Buttons + Interactive Slider)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Размер шрифта",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(uiState.fontScale.scale * 100).toInt()}% (${uiState.fontScale.title})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AppFontScale.entries.forEachIndexed { index, scale ->
                        val isSelected = uiState.fontScale == scale
                        SegmentedButton(
                            selected = isSelected,
                            onClick = {
                                try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                viewModel.setFontScale(scale)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = AppFontScale.entries.size)
                        ) {
                            Text(scale.title, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Slider for fine font scaling
                val fontIndex = remember(uiState.fontScale) {
                    AppFontScale.entries.indexOf(uiState.fontScale).coerceAtLeast(0).toFloat()
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    Text("A", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Slider(
                        value = fontIndex,
                        onValueChange = { newIdx ->
                            val targetIndex = newIdx.toInt().coerceIn(0, AppFontScale.entries.lastIndex)
                            if (AppFontScale.entries[targetIndex] != uiState.fontScale) {
                                try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                                viewModel.setFontScale(AppFontScale.entries[targetIndex])
                            }
                        },
                        valueRange = 0f..3f,
                        steps = 2,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text("A", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Live Preview Card of Text Size
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Предпросмотр сообщения:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Text(
                                text = "Привет! Мессенджер «Связь» работает плавно, быстро и без интернета 🚀",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Material You Toggle
            ListItem(
                headlineContent = { Text("Адаптивные цвета Material You") },
                supportingContent = { Text("Цвета подстраиваются под систему и обои (Android 12+)") },
                leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = uiState.isDynamicColorEnabled,
                        onCheckedChange = { viewModel.toggleDynamicColor(it) }
                    )
                }
            )

            // 5. 120Hz Smoothness Badge
            ListItem(
                headlineContent = { Text("Плавность 120 Гц") },
                supportingContent = { Text("Высокая частота обновления экрана активна") },
                leadingContent = { Icon(Icons.Default.Speed, contentDescription = null) },
                trailingContent = {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = "120 FPS",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            SectionHeader("Системные настройки")

            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.Chat,
                title = "Приложение для СМС по умолчанию",
                subtitle = "Сделать «Связь» основным приложением",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                        roleLauncher.launch(intent)
                    } else {
                        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                        context.startActivity(intent)
                    }
                }
            )
            
            val showDefaultDialer = uiState.isProModeUnlocked
            if (showDefaultDialer) {
                ProfileMenuItem(
                    icon = Icons.Default.Call,
                    title = "Телефон по умолчанию",
                    subtitle = "Для управления вызовами и записью",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                            roleLauncher.launch(intent)
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Версия ${ru.maximnosko.svyaz.BuildConfig.VERSION_NAME}${if (uiState.isProModeUnlocked) " (Pro)" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.isProModeUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        versionTapCount++
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (versionTapCount in 2..4) {
                            val remaining = 5 - versionTapCount
                            viewModel.showSnackBar("Осталось $remaining ${if (remaining == 1) "нажатие" else "нажатия"} до активации секретных функций...")
                        } else if (versionTapCount >= 5) {
                            versionTapCount = 0
                            val activated = viewModel.toggleProMode()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (activated) {
                                viewModel.showSnackBar("🔓 Чит-код активирован! Все скрытые функции «Связь Pro» открыты.")
                            } else {
                                viewModel.showSnackBar("🔒 Режим «Связь Pro» отключен. Возврат к простому СМС-мессенджеру.")
                            }
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showDeveloperEasterEgg) {
            DeveloperTerminalEasterEggDialog(onDismiss = { showDeveloperEasterEgg = false })
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

        if (showComplianceDialog) {
            AlertDialog(
                onDismissRequest = { showComplianceDialog = false },
                title = { Text("Соответствие ОРИ (ФЗ-149)") },
                text = {
                    Text("Приложение «Связь» соблюдает требования ФЗ-149. Записи аудита и метаданные защищены и хранятся локально в зашифрованной БД SQLCipher.")
                },
                confirmButton = {
                    TextButton(onClick = { showComplianceDialog = false }) { Text("Понятно") }
                }
            )
        }
    }
}

@Composable
fun SettingsActionCard(
    title: String,
    icon: ImageVector = Icons.Default.Folder,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SvyazIconBadge(
                icon = icon,
                color = iconColor,
                size = 40.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Go",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun DeveloperTerminalEasterEggDialog(onDismiss: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var confettiCount by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D1117),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SVYAZ // DEV_CORE",
                    color = Color(0xFF00FF66),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
                Badge(containerColor = Color(0xFF1F6FEB)) {
                    Text(
                        text = "RuStore Alpha",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22), shape = RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = """
                    > [KERNEL] Svyaz Mesh Core 26.10.1
                    > [DISPLAY] 120 Hz Ultra-Smooth Active
                    > [NETWORK] Offline Bluetooth & Wi-Fi Mesh
                    > [SECURITY] SQLCipher + Biometrics + Shield
                    > [RUSTORE] Package verified for Alpha testing
                    > [STATUS] Вы открыли секретный терминал!
                    """.trimIndent(),
                    color = Color(0xFF58A6FF),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                if (confettiCount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "🎉 Салют запущен (x$confettiCount)! Связь работает везде без интернета! 🛰️✨",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    confettiCount++
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636))
            ) {
                Text("🎉 Запустить салют", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = Color(0xFF8B949E))
            }
        }
    )
}

