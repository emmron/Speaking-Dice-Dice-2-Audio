package com.emmetthoolahan.dicetospeech;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.Locale;

public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_POSITION  = "finish_position";
    public static final String EXTRA_POINTS    = "points_earned";
    public static final String EXTRA_DNF       = "dnf";
    public static final String EXTRA_TEAM_NAME = "team_name";

    // Replace with your real interstitial ad unit ID before publishing
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";

    private InterstitialAd interstitialAd;
    private BillingManager billingManager;
    private TextToSpeech tts;

    private boolean pendingNavToMenu  = false;
    private boolean pendingNavToRace  = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        int    position    = getIntent().getIntExtra(EXTRA_POSITION, 20);
        int    pts         = getIntent().getIntExtra(EXTRA_POINTS, 0);
        boolean dnf        = getIntent().getBooleanExtra(EXTRA_DNF, false);

        billingManager = new BillingManager(this, new BillingManager.PurchaseListener() {
            @Override public void onAdsRemoved() {}
            @Override public void onPremiumTeamsUnlocked() {}
            @Override public void onSeasonPassUnlocked() {}
        });

        ChampionshipManager cm = new ChampionshipManager(this);
        cm.addRaceResult(pts);

        TextView tvFinishPos  = findViewById(R.id.tv_finish_position);
        TextView tvPoints     = findViewById(R.id.tv_points_earned);
        TextView tvTotal      = findViewById(R.id.tv_total_points);
        TextView tvChampPos   = findViewById(R.id.tv_championship_pos);

        if (dnf) {
            tvFinishPos.setText("DNF");
            tvFinishPos.setTextColor(0xFFFF3333);
        } else {
            tvFinishPos.setText("P" + position);
        }
        tvPoints.setText("+" + pts + " Championship Points");
        tvTotal.setText("Season Total: " + cm.getPlayerPoints() + " pts");
        tvChampPos.setText("Championship Position: P" + cm.getPlayerChampionshipPosition());

        // TTS announcement
        String announcement = dnf
                ? "DNF. Tough race. Better luck next time!"
                : "Finished P" + position + ". " + pts + " championship points earned!";
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        if (!billingManager.isAdsRemoved()) {
            MobileAds.initialize(this, s -> {});
            loadInterstitial();
        }
    }

    private void loadInterstitial() {
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                if (pendingNavToMenu) goToMainMenu();
                                else if (pendingNavToRace) goToTeamSelect();
                            }
                        });
                    }
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError e) {}
                });
    }

    public void onNextRaceClicked(View view) {
        if (interstitialAd != null) {
            pendingNavToRace = true;
            interstitialAd.show(this);
        } else {
            goToTeamSelect();
        }
    }

    public void onMainMenuClicked(View view) {
        if (interstitialAd != null) {
            pendingNavToMenu = true;
            interstitialAd.show(this);
        } else {
            goToMainMenu();
        }
    }

    private void goToMainMenu() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void goToTeamSelect() {
        startActivity(new Intent(this, TeamSelectActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        billingManager.destroy();
        super.onDestroy();
    }
}
