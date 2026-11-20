# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.kootoro.torogscanner.data.model.** { *; }
-dontwarn org.apache.poi.**
