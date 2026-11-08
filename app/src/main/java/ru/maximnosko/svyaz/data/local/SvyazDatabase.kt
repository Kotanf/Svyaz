package ru.maximnosko.svyaz.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory
import net.sqlcipher.database.SQLiteDatabase
import ru.maximnosko.svyaz.data.SecurityManager

@Database(
    entities = [
        ThreadEntity::class, 
        MessageEntity::class, 
        ComplianceLogEntity::class,
        FolderEntity::class,
        ThreadFolderLink::class
    ], 
    version = 8, 
    exportSchema = false
)
abstract class SvyazDatabase : RoomDatabase() {
    abstract fun threadDao(): ThreadDao

    companion object {
        @Volatile
        private var INSTANCE: SvyazDatabase? = null

        fun getDatabase(context: Context): SvyazDatabase {
            return INSTANCE ?: synchronized(this) {
                // Securely load passphrase from Android KeyStore
                val passphrase = SecurityManager.getDatabasePassphrase(context)
                val factory = SupportFactory(passphrase)
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SvyazDatabase::class.java,
                    "svyaz_database"
                )
                .openHelperFactory(factory)
                .addMigrations(
                    Migrations.MIGRATION_1_2, 
                    Migrations.MIGRATION_2_3, 
                    Migrations.MIGRATION_3_4, 
                    Migrations.MIGRATION_4_5, 
                    Migrations.MIGRATION_5_6,
                    Migrations.MIGRATION_6_7,
                    Migrations.MIGRATION_7_8
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
