# Gson - 防止数据类字段被混淆
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 保留所有数据模型类（Gson解析用）
-keep class com.permissionx.animalguide.data.remote.dto.** { *; }
-keep class com.permissionx.animalguide.domain.model.** { *; }

# Room - 防止数据库实体被混淆
-keep class com.permissionx.animalguide.data.local.entity.** { *; }

# Retrofit
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# CameraX
-keep class androidx.camera.** { *; }

# Coil
-keep class coil.** { *; }

# 保留 BuildConfig
-keep class com.permissionx.animalguide.BuildConfig { *; }

# 保留应用入口
-keep class com.permissionx.animalguide.AnimalApp { *; }
-keep class com.permissionx.animalguide.MainActivity { *; }

# 移除所有Log调用
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}