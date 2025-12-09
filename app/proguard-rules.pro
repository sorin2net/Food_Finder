# ============================================
# PROGUARD RULES - Food Finder App
# ============================================

# ===== FIREBASE =====
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Firebase Realtime Database
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
}
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Firebase Analytics & Crashlytics
-keep class com.google.android.gms.measurement.** { *; }
-keep class com.google.firebase.crashlytics.** { *; }

# ===== DATA CLASSES (MODELE) =====
# CRITIC: Asta protejeaza StoreModel ca sa mearga Firebase
-keep class com.example.sharoma_finder.domain.** { *; }
-keepclassmembers class com.example.sharoma_finder.domain.** {
    <init>(...);
    <fields>;
}

# ===== COIL (IMAGE LOADING) =====
-dontwarn coil.**
-keep class coil.** { *; }
-keep interface coil.** { *; }

# OkHttp (used by Coil)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
# Platform specific fixes for OkHttp
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ===== GOOGLE MAPS & LOCATION =====
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# ===== KOTLIN =====
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# ===== JETPACK COMPOSE =====
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.compose.**

# Compose Runtime
-keepclassmembers class androidx.compose.runtime.** {
    <methods>;
}

# ===== ANDROIDX =====
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**
-keep class androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ===== ACCOMPANIST (System UI Controller) =====
-keep class com.google.accompanist.** { *; }
-dontwarn com.google.accompanist.**

# ===== GENERAL RULES =====
# Keep line numbers for crash reports (important for Crashlytics!)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic signatures
-keepattributes Signature

# Keep annotations
-keepattributes *Annotation*

# Prevent crashes from missing classes (Java 8+)
-dontwarn java.lang.invoke.StringConcatFactory

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep Enums (previne erori la valueOf)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===== DEBUGGING CLEANUP =====
# Sterge Log.d, Log.e, etc din versiunea Release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# ===== R8 OPTIMIZATIONS =====
# Permite R8