package com.emmetthoolahan.dicetospeech;

import android.app.Application;
import com.google.android.gms.ads.MobileAds;

public class F1App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize AdMob exactly once at app start
        MobileAds.initialize(this, status -> {});
    }
}
