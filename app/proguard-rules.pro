# ============================================================================
# Svyaz (Связь) Bulletproof R8 / ProGuard Optimization & Keep Rules
# ============================================================================

# 1. Android Manifest Components (Activities, Services, Receivers, Application)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Application

# 2. Svyaz Application Data Models & Room Entities
-keep class ru.maximnosko.svyaz.data.** { *; }
-keep class ru.maximnosko.svyaz.data.local.** { *; }
-keep class ru.maximnosko.svyaz.viewmodel.MessagesUiState { *; }
-keep class ru.maximnosko.svyaz.viewmodel.UiEvent { *; }

# 3. Room Database Components & Generated Implementations
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomOpenHelper
-keep class * extends androidx.sqlite.db.SupportSQLiteOpenHelper$Factory
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keep @androidx.room.TypeConverter class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# 4. SQLCipher Encrypted SQLite Engine & JNI Native Methods
-keepclassmembers class * {
    native <methods>;
}
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keep class androidx.sqlite.db.SupportSQLite* { *; }
-dontwarn net.sqlcipher.**

# 5. WorkManager & Sync Workers (Reflective Instantiation)
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class ru.maximnosko.svyaz.data.sync.** { *; }

# 6. RuStore SDK & Push Client
-keep class com.rustore.** { *; }
-keep class ru.rustore.** { *; }

# 7. ViewModels (Reflective Instantiation by ViewModelProvider)
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}
-keep class ru.maximnosko.svyaz.viewmodel.MessagesViewModel { *; }

# 8. Enums, Theme Models & Parcelables
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public ** name();
    public ** ordinal();
}
-keep class ru.maximnosko.svyaz.ui.theme.ThemeMode { *; }
-keep class ru.maximnosko.svyaz.ui.theme.AppThemePreset { *; }
-keep class ru.maximnosko.svyaz.ui.theme.AppFontScale { *; }
-keep class ru.maximnosko.svyaz.data.CategoryFilter { *; }
-keep class ru.maximnosko.svyaz.data.MessageStatus { *; }
-keep class ru.maximnosko.svyaz.data.MediaType { *; }

# 9. Kotlin Coroutines & Dispatchers
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# 10. Suppression of Unused Library Warnings
-dontwarn java.awt.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**
