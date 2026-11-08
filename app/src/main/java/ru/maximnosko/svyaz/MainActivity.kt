package ru.maximnosko.svyaz

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.maximnosko.svyaz.ui.screens.MainScreen
import ru.maximnosko.svyaz.ui.theme.MessagesTheme
import ru.maximnosko.svyaz.viewmodel.MessagesViewModel
import ru.maximnosko.svyaz.viewmodel.UiEvent
import ru.rustore.sdk.pushclient.*
import ru.rustore.sdk.core.feature.model.FeatureAvailabilityResult
import ru.rustore.sdk.appupdate.manager.factory.RuStoreAppUpdateManagerFactory
import ru.rustore.sdk.appupdate.model.UpdateAvailability

private object RuStorePushLogger : ru.rustore.sdk.pushclient.common.logger.Logger {
    private const val TAG = "RuStorePush"

    override fun createLogger(tag: String): ru.rustore.sdk.pushclient.common.logger.Logger = this

    override fun debug(message: String, throwable: Throwable?) {
        if (BuildConfig.DEBUG) Log.d(TAG, message, throwable)
    }

    override fun info(message: String, throwable: Throwable?) {
        Log.i(TAG, message, throwable)
    }

    override fun verbose(message: String, throwable: Throwable?) {
        if (BuildConfig.DEBUG) Log.v(TAG, message, throwable)
    }

    override fun warn(message: String, throwable: Throwable?) {
        Log.w(TAG, message, throwable)
    }

    override fun error(message: String, throwable: Throwable?) {
        if (message.contains("pub_key") || message.contains("doesn't exist") || message.contains("code=400")) {
            Log.w(TAG, "RuStore Push token registration skipped: app signing SHA-256 fingerprint is not registered in RuStore Console ($message)")
        } else {
            Log.w(TAG, "RuStore Push SDK error handled: $message", throwable)
        }
    }
}

class MainActivity : ComponentActivity() {

    private val openChatTarget = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)

        // 120Hz High Refresh Rate Optimization (Android 11+ & 6.0+)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                val supportedModes = display?.supportedModes ?: emptyArray()
                val highHzMode = supportedModes
                    .filter { it.refreshRate >= 119f }
                    .maxByOrNull { it.refreshRate }
                    ?: supportedModes.maxByOrNull { it.refreshRate }
                if (highHzMode != null) {
                    val params = window.attributes
                    params.preferredDisplayModeId = highHzMode.modeId
                    params.preferredRefreshRate = highHzMode.refreshRate
                    window.attributes = params
                    Log.i("MainActivity", "120Hz high refresh rate mode activated: ${highHzMode.refreshRate} Hz")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val params = window.attributes
                params.preferredRefreshRate = 120f
                window.attributes = params
            }
        } catch (e: Exception) {
            Log.d("MainActivity", "120Hz config skipped: ${e.message}")
        }

        // Initialize RuStore Push SDK & App Update Check
        try {
            val projectId = BuildConfig.RUSTORE_PROJECT_ID
            val isDebug = BuildConfig.DEBUG

            RuStorePushClient.init(
                application = application,
                projectId = projectId,
                logger = RuStorePushLogger,
                testModeEnabled = isDebug
            )

            RuStorePushClient.checkPushAvailability()
                .addOnSuccessListener { result ->
                    if (result is FeatureAvailabilityResult.Available) {
                        if (isDebug) Log.i("MainActivity", "RuStore Push is available")
                        RuStorePushClient.getToken()
                            .addOnSuccessListener { token ->
                                if (isDebug) Log.d("MainActivity", "RuStore push token acquired: $token")
                                try {
                                    getSharedPreferences("svyaz_app_prefs", MODE_PRIVATE)
                                        .edit { putString("rustore_push_token", token) }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to save push token", e)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w("MainActivity", "RuStore push token request skipped or failed: ${e.message}")
                            }
                    } else {
                        Log.w("MainActivity", "RuStore Push unavailable on device: $result")
                    }
                }
                .addOnFailureListener { throwable ->
                    Log.w("MainActivity", "RuStore Push availability check failed: ${throwable.message}")
                }

            val appUpdateManager = RuStoreAppUpdateManagerFactory.create(application)
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability == UpdateAvailability.UPDATE_AVAILABLE) {
                    Log.i("MainActivity", "RuStore update available for Svyaz app")
                }
            }.addOnFailureListener { throwable ->
                Log.d("MainActivity", "RuStore update check completed or unsupported on device: ${throwable.message}")
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "RuStore Push / Update init handled safely", e)
        }

        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val viewModel: MessagesViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            MessagesTheme(
                themeMode = uiState.themeMode,
                themePreset = uiState.themePreset,
                fontScale = uiState.fontScale,
                dynamicColor = uiState.isDynamicColorEnabled
            ) {
                AppRoot(
                    viewModel = viewModel,
                    openChatTarget = openChatTarget.value,
                    onChatOpened = { openChatTarget.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val target = intent.getStringExtra("open_chat_address") 
            ?: intent.getStringExtra("open_chat_thread_id")
            ?: intent.getStringExtra("phoneNumber")
            ?: intent.data?.schemeSpecificPart
        if (!target.isNullOrBlank()) {
            openChatTarget.value = target
        }
    }

    override fun unbindService(conn: ServiceConnection) {
        try {
            super.unbindService(conn)
        } catch (e: IllegalArgumentException) {
            Log.w("MainActivity", "Safely caught unbindService error (Service not registered): ${e.message}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Unexpected error in unbindService: ${e.message}", e)
        }
    }
}

@Composable
fun AppRoot(
    viewModel: MessagesViewModel = viewModel(),
    openChatTarget: String? = null,
    onChatOpened: () -> Unit = {}
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.screenshotBlockEnabled) {
        val activity = context as? Activity
        activity?.window?.let { window ->
            if (uiState.screenshotBlockEnabled) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    // When the SMS role dialog closes, the callback may need to chain the DIALER
    // role request. We queue the next intent here instead of referencing the
    // launcher inside its own initializer (which Kotlin can't resolve).
    val pendingRoleIntent = remember { androidx.compose.runtime.mutableStateOf<Intent?>(null) }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        viewModel.syncSystemData()
        pendingRoleIntent.value = run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                } else null
            } else null
        }
    }

    // Launch any queued follow-up role request (e.g. DIALER after SMS) once the
    // previous dialog has been dismissed.
    LaunchedEffect(pendingRoleIntent.value) {
        pendingRoleIntent.value?.let { intent ->
            pendingRoleIntent.value = null
            roleLauncher.launch(intent)
        }
    }

    // Requests the SMS role first; the DIALER role is chained via the launcher
    // callback once the SMS dialog is dismissed.
    val requestRoles = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            when {
                !roleManager.isRoleHeld(RoleManager.ROLE_SMS) ->
                    roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS))
                !roleManager.isRoleHeld(RoleManager.ROLE_DIALER) ->
                    roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            }
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val essentialPermissions = listOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG
        )
        val essentialGranted = essentialPermissions.all { 
            results[it] == true || ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
        }
        
        if (essentialGranted) {
            viewModel.onPermissionsGranted()
        }
        // Only now, after the permission dialogs are dismissed, request roles
        // so the two system flows don't overlap.
        requestRoles()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val essentialPermissions = listOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG
        )

        val essentialGranted = essentialPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (essentialGranted) {
            viewModel.onPermissionsGranted()
            requestRoles()
        } else {
            permissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    LaunchedEffect(viewModel.uiEvents) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowSnackBar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.CopyToClipboard -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText("Связь", event.text).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            description.extras = PersistableBundle().apply {
                                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                            }
                        }
                    }
                    clipboard.setPrimaryClip(clipData)
                }
                is UiEvent.MakeCall -> {
                    val intent = Intent(Intent.ACTION_DIAL, "tel:${event.phoneNumber}".toUri())
                    context.startActivity(intent)
                }
                is UiEvent.NotifySendError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    MainScreen(
        viewModel = viewModel, 
        snackbarHostState = snackbarHostState,
        openChatTarget = openChatTarget,
        onChatOpened = onChatOpened,
        onRequestPermissions = {
            val permissions = mutableListOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionsLauncher.launch(permissions.toTypedArray())
        }
    )
}
