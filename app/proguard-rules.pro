# Kinetiq release rules (R8). The app is offline-only; no network serializers beyond Moshi backup.

# Room entities and database
-keep class com.example.data.model.** { *; }
-keep class com.example.data.db.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# Moshi backup payloads (reflection via KotlinJsonAdapterFactory + generated adapters)
-keep class com.example.data.backup.** { *; }
-keepclassmembers class com.example.data.backup.** { *; }
-keep interface com.squareup.moshi.JsonAdapter { *; }
-dontwarn org.jetbrains.annotations.**

# Keep line numbers for Play Console crash deobfuscation (mapping file is uploaded separately)
-keepattributes SourceFile,LineNumberTable
