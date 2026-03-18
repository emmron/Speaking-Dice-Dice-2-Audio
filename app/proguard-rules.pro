# Race Strategy Manager — ProGuard rules

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- Google AdMob ----
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# ---- Google Play Billing ----
-keep class com.android.billingclient.** { *; }
-keep interface com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ---- App classes used by billing/ads callbacks ----
-keep class com.emmetthoolahan.dicetospeech.BillingManager { *; }
-keep class com.emmetthoolahan.dicetospeech.BillingManager$PurchaseListener { *; }
-keep class com.emmetthoolahan.dicetospeech.F1App { *; }

# ---- Android TTS ----
-keep class android.speech.tts.** { *; }
