# Add project specific ProGuard rules here.

# Keep Cloud Firestore POJOs & @IgnoreExtraProperties models for reflection deserialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.example.data.remote.** { *; }
-keepclassmembers class com.example.data.remote.** { *; }

# Keep domain models serialized or reflected
-keep class com.example.domain.** { *; }

# Keep Firebase App Check & reCAPTCHA Android SDK classes
-keep class com.google.firebase.appcheck.** { *; }
-keep class com.google.android.recaptcha.** { *; }

# Keep Jetpack Credential Manager & Google Identity classes
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
