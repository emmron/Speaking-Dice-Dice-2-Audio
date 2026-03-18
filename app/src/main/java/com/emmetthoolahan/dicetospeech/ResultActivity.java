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
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.Locale;

public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_POSITION  = "finish_position";
    public static final String EXTRA_POINTS    = "points_earned";
    public static final String EXTRA_DNF       = "dnf";
    public static final String EXTRA_TEAM_NAME = "team_name";

    // Replace with your real ad unit ID before publishing
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";

    private InterstitialAd interstitialAd;
    private BillingManager billingManager;
    private TextToSpeech tts;

    // Single field replaces the two redundant boolean flags (pendingNavToMenu / pendingNavToRace)
    private Class<?> pendingNavTarget = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        int     position = getIntent().getIntExtra(EXTRA_POSITION, 20);
        int     pts      = getIntent().getIntExtra(EXTRA_POINTS, 0);
        boolean dnf      = getIntent().getBooleanExtra(EXTRA_DNF, false);

        billingManager = new BillingManager(this, new BillingManager.PurchaseListener() {
            @Override public void onAdsRemoved() {}
            @Override public void onPremiumTeamsUnlocked() {}
            @Override public void onSeasonPassUnlocked() {}
        });

        ChampionshipManager cm = new ChampionshipManager(this);
        cm.addRaceResult(pts);

        TextView tvMedal      = findViewById(R.id.tv_medal);
        TextView tvFinishPos  = findViewById(R.id.tv_finish_position);
        TextView tvPoints     = findViewById(R.id.tv_points_earned);
        TextView tvTotal      = findViewById(R.id.tv_total_points);
        TextView tvChampPos   = findViewById(R.id.tv_championship_pos);

        if (dnf) {
            tvFinishPos.setText("DNF");
            tvFinishPos.setTextColor(0xFFF44336);
        } else {
            tvFinishPos.setText("P" + position);
            // Show podium medal for top 3
            String medal = podiumMedal(position);
            if (!medal.isEmpty()) {
                tvMedal.setText(medal);
                tvMedal.setVisibility(View.VISIBLE);
            }
        }
        tvPoints.setText("+" + pts + " Championship Points");
        tvTotal.setText("Season Total: " + cm.getPlayerPoints() + " pts");
        tvChampPos.setText("Championship Position: P" + cm.getPlayerChampionshipPosition());

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
            loadInterstitial();
        }
    }

    private String podiumMedal(int position) {
        switch (position) {
            case 1: return "\uD83E\uDD47"; // 🥇
            case 2: return "\uD83E\uDD48"; // 🥈
            case 3: return "\uD83E\uDD49"; // 🥉
            default: return "";
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
                                navigateTo(pendingNavTarget);
                            }
                        });
                    }
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError e) {}
                });
    }

    public void onNextRaceClicked(View view) {
        showAdThenNavigate(TeamSelectActivity.class);
    }

    public void onMainMenuClicked(View view) {
        showAdThenNavigate(MainActivity.class);
    }

    private void showAdThenNavigate(Class<?> target) {
        if (interstitialAd != null) {
            pendingNavTarget = target;
            interstitialAd.show(this);
        } else {
            navigateTo(target);
        }
    }

    private void navigateTo(Class<?> target) {
        if (target == null) return;
        Intent intent = new Intent(this, target);
        if (target == MainActivity.class) {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (interstitialAd != null) {
            interstitialAd.setFullScreenContentCallback(null);
            interstitialAd = null;
        }
        if (tts != null) { tts.stop(); tts.shutdown(); }
        billingManager.destroy();
        super.onDestroy();
    }
}
