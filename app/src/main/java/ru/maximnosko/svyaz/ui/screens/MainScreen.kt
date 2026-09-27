package ru.maximnosko.svyaz.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.maximnosko.svyaz.data.MessageThread
import ru.maximnosko.svyaz.viewmodel.MessagesViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Contacts : Screen("contacts", "Контакты", Icons.Filled.People, Icons.Outlined.People)
    object Calls : Screen("calls", "Звонки", Icons.Filled.Call, Icons.Outlined.Call)
    object Chats : Screen("chats", "Чаты", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubble)
    object Settings : Screen("settings", "Настройки", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun MainScreen(
    viewModel: MessagesViewModel,
    snackbarHostState: SnackbarHostState,
    onRequestPermissions: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val tabs = listOf(Screen.Contacts, Screen.Calls, Screen.Chats, Screen.Settings)
    val showNavigation = tabs.any { it.route == currentDestination?.route }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showNavigation) {
                ExpressiveNavigationBar {
                    tabs.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        val badgeCount = if (screen == Screen.Chats) uiState.threads.sumOf { it.unreadCount } else 0

                        ExpressiveNavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = screen.selectedIcon,
                            unselectedIcon = screen.unselectedIcon,
                            label = screen.title,
                            badgeCount = badgeCount
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chats.route,
            modifier = Modifier.padding(if (showNavigation) innerPadding else PaddingValues()),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { (it * 0.12f).toInt() },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(220))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { (-it * 0.12f).toInt() },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = tween(220))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { (-it * 0.12f).toInt() },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(220))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { (it * 0.12f).toInt() },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = tween(220))
            }
        ) {
            composable(Screen.Contacts.route) {
                ContactsScreen(
                    contacts = uiState.contacts,
                    isPermissionsGranted = uiState.isPermissionsGranted,
                    isLoading = uiState.isLoading,
                    onContactClick = { thread ->
                        navController.navigate("chat/${Uri.encode(thread.id)}")
                    },
                    onRequestPermissions = onRequestPermissions
                )
            }
            composable(Screen.Calls.route) {
                CallsScreen(
                    viewModel = viewModel,
                    onCallClick = { id ->
                        navController.navigate("chat/${Uri.encode(id)}")
                    }
                )
            }
            composable(Screen.Chats.route) {
                ChatsScreen(
                    viewModel = viewModel,
                    onThreadClick = { thread ->
                        navController.navigate("chat/${Uri.encode(thread.id)}")
                    },
                    onNewChatClick = { number ->
                        navController.navigate("chat/${Uri.encode(number)}")
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onRequestPermissions = onRequestPermissions
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToFolders = { navController.navigate("folders") },
                    onNavigateToNetwork = { navController.navigate("network") },
                    onNavigateToServices = { navController.navigate("services") }
                )
            }
            composable("folders") {
                FoldersScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("network") {
                NetworkInfoScreen(onBack = { navController.popBackStack() })
            }
            composable("services") {
                SmsServiceCatalogScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenServiceChat = { number ->
                        navController.navigate("chat/${Uri.encode(number)}")
                    }
                )
            }
            composable(
                route = "chat/{threadId}"
            ) { backStackEntry ->
                val rawThreadId = backStackEntry.arguments?.getString("threadId") ?: return@composable
                val threadId = Uri.decode(rawThreadId)
                
                LaunchedEffect(threadId) {
                    viewModel.loadTimeline(threadId)
                }

                val thread = uiState.threads.find { it.id == threadId || it.phoneNumber == threadId } 
                    ?: uiState.contacts.find { it.id == threadId || it.phoneNumber == threadId }
                    ?: MessageThread(
                        id = threadId,
                        contactName = if (threadId.all { it.isDigit() || it == '+' }) threadId else "Чат $threadId",
                        phoneNumber = if (threadId.all { it.isDigit() || it == '+' }) threadId else "",
                        lastMessage = "",
                        lastMessageTime = System.currentTimeMillis()
                    )
                
                ChatDetailScreen(
                    thread = thread,
                    viewModel = viewModel,
                    onBack = { 
                        viewModel.onCloseChat()
                        navController.popBackStack() 
                    }
                )
            }
        }
    }
}

/**
 * Google Material 3 Expressive Animated Navigation Bar
 */
@Composable
fun ExpressiveNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                .height(80.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun RowScope.ExpressiveNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    badgeCount: Int = 0
) {
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 64.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pillWidth"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.22f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iconScale"
    )

    val pillColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = tween(220),
        label = "pillColor"
    )

    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "iconColor"
    )

    val labelColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "labelColor"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(64.dp),
            contentAlignment = Alignment.Center
        ) {
            // Active Indicator Pill Expansion Animation
            Surface(
                modifier = Modifier
                    .height(32.dp)
                    .width(pillWidth),
                shape = CircleShape,
                color = pillColor
            ) {}

            // Bouncy Icon Scale with Badge
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (selected) icon else unselectedIcon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.scale(iconScale)
                )

                if (badgeCount > 0) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = labelColor
        )
    }
}
