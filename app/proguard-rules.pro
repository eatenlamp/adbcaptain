# Keep Shizuku API — accessed via reflection in some flows
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }

# Keep Hilt-generated components (required when processRes give lombok-like classes)
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class com.google.dagger.hilt.** { *; }

# Keep Room-generated database implementations
-keep class adb.captain.data.local.** { *; }
-keep,allowobfuscation,allowshrinking class * extends androidx.room.RoomDatabase

# Keep Compose view model implementations that Hilt injects
-keep,allowobfuscation,allowshrinking class adb.captain.presentation.**ViewModel

# Shizuku provider token red zone
-dontwarn rikka.shizuku.**

# Required for application services referenced from the manifest
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application