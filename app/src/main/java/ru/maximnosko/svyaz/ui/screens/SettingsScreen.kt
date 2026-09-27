package ru.maximnosko.svyaz.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.maximnosko.svyaz.ui.theme.MessagesTheme
import ru.maximnosko.svyaz.viewmodel.MessagesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MessagesViewModel,
    onBack: () -> Unit,
    onNavigateToFolders: () -> Unit = {},
    onNavigateToNetwork: () -> Unit = {},
    onNavigateToServices: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showComplianceDialog by remember { mutableStateOf(false) }
    var showMeshDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(uiState.userName) }

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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .clickable { showEditNameDialog = true }
                    .padding(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            uiState.userName.take(1).uppercase().ifEmpty { "Я" },
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsActionCard(
                title = "Папки чатов (Автосортировка)",
                onClick = onNavigateToFolders
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingsActionCard(
                title = "СМС-сервисы и таймер парковки",
                onClick = onNavigateToServices
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            SettingsActionCard(
                title = "Данные о сотовой вышке и сигнале",
                onClick = onNavigateToNetwork
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            SectionHeader("Локальная связь (Без интернета)")
            
            ListItem(
                headlineContent = { Text("Режим ближней связи (Mesh)") },
                supportingContent = { Text("Чаты и звонки через Bluetooth / Wi-Fi без сети") },
                leadingContent = { Icon(Icons.Default.WifiTethering, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = uiState.isMeshModeEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleMeshMode(enabled)
                            if (enabled) showMeshDialog = true
                        }
                    )
                }
            )

            SectionHeader("Безопасность: Деньги и Коды")

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

            SectionHeader("Интеллект и Определитель")

            ListItem(
                headlineContent = { Text("Определитель номера Яндекса") },
                supportingContent = { Text("Автоматическое определение спама и сервисов") },
                leadingContent = { Icon(Icons.Default.Verified, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = uiState.yandexCallerIdEnabled,
                        onCheckedChange = { viewModel.toggleYandexCallerId(it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("ИИ для контактов и звонков") },
                supportingContent = { Text("Разделение по ФИО, объединение дубликатов и расшифровка") },
                leadingContent = { Icon(Icons.Default.Psychology, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = uiState.aiContactDeduplication,
                        onCheckedChange = { viewModel.toggleAiDeduplication(it) }
                    )
                }
            )

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
                icon = Icons.Default.Gavel,
                title = "Соответствие ОРИ",
                subtitle = "Приложение работает согласно ФЗ-149",
                onClick = { showComplianceDialog = true }
            )
            
            ProfileMenuItem(
                icon = Icons.Default.DeleteForever,
                title = "Очистить локальный кэш",
                subtitle = "Удалить временные данные и пересинхронизировать",
                onClick = { viewModel.clearCacheAndResync() }
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
            
            Spacer(modifier = Modifier.height(32.dp))
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

        if (showMeshDialog) {
            AlertDialog(
                onDismissRequest = { showMeshDialog = false },
                title = { Text("Локальная автономная сеть") },
                text = {
                    Text("Режим Ближней связи позволяет отправлять сообщения и звонить устройствам поблизости по Bluetooth и Wi-Fi Direct без мобильной вышки и интернета.")
                },
                confirmButton = {
                    TextButton(onClick = { showMeshDialog = false }) { Text("Понятно") }
                }
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
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
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
