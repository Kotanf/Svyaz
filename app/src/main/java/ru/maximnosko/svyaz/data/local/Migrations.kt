package ru.maximnosko.svyaz.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    // Migration from v1 to v2 - Added 'category' column to threads
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE threads ADD COLUMN category TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE messages ADD COLUMN mediaType TEXT")
            db.execSQL("ALTER TABLE messages ADD COLUMN mediaUri TEXT")
        }
    }

    // Migration from v2 to v3 - Added 'folder' for manual sorting, changed 'type' to ENUM
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE threads ADD COLUMN folder TEXT")
            db.execSQL("CREATE TABLE messages_new (id TEXT PRIMARY KEY, threadId TEXT, timestamp INTEGER, content TEXT, isFromMe INTEGER NOT NULL, isEncrypted INTEGER NOT NULL, isEdited INTEGER NOT NULL DEFAULT 0, status TEXT NOT NULL, mediaUri TEXT, mediaType TEXT)")
            db.execSQL("INSERT INTO messages_new (id, threadId, timestamp, content, isFromMe, isEncrypted, isEdited, status, mediaUri, mediaType) SELECT id, threadId, timestamp, content, isFromMe, isEncrypted, isEdited, status, mediaUri, mediaType FROM messages")
            db.execSQL("DROP TABLE messages")
            db.execSQL("ALTER TABLE messages_new RENAME TO messages")
        }
    }

    // Migration from v3 to v4 - Added 'unreadCount' column, created folders table
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE threads ADD COLUMN unreadCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE TABLE folders (id TEXT PRIMARY KEY, name TEXT NOT NULL)")
            db.execSQL("INSERT INTO folders (id, name) VALUES ('all', 'Все'), ('personal', 'Личные'), ('work', 'Служебные'), ('unread', 'Непрочитанные')")
        }
    }

    // Migration from v4 to v5 - Added 'isMachine' flag and 'folderId' to threads
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE threads ADD COLUMN isMachine INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE threads ADD COLUMN folderId TEXT REFERENCES folders(id)")
        }
    }

    // Migration from v5 to v6 - Adding compliance logs encryption flag
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE compliance_logs ADD COLUMN contentEncrypted INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE compliance_logs SET contentEncrypted = 1 WHERE content IS NOT NULL")
        }
    }

    // Migration from v6 to v7 - Added 'phoneNumber' column to threads
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE threads ADD COLUMN phoneNumber TEXT NOT NULL DEFAULT ''")
        }
    }
}
