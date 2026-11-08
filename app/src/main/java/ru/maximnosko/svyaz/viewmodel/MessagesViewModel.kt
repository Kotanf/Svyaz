package ru.maximnosko.svyaz.viewmodel

import android.app.Application
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.app.NotificationManager
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import ru.maximnosko.svyaz.SmsDeliveryReceiver
import ru.maximnosko.svyaz.data.*
import ru.maximnosko.svyaz.data.local.*
import ru.maximnosko.svyaz.data.sync.SmsSyncWorker
import ru.maximnosko.svyaz.ui.theme.AppFontScale
import ru.maximnosko.svyaz.ui.theme.AppThemePreset
import ru.maximnosko.svyaz.ui.theme.ThemeMode
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class MessagesUiState(
    val threads: List<MessageThread> = emptyList(),
    val contacts: List<MessageThread> = emptyList(),
    val callLogs: List<CallLogItem> = emptyList(),
    val timelineByThread: Map<String, List<TimelineEvent>> = emptyMap(),
    val activeThreadId: String? = null,
    val selectedNotificationFilter: CategoryFilter = CategoryFilter.ALL,
    val otpCode: OtpCodeData? = null,
    val isUndoSendVisible: Boolean = false,
    val undoProgress: Float = 0f,
    val pendingMessage: TimelineEvent.Message? = null,
    val isPermissionsGranted: Boolean = false,
    val isDefaultSmsApp: Boolean = true,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val searchQuery: String = "",
    val selectedFolder: String = "Все",
    val customFolders: List<FolderEntity> = emptyList(),
    val threadIdsInSelectedFolder: Set<String> = emptySet(),
    val undoSendPeriodMs: Long = 3000L,
    val deliveryReportsEnabled: Boolean = true,
    val userName: String = "Укажите имя",
    val userPhone: String = "+7 (900) 123-45-67",
    val isMeshModeEnabled: Boolean = false,
    val hideFinanceInNotifications: Boolean = true,
    val hideCodesInNotifications: Boolean = true,
    val biometricAuthEnabled: Boolean = true,
    val screenshotBlockEnabled: Boolean = true,
    val yandexCallerIdEnabled: Boolean = true,
    val aiContactDeduplication: Boolean = true,
    val selectedSimSlot: Int = 1,
    val userAvatarUri: String? = null,
    val isPrivacyShieldEnabled: Boolean = false,
    val isDynamicColorEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themePreset: AppThemePreset = AppThemePreset.COSMIC,
    val fontScale: AppFontScale = AppFontScale.NORMAL,
    val isProModeUnlocked: Boolean = false
) {
    val allFolderNames: List<String>
        get() = listOf("Все", "Архив", "Корзина", "Личные", "Служебные", "Непрочитанные") + 
                customFolders.map { it.name }.filter { it !in listOf("Все", "Архив", "Корзина", "Личные", "Служебные", "Непрочитанные") }
}

sealed class UiEvent {
    data class ShowSnackBar(val message: String) : UiEvent()
    data class CopyToClipboard(val text: String) : UiEvent()
    data class MakeCall(val phoneNumber: String) : UiEvent()
    data class NotifySendError(val message: String) : UiEvent()
}

/**
 * MessagesViewModel is the core of the app's business logic.
 * It manages state for threads, contacts, calls, and the active timeline.
 * Robust sync logic implemented to prevent UI "vanishing" bugs.
 */
class MessagesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MessagingRepository(application)
    private val systemDataProvider = SystemDataProvider(application)
    private val database = SvyazDatabase.getDatabase(application)
    private val threadDao = database.threadDao()
    private val prefs = application.getSharedPreferences("svyaz_app_prefs", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(
        MessagesUiState(
            userName = prefs.getString("user_name", "Укажите имя") ?: "Укажите имя",
            userPhone = prefs.getString("user_phone", "+7 (900) 123-45-67") ?: "+7 (900) 123-45-67",
            userAvatarUri = prefs.getString("user_avatar_uri", null),
            undoSendPeriodMs = prefs.getLong("undo_period_ms", 3000L),
            deliveryReportsEnabled = prefs.getBoolean("delivery_reports", true),
            isMeshModeEnabled = prefs.getBoolean("mesh_mode", false),
            hideFinanceInNotifications = prefs.getBoolean("hide_finance", true),
            hideCodesInNotifications = prefs.getBoolean("hide_codes", true),
            biometricAuthEnabled = prefs.getBoolean("biometric_auth", true),
            screenshotBlockEnabled = prefs.getBoolean("screenshot_block", true),
            yandexCallerIdEnabled = prefs.getBoolean("yandex_caller_id", true),
            aiContactDeduplication = prefs.getBoolean("ai_deduplication", true),
            isPrivacyShieldEnabled = prefs.getBoolean("privacy_shield", false),
            isDynamicColorEnabled = prefs.getBoolean("dynamic_color", false),
            themeMode = ThemeMode.fromId(prefs.getString("theme_mode", ThemeMode.SYSTEM.id)),
            themePreset = AppThemePreset.fromId(prefs.getString("theme_preset", AppThemePreset.COSMIC.id)),
            fontScale = AppFontScale.fromId(prefs.getString("font_scale", AppFontScale.NORMAL.id)),
            isProModeUnlocked = prefs.getBoolean("pro_mode_unlocked", false)
        )
    )
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    // Single source of truth for the thread list
    val filteredThreads: StateFlow<List<MessageThread>> = combine(
        _uiState.map { it.threads },
        _uiState.map { it.selectedFolder },
        _uiState.map { it.searchQuery },
        _uiState.map { it.threadIdsInSelectedFolder }
    ) { threads, selectedFolder, searchQuery, threadIdsInSelectedFolder ->
        val activeThreads = threads.filter { it.folder != "Корзина" && it.folder != "Архив" }
        val baseList = when (selectedFolder) {
            "Личные" -> activeThreads.filter { !it.isMachine }
            "Служебные" -> activeThreads.filter { it.isMachine }
            "Непрочитанные" -> activeThreads.filter { it.unreadCount > 0 }
            "Все" -> activeThreads
            "Архив" -> threads.filter { it.folder == "Архив" }
            "Корзина" -> threads.filter { it.folder == "Корзина" }
            else -> activeThreads.filter { it.id in threadIdsInSelectedFolder || it.folder == selectedFolder }
        }
        val filtered = if (searchQuery.isBlank()) baseList else baseList.filter { 
            it.contactName.contains(searchQuery, ignoreCase = true) || 
            it.lastMessage.contains(searchQuery, ignoreCase = true)
        }
        filtered.sortedWith(compareByDescending<MessageThread> { it.isPinned }.thenByDescending { it.lastMessageTime })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiEvents = Channel<UiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    private var undoJob: Job? = null
    private var folderJob: Job? = null
    private var syncDebounceJob: Job? = null

    @Volatile
    private var observersRegistered = false

    @Volatile
    private var lastFullSyncTime = 0L

    private val isPerformingSync = AtomicBoolean(false)

    @Volatile
    private var needIncrementalSync = AtomicBoolean(false)

    private val smsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            // Mark that we need sync regardless of selfChange - the sync itself will
            // handle ignoring its own changes. This prevents missing updates.
            if (isPerformingSync.get()) {
                needIncrementalSync.set(true)
                return
            }
            scheduleIncrementalSync()
        }
    }

    /**
     * Schedules an incremental sync (threads only) with proper batching.
     * If a sync is already scheduled, we don't cancel it - just wait for it
     * and batch all changes that arrive during the delay window.
     */
    private fun scheduleIncrementalSync() {
        if (syncDebounceJob?.isActive == true) return

        syncDebounceJob = viewModelScope.launch {
            delay(5.seconds)
            if (needIncrementalSync.compareAndSet(true, false)) {
                performIncrementalSync()
            }
        }
    }

    /**
     * Lightweight sync - only fetches threads from system provider.
     * This is safe to call frequently because it only touches
     * the threads table and doesn't re-fetch contacts or call logs.
     */
    private suspend fun performIncrementalSync() {
        if (!isPerformingSync.compareAndSet(false, true)) {
            // Another sync is already running, mark to retry
            needIncrementalSync.set(true)
            return
        }
        lastFullSyncTime = System.currentTimeMillis()
        
        try {
            Log.d("MessagesViewModel", "Incremental sync started")
            val threads = systemDataProvider.fetchThreads()
            
            if (threads.isNotEmpty()) {
                threadDao.insertThreads(threads.map { it.toEntity() })
                _uiState.update { state ->
                    state.copy(
                        threads = if (threads.isEmpty()) state.threads else threads,
                        isSyncing = false
                    )
                }

                // Refresh timeline if viewing a thread
                _uiState.value.activeThreadId?.let { refreshTimelineData(it) }
            }

            // Check if we should do a full sync (contacts + call logs) - every 2 minutes
            val now = System.currentTimeMillis()
            if (now - lastFullSyncTime > 120_000L) {
                lastFullSyncTime = now
                viewModelScope.launch {
                    syncSystemData() // Full sync
                }
            }
        } catch (e: Exception) {
            Log.e("MessagesViewModel", "Incremental sync failed", e)
        } finally {
            _uiState.update { it.copy(isSyncing = false) }
            isPerformingSync.set(false)
        }
    }

    init {
        // Observe DB threads
        threadDao.getAllThreads()
            .map { entities -> entities.map { it.toDomain() } }
            .onEach { cachedThreads ->
                // ONLY update if we actually have data, to avoid split-second empty screen
                if (cachedThreads.isNotEmpty() || _uiState.value.threads.isEmpty()) {
                    Log.d("MessagesViewModel", "Updating threads from DB: ${cachedThreads.size}")
                    _uiState.update { it.copy(threads = cachedThreads) }
                }
            }
            .catch { e -> Log.e("MessagesViewModel", "DB Observation error", e) }
            .launchIn(viewModelScope)

        threadDao.getAllFolders()
            .onEach { folders -> _uiState.update { it.copy(customFolders = folders) } }
            .launchIn(viewModelScope)
            
        viewModelScope.launch {
            // Background setup
            listOf("Все", "Личные", "Служебные", "Непрочитанные").forEach { name ->
                threadDao.insertFolder(FolderEntity(name))
            }
            
            registerSmsObservers()
            
            syncSystemData()
        }

        viewModelScope.launch {
            SmsDeliveryReceiver.deliveryUpdates.collect { update ->
                _uiState.update { state ->
                    val updatedMap = state.timelineByThread.toMutableMap()
                    for ((key, list) in updatedMap) {
                        val idx = list.indexOfFirst { it.id == update.messageId }
                        if (idx != -1) {
                            val msg = list[idx] as? TimelineEvent.Message ?: continue
                            val updatedList = list.toMutableList()
                            updatedList[idx] = msg.copy(status = update.status, deliveryTime = System.currentTimeMillis())
                            updatedMap[key] = updatedList
                        }
                    }
                    state.copy(timelineByThread = updatedMap)
                }
            }
        }
    }

    private fun registerSmsObservers() {
        if (observersRegistered) return
        try {
            val cr = getApplication<Application>().contentResolver
            cr.registerContentObserver(Telephony.Sms.CONTENT_URI, true, smsObserver)
            cr.registerContentObserver(Telephony.Mms.CONTENT_URI, true, smsObserver)
            observersRegistered = true
        } catch (e: Exception) {
            Log.w("MessagesViewModel", "Could not register ContentObserver (permissions may not be granted yet): ${e.message}")
        }
    }

    override fun onCleared() {
        try {
            getApplication<Application>().contentResolver.unregisterContentObserver(smsObserver)
            observersRegistered = false
        } catch (e: Exception) {
            Log.w("MessagesViewModel", "Failed to unregister ContentObserver: ${e.message}")
        }
    }

    fun onPermissionsGranted() {
        _uiState.update { it.copy(isPermissionsGranted = true) }
        registerSmsObservers()
        syncSystemData()
    }

    fun syncSystemData() {
        if (!isPerformingSync.compareAndSet(false, true)) return
        lastFullSyncTime = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("MessagesViewModel", "Sync started...")
                _uiState.update { it.copy(isSyncing = true) }
                
                val threads = systemDataProvider.fetchThreads()
                val contacts = systemDataProvider.fetchContacts()
                val callLogs = systemDataProvider.fetchCallLogs()
                
                Log.d("MessagesViewModel", "Sync fetched ${threads.size} threads")
                
                // CRITICAL: Update DB first
                threadDao.insertThreads(threads.map { it.toEntity() })
                
                val now = System.currentTimeMillis()
                val thirtyMinutes = 30 * 60 * 1000L
                val latestOtp = threads.filter {
                    it.category == CategoryFilter.CODES && (now - it.lastMessageTime) < thirtyMinutes
                }.maxByOrNull { it.lastMessageTime }?.let { thread ->
                    detectOtpCode(thread.lastMessage, thread.contactName)
                }
                
                _uiState.update { state ->
                    state.copy(
                        threads = threads.ifEmpty { state.threads },
                        contacts = contacts, 
                        callLogs = callLogs, 
                        isSyncing = false,
                        otpCode = latestOtp
                    )
                }

                cleanupExpiredTrash()

                _uiState.value.activeThreadId?.let { refreshTimelineData(it) }
            } catch (e: Exception) {
                Log.e("MessagesViewModel", "Sync failed", e)
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
                isPerformingSync.set(false)
            }
        }
    }

    fun clearCacheAndResync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            syncSystemData()
            _uiState.update { it.copy(isLoading = false) }
            _uiEvents.send(UiEvent.ShowSnackBar("Синхронизация завершена"))
        }
    }

    private fun detectOtpCode(text: String, sender: String): OtpCodeData? {
        val lower = text.lowercase()
        val otpKeywords = listOf("код", "code", "pass", "пароль", "otp", "аутентификация", "подтверждение", "подтвержд")
        if (!otpKeywords.any { lower.contains(it) }) return null

        val regex = Regex("(\\d{3}-\\d{3}|\\d{4,6})")
        val match = regex.find(text) ?: return null

        val realMinutes = parseExpirationMinutesFromText(text)

        return OtpCodeData(
            code = match.value,
            sender = sender,
            expireTimeMinutes = realMinutes
        )
    }

    private fun parseExpirationMinutesFromText(text: String): Int? {
        val lower = text.lowercase()
        val regex = Regex("""(?:действител|действу|истека|срок|valid|expire|течен).{0,25}?(\d{1,3})\s*(?:мин|минут|min)""", RegexOption.IGNORE_CASE)
        val match = regex.find(lower)
        if (match != null) {
            val min = match.groupValues[1].toIntOrNull()
            if (min != null && min in 1..120) {
                return min
            }
        }
        val altRegex = Regex("""(\d{1,3})\s*(?:мин|минут|min)""", RegexOption.IGNORE_CASE)
        val altMatch = altRegex.find(lower)
        if (altMatch != null) {
            val min = altMatch.groupValues[1].toIntOrNull()
            if (min != null && min in 1..60) {
                return min
            }
        }
        return null
    }

    private suspend fun refreshTimelineData(idOrAddress: String) {
        try {
            val (normalizedThreadId, systemEvents) = systemDataProvider.fetchTimeline(idOrAddress)
            val localEvents = threadDao.getMessagesForThread(normalizedThreadId).map { it.toDomain() }
            val combined = (systemEvents + localEvents).distinctBy { it.id }.sortedBy { it.timestamp }
            
            val thread = _uiState.value.threads.find { it.id == idOrAddress || it.id == normalizedThreadId || it.phoneNumber == idOrAddress }
                ?: _uiState.value.contacts.find { it.id == idOrAddress || it.phoneNumber == idOrAddress }
            val resolvedPhone = thread?.phoneNumber?.takeIf { it.isNotBlank() } ?: systemDataProvider.resolveAddress(idOrAddress) ?: systemDataProvider.resolveAddress(normalizedThreadId)

            updateTimelineInState(normalizedThreadId, idOrAddress, resolvedPhone, thread, combined)
        } catch (e: Exception) {
            Log.e("MessagesViewModel", "Timeline refresh error", e)
        }
    }

    private fun updateTimelineInState(
        normalizedThreadId: String,
        idOrAddress: String,
        resolvedPhone: String?,
        thread: MessageThread?,
        combined: List<TimelineEvent>
    ) {
        _uiState.update { state ->
            val updatedMap = state.timelineByThread.toMutableMap()
            updatedMap[normalizedThreadId] = combined
            updatedMap[idOrAddress] = combined
            if (!resolvedPhone.isNullOrBlank()) {
                updatedMap[resolvedPhone] = combined
            }
            if (thread != null) {
                updatedMap[thread.id] = combined
                if (thread.phoneNumber.isNotBlank()) {
                    updatedMap[thread.phoneNumber] = combined
                }
            }
            val updatedThreads = state.threads.map { t ->
                if (t.id == idOrAddress || t.id == normalizedThreadId || (resolvedPhone != null && t.phoneNumber == resolvedPhone)) {
                    t.copy(unreadCount = 0)
                } else t
            }
            state.copy(timelineByThread = updatedMap, threads = updatedThreads)
        }
    }

    fun loadTimeline(idOrAddress: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (normalizedThreadId, systemEvents) = systemDataProvider.fetchTimeline(idOrAddress)
                _uiState.update { it.copy(activeThreadId = normalizedThreadId) }

                val localEvents = threadDao.getMessagesForThread(normalizedThreadId).map { it.toDomain() }
                val combined = (systemEvents + localEvents).distinctBy { it.id }.sortedBy { it.timestamp }
                
                val thread = _uiState.value.threads.find { it.id == idOrAddress || it.id == normalizedThreadId || it.phoneNumber == idOrAddress }
                    ?: _uiState.value.contacts.find { it.id == idOrAddress || it.phoneNumber == idOrAddress }
                val resolvedPhone = thread?.phoneNumber?.takeIf { it.isNotBlank() } ?: systemDataProvider.resolveAddress(idOrAddress) ?: systemDataProvider.resolveAddress(normalizedThreadId)

                if (thread == null || thread.unreadCount > 0) {
                    systemDataProvider.markMessagesAsRead(normalizedThreadId, resolvedPhone)
                }

                // Dismiss notifications for this sender
                try {
                    val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(kotlin.math.abs(idOrAddress.hashCode()))
                    if (!resolvedPhone.isNullOrBlank()) {
                        notificationManager.cancel(kotlin.math.abs(resolvedPhone.hashCode()))
                    }
                    if (normalizedThreadId.isNotBlank()) {
                        notificationManager.cancel(kotlin.math.abs(normalizedThreadId.hashCode()))
                    }
                } catch (e: Exception) {
                    Log.e("MessagesViewModel", "Error canceling notification", e)
                }

                updateTimelineInState(normalizedThreadId, idOrAddress, resolvedPhone, thread, combined)
            } catch (e: Exception) {
                Log.e("MessagesViewModel", "Timeline load error", e)
            }
        }
    }

    fun onCloseChat() {
        _uiState.update { it.copy(activeThreadId = null, timelineByThread = emptyMap()) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFolderSelected(folderName: String) {
        _uiState.update { it.copy(selectedFolder = folderName) }
        // Cancel any previous folder collector so they don't accumulate on every switch.
        folderJob?.cancel()
        if (folderName !in listOf("Все", "Личные", "Служебные", "Непрочитанные")) {
            folderJob = viewModelScope.launch {
                threadDao.getThreadIdsInFolder(folderName).collect { ids ->
                    _uiState.update { it.copy(threadIdsInSelectedFolder = ids.toSet()) }
                }
            }
        } else {
            _uiState.update { it.copy(threadIdsInSelectedFolder = emptySet()) }
        }
    }

    fun onPinThread(thread: MessageThread) {
        viewModelScope.launch { threadDao.insertThreads(listOf(thread.copy(isPinned = !thread.isPinned).toEntity())) }
    }

    fun onArchiveThread(thread: MessageThread) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = thread.copy(folder = "Архив", isPinned = false)
            threadDao.insertThreads(listOf(updated.toEntity()))
            _uiState.update { state ->
                state.copy(threads = state.threads.map { if (it.id == thread.id) updated else it })
            }
            _uiEvents.send(UiEvent.ShowSnackBar("Чат «${thread.contactName}» перемещён в Архив"))
        }
    }

    fun onDeleteThread(thread: MessageThread) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = thread.copy(folder = "Корзина", isPinned = false)
            threadDao.insertThreads(listOf(updated.toEntity()))
            _uiState.update { state ->
                state.copy(threads = state.threads.map { if (it.id == thread.id) updated else it })
            }
            _uiEvents.send(UiEvent.ShowSnackBar("Чат «${thread.contactName}» перемещён в Корзину (хранится 30 дней)"))
        }
    }

    fun onRestoreThread(thread: MessageThread) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = thread.copy(folder = null)
            threadDao.insertThreads(listOf(updated.toEntity()))
            _uiState.update { state ->
                state.copy(threads = state.threads.map { if (it.id == thread.id) updated else it })
            }
            _uiEvents.send(UiEvent.ShowSnackBar("Чат «${thread.contactName}» восстановлен"))
        }
    }

    fun onPermanentDeleteThread(thread: MessageThread) {
        viewModelScope.launch(Dispatchers.IO) {
            threadDao.deleteThread(thread.id)
            threadDao.deleteMessagesForThread(thread.id)
            _uiState.update { state ->
                state.copy(threads = state.threads.filter { it.id != thread.id })
            }
            _uiEvents.send(UiEvent.ShowSnackBar("Чат «${thread.contactName}» окончательно удалён"))
        }
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            val trashList = threadDao.getThreadsInFolderList("Корзина")
            for (t in trashList) {
                threadDao.deleteThread(t.id)
                threadDao.deleteMessagesForThread(t.id)
            }
            threadDao.emptyTrashThreads()
            _uiState.update { state ->
                state.copy(threads = state.threads.filter { it.folder != "Корзина" })
            }
            _uiEvents.send(UiEvent.ShowSnackBar("Корзина полностью очищена"))
        }
    }

    private suspend fun cleanupExpiredTrash() {
        try {
            val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            val trashList = threadDao.getThreadsInFolderList("Корзина")
            for (t in trashList) {
                if ((now - t.lastMessageTime) > thirtyDaysMs) {
                    threadDao.deleteThread(t.id)
                    threadDao.deleteMessagesForThread(t.id)
                }
            }
        } catch (e: Exception) {
            Log.e("MessagesViewModel", "Error cleaning up trash", e)
        }
    }

    fun sendMessage(threadId: String, content: String, isMms: Boolean = false, mediaUri: String? = null, mediaType: MediaType? = null, requestDeliveryReport: Boolean = _uiState.value.deliveryReportsEnabled) {
        val thread = _uiState.value.threads.find { it.id == threadId } ?: _uiState.value.contacts.find { it.id == threadId }
        val address = thread?.phoneNumber?.takeIf { it.isNotBlank() } 
            ?: if (!threadId.all { it.isDigit() } || threadId.length >= 10) threadId else systemDataProvider.resolveAddress(threadId)
        
        val finalAddress = address ?: threadId

        if (finalAddress.isBlank()) {
            viewModelScope.launch { _uiEvents.send(UiEvent.ShowSnackBar("Ошибка: Некорректный номер")) }
            return
        }

        // If there was a pending message in undo queue, flush and send it immediately
        val previousPending = _uiState.value.pendingMessage
        if (previousPending != null) {
            undoJob?.cancel()
            viewModelScope.launch {
                try {
                    val sentMessage = repository.sendMessage(
                        address = finalAddress,
                        content = previousPending.content,
                        isMms = previousPending.isMms,
                        mediaUri = previousPending.mediaUri,
                        mediaType = previousPending.mediaType,
                        subId = _uiState.value.selectedSimSlot,
                        requestDeliveryReport = requestDeliveryReport,
                        messageId = previousPending.id,
                        threadId = threadId
                    )
                    confirmSend(threadId, sentMessage)
                } catch (e: Exception) {
                    markSendError(threadId, previousPending)
                }
            }
        }

        val newMessage = TimelineEvent.Message(content = content, timestamp = System.currentTimeMillis(), isFromMe = true, isPending = true, status = MessageStatus.SENDING, isMms = isMms, mediaUri = mediaUri, mediaType = mediaType)
        _uiState.update { it.copy(pendingMessage = newMessage, isUndoSendVisible = true, undoProgress = 1f) }
        
        undoJob = viewModelScope.launch {
            delay(_uiState.value.undoSendPeriodMs.milliseconds)
            try {
                val sentMessage = repository.sendMessage(
                    address = finalAddress, 
                    content = content, 
                    isMms = isMms, 
                    mediaUri = mediaUri, 
                    mediaType = mediaType,
                    subId = _uiState.value.selectedSimSlot,
                    requestDeliveryReport = requestDeliveryReport,
                    messageId = newMessage.id,
                    threadId = threadId
                )
                confirmSend(threadId, sentMessage)
                syncSystemData()
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.NotifySendError("Ошибка: ${e.message}"))
                markSendError(threadId, newMessage)
            }
        }
    }

    fun toggleSimSlot(slot: Int) {
        _uiState.update { it.copy(selectedSimSlot = slot) }
    }

    private fun confirmSend(threadId: String, finalMessage: TimelineEvent.Message) {
        val thread = _uiState.value.threads.find { it.id == threadId } ?: _uiState.value.contacts.find { it.id == threadId }
        val phone = thread?.phoneNumber?.takeIf { it.isNotBlank() }

        _uiState.update { state ->
            val updatedMap = state.timelineByThread.toMutableMap()
            val currentList = state.timelineByThread[threadId] 
                ?: (if (phone != null) state.timelineByThread[phone] else null) 
                ?: emptyList()
            val filtered = currentList.filter { it.id != finalMessage.id }
            val newList = filtered + finalMessage
            updatedMap[threadId] = newList
            if (phone != null) {
                updatedMap[phone] = newList
            }
            state.copy(timelineByThread = updatedMap, pendingMessage = null, isUndoSendVisible = false, undoProgress = 0f)
        }
    }

    private fun markSendError(threadId: String, failedMessage: TimelineEvent.Message) {
        _uiState.update { state ->
            val updatedMap = state.timelineByThread.toMutableMap()
            updatedMap[threadId] = (state.timelineByThread[threadId] ?: emptyList()) + failedMessage.copy(status = MessageStatus.ERROR, isPending = false)
            state.copy(timelineByThread = updatedMap, pendingMessage = null, isUndoSendVisible = false, undoProgress = 0f)
        }
    }

    fun cancelSend() {
        undoJob?.cancel()
        _uiState.update { it.copy(pendingMessage = null, isUndoSendVisible = false, undoProgress = 0f) }
    }

    fun onDeleteMessage(threadId: String, messageId: String) {
        _uiState.update { state ->
            val updatedMap = state.timelineByThread.toMutableMap()
            updatedMap[threadId] = (state.timelineByThread[threadId] ?: emptyList()).filter { it.id != messageId }
            state.copy(timelineByThread = updatedMap)
        }
    }

    fun onEditMessage(threadId: String, messageId: String, newContent: String) {
        _uiState.update { state ->
            val updatedMap = state.timelineByThread.toMutableMap()
            updatedMap[threadId] = (state.timelineByThread[threadId] ?: emptyList()).map { 
                if (it is TimelineEvent.Message && it.id == messageId) it.copy(content = newContent, isEdited = true) else it
            }
            state.copy(timelineByThread = updatedMap)
        }
    }

    fun onToggleReaction(threadId: String, messageId: String, emoji: String) {
        _uiState.update { state ->
            val updatedMap = state.timelineByThread.toMutableMap()
            updatedMap[threadId] = (state.timelineByThread[threadId] ?: emptyList()).map { 
                if (it is TimelineEvent.Message && it.id == messageId) {
                    val reactions = it.reactions.toMutableMap()
                    if (reactions.containsKey(emoji)) reactions.remove(emoji) else reactions[emoji] = 1
                    it.copy(reactions = reactions)
                } else it
            }
            state.copy(timelineByThread = updatedMap)
        }
    }

    fun makeCall(phoneNumber: String) {
        viewModelScope.launch { _uiEvents.send(UiEvent.MakeCall(phoneNumber)) }
    }

    // Settings / Folder functions required for compilation & persistence
    fun createFolder(name: String) { viewModelScope.launch { threadDao.insertFolder(FolderEntity(name)) } }
    fun deleteFolder(name: String) { viewModelScope.launch { threadDao.deleteFolder(name) } }
    fun addThreadToFolder(threadId: String, folderName: String) { viewModelScope.launch { threadDao.insertThreadFolderLink(ThreadFolderLink(threadId, folderName)) } }
    fun removeThreadFromFolder(threadId: String, folderName: String) { viewModelScope.launch { threadDao.removeThreadFromFolder(threadId, folderName) } }

    fun setUndoSendPeriod(ms: Long) { 
        try { prefs.edit { putLong("undo_period_ms", ms) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(undoSendPeriodMs = ms) } 
    }
    fun toggleDeliveryReports(enabled: Boolean) { 
        try { prefs.edit { putBoolean("delivery_reports", enabled) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(deliveryReportsEnabled = enabled) } 
    }
    fun updateUserName(name: String) { 
        val finalName = name.ifBlank { "Укажите имя" }
        try { prefs.edit { putString("user_name", finalName) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(userName = finalName) } 
    }
    fun updateUserPhone(phone: String) { 
        try { prefs.edit { putString("user_phone", phone) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(userPhone = phone) } 
    }

    fun toggleMeshMode(enabled: Boolean) {
        try { prefs.edit { putBoolean("mesh_mode", enabled) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(isMeshModeEnabled = enabled) }
    }
    fun toggleHideFinance(enabled: Boolean) {
        try { prefs.edit { putBoolean("hide_finance", enabled) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(hideFinanceInNotifications = enabled) }
    }
    fun toggleHideCodes(enabled: Boolean) {
        try { prefs.edit { putBoolean("hide_codes", enabled) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(hideCodesInNotifications = enabled) }
    }
    fun toggleBiometricAuth(enabled: Boolean) {
        try { prefs.edit { putBoolean("biometric_auth", enabled) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(biometricAuthEnabled = enabled) }
    }
    fun toggleScreenshotBlock(enabled: Boolean) {
        try { prefs.edit { putBoolean("screenshot_block", enabled) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(screenshotBlockEnabled = enabled) }
    }
    fun toggleYandexCallerId(enabled: Boolean) {
        try { prefs.edit { putBoolean("yandex_caller_id", enabled) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(yandexCallerIdEnabled = enabled) }
    }
    fun toggleAiDeduplication(enabled: Boolean) {
        try { prefs.edit { putBoolean("ai_deduplication", enabled) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(aiContactDeduplication = enabled) }
    }
    fun togglePrivacyShield(enabled: Boolean) {
        try { prefs.edit { putBoolean("privacy_shield", enabled) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(isPrivacyShieldEnabled = enabled) }
    }
    fun toggleDynamicColor(enabled: Boolean) {
        try { prefs.edit { putBoolean("dynamic_color", enabled) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(isDynamicColorEnabled = enabled) }
    }
    fun setThemeMode(mode: ThemeMode) {
        try { prefs.edit { putString("theme_mode", mode.id) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(themeMode = mode) }
    }
    fun setThemePreset(preset: AppThemePreset) {
        try {
            prefs.edit {
                putString("theme_preset", preset.id)
                putBoolean("dynamic_color", false)
            }
        } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(themePreset = preset, isDynamicColorEnabled = false) }
    }
    fun setFontScale(scale: AppFontScale) {
        try { prefs.edit { putString("font_scale", scale.id) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(fontScale = scale) }
    }

    fun toggleProMode(): Boolean {
        val newStatus = !_uiState.value.isProModeUnlocked
        try { prefs.edit { putBoolean("pro_mode_unlocked", newStatus) } } catch (e: Exception) { Log.e("MessagesViewModel", "Error saving pref", e) }
        _uiState.update { it.copy(isProModeUnlocked = newStatus) }
        return newStatus
    }

    fun copyOtp() {
        val code = _uiState.value.otpCode?.code ?: return
        viewModelScope.launch {
            _uiEvents.send(UiEvent.CopyToClipboard(code))
            _uiEvents.send(UiEvent.ShowSnackBar("Код $code скопирован"))
        }
        _uiState.update { it.copy(otpCode = null) }
    }

    fun copyMessageText(text: String) {
        viewModelScope.launch {
            _uiEvents.send(UiEvent.CopyToClipboard(text))
            _uiEvents.send(UiEvent.ShowSnackBar("Текст скопирован"))
        }
    }

    fun saveToSim(name: String, number: String) {
        viewModelScope.launch {
            val success = systemDataProvider.saveToSim(name, number)
            _uiEvents.send(UiEvent.ShowSnackBar(if (success) "Контакт сохранен на SIM" else "Ошибка при сохранении на SIM"))
        }
    }

    fun updateUserAvatarFromUri(sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val avatarFile = File(context.filesDir, "profile_avatar.jpg")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    avatarFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val savedUri = Uri.fromFile(avatarFile).toString()
                prefs.edit { putString("user_avatar_uri", savedUri) }
                _uiState.update { it.copy(userAvatarUri = savedUri) }
                _uiEvents.send(UiEvent.ShowSnackBar("Аватар обновлен"))
            } catch (e: Exception) {
                Log.e("MessagesViewModel", "Error saving avatar", e)
                _uiEvents.send(UiEvent.ShowSnackBar("Ошибка сохранения фото"))
            }
        }
    }

    fun updateUserAvatarFromBitmap(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val avatarFile = File(context.filesDir, "profile_avatar.jpg")
                avatarFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                val savedUri = Uri.fromFile(avatarFile).toString()
                prefs.edit { putString("user_avatar_uri", savedUri) }
                _uiState.update { it.copy(userAvatarUri = savedUri) }
                _uiEvents.send(UiEvent.ShowSnackBar("Аватар обновлен"))
            } catch (e: Exception) {
                Log.e("MessagesViewModel", "Error saving camera avatar", e)
                _uiEvents.send(UiEvent.ShowSnackBar("Ошибка сохранения фото"))
            }
        }
    }

    fun removeUserAvatar() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val avatarFile = File(context.filesDir, "profile_avatar.jpg")
            if (avatarFile.exists()) avatarFile.delete()
            prefs.edit { remove("user_avatar_uri") }
            _uiState.update { it.copy(userAvatarUri = null) }
            _uiEvents.send(UiEvent.ShowSnackBar("Аватар удален"))
        }
    }

    fun updateContactAvatarFromUri(contactKey: String, sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val safeKey = contactKey.replace(Regex("[^a-zA-Z0-9]"), "_")
                val avatarFile = File(context.filesDir, "contact_avatar_$safeKey.jpg")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    avatarFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val savedUri = Uri.fromFile(avatarFile).toString()
                prefs.edit { putString("contact_avatar_$safeKey", savedUri) }
                _uiState.update { state ->
                    val updatedThreads = state.threads.map {
                        if (it.id == contactKey || it.phoneNumber == contactKey) it.copy(avatarUri = savedUri) else it
                    }
                    state.copy(threads = updatedThreads)
                }
                _uiEvents.send(UiEvent.ShowSnackBar("Фото контакта обновлено"))
            } catch (e: Exception) {
                Log.e("MessagesViewModel", "Error saving contact avatar", e)
            }
        }
    }

    fun getContactAvatarUri(contactKey: String): String? {
        val safeKey = contactKey.replace(Regex("[^a-zA-Z0-9]"), "_")
        return prefs.getString("contact_avatar_$safeKey", null)
    }

    private fun ThreadEntity.toDomain() = MessageThread(
        id = id,
        contactName = contactName,
        phoneNumber = phoneNumber,
        avatarColor = getAvatarColorFor(contactName, phoneNumber),
        avatarUri = getContactAvatarUri(phoneNumber.ifEmpty { id }) ?: systemDataProvider.getContactPhotoUri(phoneNumber),
        lastMessage = lastMessage,
        lastMessageTime = lastMessageTime,
        unreadCount = unreadCount,
        isPinned = isPinned,
        isMachine = isMachine,
        category = try { CategoryFilter.valueOf(category) } catch (_: Exception) { CategoryFilter.ALL },
        folder = folder
    )

    private fun MessageThread.toEntity() = ThreadEntity(
        id = id,
        contactName = contactName,
        phoneNumber = phoneNumber,
        lastMessage = lastMessage,
        lastMessageTime = lastMessageTime,
        unreadCount = unreadCount,
        isPinned = isPinned,
        isMachine = isMachine,
        category = category.name,
        folder = folder
    )

    private fun MessageEntity.toDomain() = TimelineEvent.Message(
        id = id,
        timestamp = timestamp,
        content = content,
        isFromMe = isFromMe,
        isEncrypted = isEncrypted,
        status = try { MessageStatus.valueOf(status) } catch (_: Exception) { MessageStatus.SENT },
        mediaUri = mediaUri,
        mediaType = mediaType?.let { try { MediaType.valueOf(it) } catch (_: Exception) { null } },
        isEdited = isEdited,
        reactions = emptyMap()
    )

    fun showSnackBar(string: String) {
        viewModelScope.launch {
            _uiEvents.send(UiEvent.ShowSnackBar(string))
        }
    }
}
