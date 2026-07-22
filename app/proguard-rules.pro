# OHMusic ProGuard Rules
-keepclassmembers class com.ohmusic.app.data.model.** { *; }
-keep class com.ohmusic.app.data.db.AppDatabase { *; }

# Hilt
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }

# Room
-keep class androidx.room.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Coil
-keep class coil3.** { *; }

# Prevent shrinking of essential classes
-keep class com.ohmusic.app.player.** { *; }
-keep class com.ohmusic.app.ui.player.** { *; }
-keep class com.ohmusic.app.ui.playlist.** { *; }
-keep class com.ohmusic.app.util.** { *; }
