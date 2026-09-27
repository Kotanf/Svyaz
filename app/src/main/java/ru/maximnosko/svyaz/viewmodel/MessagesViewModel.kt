package ru.maximnosko.svyaz.viewmodel

import android.app.Application
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import androidx.core.net.toUri
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
import ru.maximnosko.svyaz.data.*
import ru.maximnosko.svyaz.data.local.*
import ru.maximnosko.svyaz.data.sync.SmsSyncWorker
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

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
    val selectedSimSlot: Int = 1
) {
    val allFolderNames: List<String>
        get() = listOf("Все", "Личные", "Служебные", "Непрочитанные") + 
                customFolders.map { it.name }.filter { it !in listOf("Все", "Личные", "Служебные", "Непрочитанные") }
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
            undoSendPeriodMs = prefs.getLong("undo_period_ms", 3000L),
            deliveryReportsEnabled = prefs.getBoolean("delivery_reports", true),
            isMeshModeEnabled = prefs.getBoolean("mesh_mode", false),
            hideFinanceInNotifications = prefs.getBoolean("hide_finance", true),
            hideCodesInNotifications = prefs.getBoolean("hide_codes", true),
            biometricAuthEnabled = prefs.getBoolean("biometric_auth", true),
            screenshotBlockEnabled = prefs.getBoolean("screenshot_block", true),
            yandexCallerIdEnabled = prefs.getBoolean("yandex_caller_id", true),
            aiContactDeduplication = prefs.getBoolean("ai_deduplication", true)
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
        val baseList = when (selectedFolder) {
            "Личные" -> threads.filter { !it.isMachine }
            "Служебные" -> threads.filter { it.isMachine }
            "Непрочитанные" -> threads.filter { it.unreadCount > 0 }
            "Все" -> threads
            else -> threads.filter { it.id in threadIdsInSelectedFolder }
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
    private var progressJob: Job? = null

    private val smsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            Log.d("MessagesViewModel", "System SMS DB changed, triggering sync")
            syncSystemData()
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
            
            val cr = application.contentResolver
            cr.registerContentObserver(Telephony.Sms.CONTENT_URI, true, smsObserver)
            cr.registerContentObserver(Telephony.Mms.CONTENT_URI, true, smsObserver)
            
            syncSystemData()
        }
    }

    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(smsObserver)
    }

    fun onPermissionsGranted() {
        _uiState.update { it.copy(isPermissionsGranted = true) }
        syncSystemData()
    }

    fun syncSystemData() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            try {
                Log.d("MessagesViewModel", "Sync started...")
                _uiState.update { it.copy(isSyncing = true) }
                
                val threads = systemDataProvider.fetchThreads()
                val contacts = systemDataProvider.fetchContacts()
                val callLogs = systemDataProvider.fetchCallLogs()
                
                Log.d("MessagesViewModel", "Sync fetched ${threads.size} threads")
                
                // CRITICAL: Update DB first
                threadDao.insertThreads(threads.map { it.toEntity() })
                
                // Enqueue background SmsSyncWorker to sync system messages into local Room DB
                try {
                    val syncWorkRequest = OneTimeWorkRequestBuilder<SmsSyncWorker>().build()
                    WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                        "SmsSyncWorker",
                        ExistingWorkPolicy.REPLACE,
                        syncWorkRequest
                    )
                } catch (e: Exception) {
                    Log.e("MessagesViewModel", "Failed to enqueue SmsSyncWorker", e)
                }
                
                val latestOtp = threads.firstOrNull { it.category == CategoryFilter.CODES }?.let {
                    detectOtpCode(it.lastMessage, it.contactName)
                }
                
                _uiState.update { state ->
                    state.copy(
                        threads = if (threads.isNotEmpty()) threads else state.threads,
                        contacts = contacts, 
                        callLogs = callLogs, 
                        isSyncing = false,
                        otpCode = latestOtp
                    )
                }

                _uiState.value.activeThreadId?.let { loadTimeline(it) }
            } catch (e: Exception) {
                Log.e("MessagesViewModel", "Sync failed", e)
                _uiState.update { it.copy(isSyncing = false) }
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
        val otpKeywords = listOf("код", "code", "pass", "пароль", "otp", "аутентификаци", "подтвержд")
        if (!otpKeywords.any { lower.contains(it) }) return null

        val regex = Regex("(\\d{3}-\\d{3}|\\d{4,6})")
        val match = regex.find(text)
        return match?.let { OtpCodeData(code = it.value, sender = sender) }
    }

    fun loadTimeline(idOrAddress: String) {
        viewModelScope.launch {
            try {
                val (normalizedThreadId, systemEvents) = systemDataProvider.fetchTimeline(idOrAddress)
                _uiState.update { it.copy(activeThreadId = normalizedThreadId) }

                val localEvents = threadDao.getMessagesForThread(normalizedThreadId).map { it.toDomain() }
                val combined = (systemEvents + localEvents).distinctBy { it.id }.sortedBy { it.timestamp }
                
                val thread = _uiState.value.threads.find { it.id == idOrAddress || it.id == normalizedThreadId || it.phoneNumber == idOrAddress }
                val resolvedPhone = thread?.phoneNumber?.takeIf { it.isNotBlank() } ?: systemDataProvider.resolveAddress(idOrAddress) ?: systemDataProvider.resolveAddress(normalizedThreadId)

                _uiState.update { state ->
                    val updatedMap = state.timelineByThread.toMutableMap()
                    updatedMap[normalizedThreadId] = combined
                    updatedMap[idOrAddress] = combined
                    if (!resolvedPhone.isNullOrBlank()) {
                        updatedMap[resolvedPhone] = combined
                    }
                    state.copy(timelineByThread = updatedMap)
                }
            } catch (e: Exception) {
                Log.e("MessagesViewModel", "Timeline load error", e)
            }
        }
    }

    fun onCloseChat() {
        _uiState.update { it.copy(activeThreadId = null) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFolderSelected(folderName: String) {
        _uiState.update { it.copy(selectedFolder = folderName) }
        if (folderName !in listOf("Все", "Личные", "Служебные", "Непрочитанные")) {
            viewModelScope.launch {
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

    fun onDeleteThread(thread: MessageThread) {
        _uiState.update { it.copy(threads = it.threads.filter { it.id != thread.id }) }
    }

    fun sendMessage(threadId: String, content: String, isMms: Boolean = false, mediaUri: String? = null, mediaType: MediaType? = null) {
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
                    val sentMessage = repository.sendMessage(finalAddress, previousPending.content, previousPending.isMms, previousPending.mediaUri, previousPending.mediaType)
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
                    subId = _uiState.value.selectedSimSlot
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
        _uiState.update { state ->
            val updatedMap = state.timelineByThread.toMutableMap()
            updatedMap[threadId] = (state.timelineByThread[threadId] ?: emptyList()) + finalMessage
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
        prefs.edit().putLong("undo_period_ms", ms).apply()
        _uiState.update { it.copy(undoSendPeriodMs = ms) } 
    }
    fun toggleDeliveryReports(enabled: Boolean) { 
        prefs.edit().putBoolean("delivery_reports", enabled).apply()
        _uiState.update { it.copy(deliveryReportsEnabled = enabled) } 
    }
    fun updateUserName(name: String) { 
        val finalName = name.ifBlank { "Укажите имя" }
        prefs.edit().putString("user_name", finalName).apply()
        _uiState.update { it.copy(userName = finalName) } 
    }
    fun updateUserPhone(phone: String) { 
        prefs.edit().putString("user_phone", phone).apply()
        _uiState.update { it.copy(userPhone = phone) } 
    }

    fun toggleMeshMode(enabled: Boolean) {
        prefs.edit().putBoolean("mesh_mode", enabled).apply()
        _uiState.update { it.copy(isMeshModeEnabled = enabled) }
    }
    fun toggleHideFinance(enabled: Boolean) {
        prefs.edit().putBoolean("hide_finance", enabled).apply()
        _uiState.update { it.copy(hideFinanceInNotifications = enabled) }
    }
    fun toggleHideCodes(enabled: Boolean) {
        prefs.edit().putBoolean("hide_codes", enabled).apply()
        _uiState.update { it.copy(hideCodesInNotifications = enabled) }
    }
    fun toggleBiometricAuth(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_auth", enabled).apply()
        _uiState.update { it.copy(biometricAuthEnabled = enabled) }
    }
    fun toggleScreenshotBlock(enabled: Boolean) {
        prefs.edit().putBoolean("screenshot_block", enabled).apply()
        _uiState.update { it.copy(screenshotBlockEnabled = enabled) }
    }
    fun toggleYandexCallerId(enabled: Boolean) {
        prefs.edit().putBoolean("yandex_caller_id", enabled).apply()
        _uiState.update { it.copy(yandexCallerIdEnabled = enabled) }
    }
    fun toggleAiDeduplication(enabled: Boolean) {
        prefs.edit().putBoolean("ai_deduplication", enabled).apply()
        _uiState.update { it.copy(aiContactDeduplication = enabled) }
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

    private fun ThreadEntity.toDomain() = MessageThread(
        id = id,
        contactName = contactName,
        phoneNumber = phoneNumber, 
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
}
