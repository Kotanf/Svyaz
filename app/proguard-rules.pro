# Keep Room components
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keep @androidx.room.TypeConverter class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep RuStore SDK classes
-keep class com.rustore.** { *; }
-keep class ru.rustore.** { *; }

# Keep Compose and Material3
-keep class androidx.compose.** { *; }
-keep class com.google.android.material.** { *; }

# Keep Kotlin metadata for reflection
-keepclassmembers class ** {
    @org.jetbrains.annotations.NotNull *;
    @org.jetbrains.annotations.Nullable *;
}

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class androidx.lifecycle.** { *; }

# SQLCipher - Essential Rules for Release
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# Support SQLite classes used by SQLCipher/Room
-keep class androidx.sqlite.db.SupportSQLite* { *; }

# Prevent R8 from removing Base64 (used in compliance logging)
-keep class android.util.Base64 { *; }

# Keep domain models and entities from being obfuscated (essential for database/JSON)
-keep class ru.maximnosko.svyaz.data.** { *; }
-keep class ru.maximnosko.svyaz.data.local.** { *; }

# Keep enums and constants
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# Suppression of library warnings
-dontwarn java.awt.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**
