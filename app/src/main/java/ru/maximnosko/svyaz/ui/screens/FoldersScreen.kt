package ru.maximnosko.svyaz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.window.DialogProperties
import ru.maximnosko.svyaz.data.local.FolderEntity
import ru.maximnosko.svyaz.viewmodel.MessagesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    viewModel: MessagesViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedFolderForEdit by remember { mutableStateOf<FolderEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Папки чатов", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.CreateNewFolder, null) },
                text = { Text("Создать") },
                shape = MaterialTheme.shapes.medium
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Ваши категории",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(uiState.customFolders) { folder ->
                FolderListItem(
                    folder = folder,
                    onEdit = { selectedFolderForEdit = folder },
                    onDelete = { viewModel.deleteFolder(folder.name) }
                )
            }
            
            item {
                Spacer(Modifier.height(80.dp))
            }
        }

        if (showCreateDialog) {
            CreateFolderDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { name, _ ->
                    viewModel.createFolder(name)
                    showCreateDialog = false
                }
            )
        }

        if (selectedFolderForEdit != null) {
            EditFolderChatsDialog(
                folder = selectedFolderForEdit!!,
                viewModel = viewModel,
                onDismiss = { selectedFolderForEdit = null }
            )
        }
    }
}

@Composable
fun FolderListItem(
    folder: FolderEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isSystemFolder = folder.name in listOf("Все", "Личные", "Служебные", "Непрочитанные")
    
    ElevatedCard(
        onClick = { if (!isSystemFolder) onEdit() },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSystemFolder) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isSystemFolder) MaterialTheme.colorScheme.secondaryContainer 
                        else MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (folder.name) {
                        "Все" -> Icons.Default.AllInbox
                        "Личные" -> Icons.Default.Person
                        "Служебные" -> Icons.Default.Work
                        "Непрочитанные" -> Icons.Default.MarkChatUnread
                        else -> Icons.Default.Folder
                    },
                    contentDescription = null,
                    tint = if (isSystemFolder) MaterialTheme.colorScheme.onSecondaryContainer 
                           else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = folder.name, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isSystemFolder) "Системная категория" else "Пользовательская папка", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (!isSystemFolder) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun CreateFolderDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая папка") },
        text = {
            Column {
                Text("Введите название для новой категории чатов", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Например: Семья") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, "folder") },
                shape = MaterialTheme.shapes.medium
            ) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun EditFolderChatsDialog(
    folder: FolderEntity,
    viewModel: MessagesViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredThreads = remember(uiState.threads, searchQuery) {
        if (searchQuery.isBlank()) uiState.threads
        else uiState.threads.filter { it.contactName.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        title = {
            Column {
                Text("Настройка папки «${folder.name}»")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск чатов...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        },
        text = {
            Box(Modifier.heightIn(min = 200.dp, max = 500.dp)) {
                if (filteredThreads.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Чаты не найдены", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn {
                        items(filteredThreads, key = { it.id }) { thread ->
                            val isInFolder = thread.id in uiState.threadIdsInSelectedFolder
                            ListItem(
                                headlineContent = { Text(thread.contactName, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text(thread.id, style = MaterialTheme.typography.labelSmall) },
                                leadingContent = { 
                                    Checkbox(
                                        checked = isInFolder, 
                                        onCheckedChange = { checked ->
                                            if (checked) viewModel.addThreadToFolder(thread.id, folder.name)
                                            else viewModel.removeThreadFromFolder(thread.id, folder.name)
                                            viewModel.onFolderSelected(folder.name)
                                        }
                                    ) 
                                },
                                modifier = Modifier.clickable {
                                    if (!isInFolder) viewModel.addThreadToFolder(thread.id, folder.name)
                                    else viewModel.removeThreadFromFolder(thread.id, folder.name)
                                    viewModel.onFolderSelected(folder.name)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = MaterialTheme.shapes.medium) { Text("Готово") }
        }
    )
    
    LaunchedEffect(folder) {
        viewModel.onFolderSelected(folder.name)
    }
}
