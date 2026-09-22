# ==========================================================================
# 进制工坊 / RadixLab — R8 / ProGuard 规则
# ==========================================================================

# 保留行号，便于线上崩溃定位
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- Kotlin 元数据 ----
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ---- Kotlinx Coroutines ----
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ---- Jetpack Compose ----
# Compose 运行时的内部实现需要保留，否则 Release 包会崩溃
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.platform.** { *; }
-dontwarn androidx.compose.**

# ---- Navigation Compose（通过反射实例化路由）----
-keep class * extends androidx.navigation.NavArgs { *; }

# ---- DataStore / Protobuf ----
-dontwarn androidx.datastore.**
-keep class androidx.datastore.preferences.** { *; }

# ---- 应用自身的数据模型（JSON 反序列化依赖字段名）----
-keep class com.radixlab.app.data.model.** { *; }

# ---- 枚举与 Compose Preview ----
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- 去除日志 ----
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
