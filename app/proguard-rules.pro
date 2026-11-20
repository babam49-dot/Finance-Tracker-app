# Keep Room annotations and schema
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep models for kotlinx.serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Keep Hilt
-keep class * extends dagger.hilt.internal.GeneratedComponent
