# ADNM Project ProGuard Rules

# 1. Volley Rules
-keep class com.android.volley.** { *; }
-keep interface com.android.volley.** { *; }

# 2. Firebase & GMS Rules (usually handled by AAR, but kept for safety)
-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.ktx.Firebase

# 3. Keep Data Models (to avoid issues with JSON parsing)
-keep class com.dldevalopement.adnm.home.reporter.Report { *; }
-keep class com.dldevalopement.adnm.home.reporter.WasteItem { *; }
-keep class com.dldevalopement.adnm.home.reporter.WasteType { *; }

# 4. Standard Android/Kotlin rules
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-dontwarn okio.**
-dontwarn javax.annotation.**

# 5. Start.io Rules
-keep class com.startapp.** { *; }
-keep class com.truenet.** { *; }
-dontwarn com.startapp.**
-dontwarn com.truenet.**

# 6. reCAPTCHA / SafetyNet
-keep class com.google.android.gms.safetynet.** { *; }

# 7. App Update Library
-keep class com.google.android.play.core.** { *; }
