# Add project specific ProGuard rules here.

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep enums (stored as strings in Room DB)
-keepclassmembers enum * { *; }

# Keep HiltWorker classes (instantiated by class name via WorkManager)
-keep class com.woliveiras.petit.worker.** { *; }

# Keep Navigation Compose routes (string-based sealed class)
-keep class com.woliveiras.petit.presentation.navigation.Screen { *; }
-keep class com.woliveiras.petit.presentation.navigation.Screen$* { *; }
