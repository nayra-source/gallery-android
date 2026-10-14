# ProGuard / R8 Rules for Gallery Android

# Keep Kotlinx Serialization DTOs and models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class *$$serializer {
    *** INSTANCE;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}

# Keep Room entities & DAOs
-keepclassmembers class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Retrofit & OkHttp models
-keepclassmembers class com.aryan.gallery.data.remote.models.** { *; }
-keepclassmembers class com.aryan.gallery.service.handlers.** { *; }
