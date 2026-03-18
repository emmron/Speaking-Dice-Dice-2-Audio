# Race Strategy Manager — ProGuard rules

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- Google Play Billing ----
-keep class com.android.billingclient.** { *; }
-keep interface com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ---- App classes used by billing callbacks ----
-keep class com.emmetthoolahan.dicetospeech.BillingManager { *; }
-keep class com.emmetthoolahan.dicetospeech.BillingManager$PurchaseListener { *; }

# ---- Android TTS ----
-keep class android.speech.tts.** { *; }
