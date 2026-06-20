# ==========================================
# MyPurse ProGuard/R8 混淆规则
# ==========================================

# --- 保留行号信息（方便调试崩溃堆栈）---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Room 数据库 ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# --- Hilt 依赖注入 ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# --- Kotlin Serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.wyd.mypurse.**$$serializer { *; }
-keepclassmembers class com.wyd.mypurse.** {
    *** Companion;
}
-keepclasseswithmembers class com.wyd.mypurse.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Compose / Navigation ---
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }

# --- 通用优化 ---
-keepattributes Signature
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
