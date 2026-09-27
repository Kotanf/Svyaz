package ru.maximnosko.svyaz.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_folders")
data class FolderEntity(
    @PrimaryKey val name: String,
    val icon: String = "folder" // Store icon name
)

@Entity(tableName = "thread_folder_links", primaryKeys = ["threadId", "folderName"])
data class ThreadFolderLink(
    val threadId: String,
    val folderName: String
)
