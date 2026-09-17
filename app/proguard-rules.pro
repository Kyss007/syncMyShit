# Keep Google Drive API and HTTP client models
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keepattributes Signature, *Annotation*
-dontwarn com.google.api.client.**
-dontwarn com.google.common.**
-dontwarn org.apache.http.**
