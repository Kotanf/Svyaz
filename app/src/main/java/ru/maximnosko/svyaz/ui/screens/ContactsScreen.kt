package ru.maximnosko.svyaz.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import ru.maximnosko.svyaz.data.MessageThread

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<MessageThread>,
    isPermissionsGranted: Boolean,
    isLoading: Boolean = false,
    onContactClick: (MessageThread) -> Unit,
    onRequestPermissions: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredContacts = if (searchQuery.isBlank()) contacts else contacts.filter { it.contactName.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = { searchQuery = it },
                            expanded = false,
                            onExpandedChange = {},
                            placeholder = { Text("Поиск контактов") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                AnimatedVisibility(
                                    visible = searchQuery.isNotEmpty(),
                                    enter = fadeIn(tween(150)) + scaleIn(spring(dampingRatio = 0.6f)),
                                    exit = fadeOut(tween(100)) + scaleOut()
                                ) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Очистить")
                                    }
                                }
                            }
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    modifier = Modifier.fillMaxWidth()
                ) {}
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!isPermissionsGranted) {
                PermissionRequiredState(onRequestPermissions)
            } else if (isLoading && contacts.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (contacts.isEmpty()) {
                EmptyContactsState()
            } else if (filteredContacts.isEmpty() && searchQuery.isNotBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Контакт не найден", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    if (searchQuery.isBlank()) {
                        item {
                            Text(
                                text = "Избранное",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Bold
                            )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(contacts.take(5)) { contact ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { onContactClick(contact) }
                                ) {
                                    if (contact.avatarUri != null) {
                                        AsyncImage(
                                            model = contact.avatarUri,
                                            contentDescription = contact.contactName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Surface(
                                            modifier = Modifier.size(56.dp),
                                            shape = CircleShape,
                                            color = contact.avatarColor
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(contact.contactName.take(1), color = Color.White)
                                            }
                                        }
                                    }
                                    Text(
                                        text = contact.contactName.split(" ").first(),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                    }
                }

                items(filteredContacts, key = { it.id }) { contact ->
                        ListItem(
                            modifier = Modifier.animateItem().clickable { onContactClick(contact) },
                            headlineContent = { Text(contact.contactName) },
                            supportingContent = { Text(contact.lastMessage, style = MaterialTheme.typography.labelSmall) },
                            leadingContent = {
                                if (contact.avatarUri != null) {
                                    AsyncImage(
                                        model = contact.avatarUri,
                                        contentDescription = contact.contactName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = CircleShape,
                                        color = contact.avatarColor
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(contact.contactName.take(1), color = Color.White)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyContactsState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Контактов пока нет", color = MaterialTheme.colorScheme.outline)
    }
}
