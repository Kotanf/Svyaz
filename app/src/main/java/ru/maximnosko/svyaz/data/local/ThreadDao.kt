package ru.maximnosko.svyaz.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao {
    @Query("SELECT * FROM threads ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getAllThreads(): Flow<List<ThreadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreads(threads: List<ThreadEntity>)

    @Query("UPDATE threads SET folder = :folderName WHERE id = :threadId")
    suspend fun updateFolder(threadId: String, folderName: String?)

    @Query("DELETE FROM threads WHERE id = :threadId")
    suspend fun deleteThread(threadId: String)

    @Query("DELETE FROM messages WHERE threadId = :threadId")
    suspend fun deleteMessagesForThread(threadId: String)

    @Query("DELETE FROM threads WHERE folder = 'Корзина'")
    suspend fun emptyTrashThreads()

    @Query("SELECT * FROM threads WHERE folder = :folderName")
    suspend fun getThreadsInFolderList(folderName: String): List<ThreadEntity>

    // Messages
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    suspend fun getMessagesForThread(threadId: String): List<MessageEntity>

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    // Compliance / ORI
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertComplianceLog(log: ComplianceLogEntity)

    @Query("SELECT * FROM compliance_logs ORDER BY timestamp DESC")
    fun getAllComplianceLogs(): Flow<List<ComplianceLogEntity>>

    // Custom Folders
    @Query("SELECT * FROM chat_folders")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("DELETE FROM chat_folders WHERE name = :name")
    suspend fun deleteFolder(name: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreadFolderLink(link: ThreadFolderLink)

    @Query("DELETE FROM thread_folder_links WHERE threadId = :threadId AND folderName = :folderName")
    suspend fun removeThreadFromFolder(threadId: String, folderName: String)

    @Query("SELECT threadId FROM thread_folder_links WHERE folderName = :folderName")
    fun getThreadIdsInFolder(folderName: String): Flow<List<String>>
}
