package ru.maximnosko.svyaz.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onRequestPermissions: () -> Unit = {},
    openChatTarget: String? = null,
    onChatOpened: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val uiState by viewModel.uiState.collectAsState()
    val tabs = remember(uiState.isProModeUnlocked) {
        if (uiState.isProModeUnlocked) {
            listOf(Screen.Chats, Screen.Contacts, Screen.Calls, Screen.Settings)
        } else {
            listOf(Screen.Chats, Screen.Contacts, Screen.Settings)
        }
    }
    val showNavigation = tabs.any { it.route == currentDestination?.route }

    LaunchedEffect(openChatTarget) {
        if (!openChatTarget.isNullOrBlank()) {
            navController.navigate("chat/${Uri.encode(openChatTarget)}")
            onChatOpened()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Chats.route,
            modifier = Modifier.fillMaxSize(),
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
                    onNavigateToServices = { navController.navigate("services") },
                    onNavigateToRadar = { navController.navigate("radar") }
                )
            }
            composable("radar") {
                OfflineRadarScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenChat = { target ->
                        navController.navigate("chat/${Uri.encode(target)}")
                    }
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

        // Truly floating capsule dock over content
        AnimatedVisibility(
            visible = showNavigation,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(animationSpec = tween(200))
        ) {
            FloatingOvalNavigationBar {
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
                        badgeCount = badgeCount,
                        route = screen.route
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (showNavigation) 96.dp else 16.dp)
        )
    }
}

/**
 * Modern Floating Oval Navigation Bar (Dock capsule without background overlay)
 */
@Composable
fun FloatingOvalNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

@Composable
fun RowScope.ExpressiveNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    badgeCount: Int = 0,
    route: String = ""
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )

    // Material Symbols micro-animations per tab:
    // 1. Settings gear rotates 90 degrees
    val gearRotation by animateFloatAsState(
        targetValue = if (selected && route == "settings") 90f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "gearRotation"
    )

    // 2. Calls phone tilt wobble (-14 degrees)
    val callRotation by animateFloatAsState(
        targetValue = if (selected && route == "calls") -14f else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow),
        label = "callRotation"
    )

    // 3. Chats bubble bouncy pop
    val chatScalePop by animateFloatAsState(
        targetValue = if (selected && route == "chats") 1.26f else if (selected) 1.18f else 1.0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
        label = "chatScalePop"
    )

    // 4. Contacts jump
    val contactsJumpY by animateDpAsState(
        targetValue = if (selected && route == "contacts") (-4).dp else if (selected) (-2).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "contactsJumpY"
    )

    val currentRotation = when (route) {
        "settings" -> gearRotation
        "calls" -> callRotation
        else -> 0f
    }

    val currentScale = when (route) {
        "chats" -> chatScalePop
        else -> if (selected) 1.20f else 1.0f
    }

    val currentOffsetY = when (route) {
        "contacts" -> contactsJumpY
        else -> if (selected) (-2).dp else 0.dp
    }

    val pillScaleX by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.35f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pillScaleX"
    )

    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.0f,
        animationSpec = tween(180),
        label = "pillAlpha"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.20f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iconScale"
    )

    val iconOffsetY by animateDpAsState(
        targetValue = if (selected) (-2).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconOffsetY"
    )

    val pillColor = MaterialTheme.colorScheme.secondaryContainer

    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "iconColor"
    )

    val labelColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "labelColor"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .scale(pressScale)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .height(30.dp)
                .width(58.dp),
            contentAlignment = Alignment.Center
        ) {
            // Hardware-accelerated smooth background pill
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pillScaleX
                        scaleY = if (selected) 1.0f else 0.5f
                        alpha = pillAlpha
                    },
                shape = CircleShape,
                color = pillColor
            ) {}

            // Icon with Material Symbols micro-animations (scale, rotation, offset)
            Box(
                modifier = Modifier
                    .scale(currentScale)
                    .rotate(currentRotation)
                    .offset(y = currentOffsetY),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = selected,
                    animationSpec = tween(180),
                    label = "iconCrossfade"
                ) { isSel ->
                    Icon(
                        imageVector = if (isSel) icon else unselectedIcon,
                        contentDescription = label,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (badgeCount > 0) {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp),
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = labelColor,
            maxLines = 1
        )
    }
}
