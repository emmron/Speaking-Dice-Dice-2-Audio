package com.emmetthoolahan.dicetospeech;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.Locale;

public class GameActivity extends AppCompatActivity {

    private static final int TOTAL_LAPS_FREE    = 5;
    private static final int TOTAL_LAPS_PREMIUM = 10;

    private RaceEngine raceEngine;
    private TextToSpeech tts;
    private BillingManager billingManager;
    private AdView bannerAdView;

    private TextView tvLap, tvPosition, tvTeamName, tvTire, tvEvent, tvLastResult;
    private Button btnDecision1, btnDecision2, btnDecision3;

    private RaceEngine.Decision[] currentDecisions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        billingManager = new BillingManager(this, new BillingManager.PurchaseListener() {
            @Override
            public void onAdsRemoved() {
                runOnUiThread(() -> bannerAdView.setVisibility(View.GONE));
            }
            @Override public void onPremiumTeamsUnlocked() {}
            @Override public void onSeasonPassUnlocked() {}
        });

        int teamIndex = getIntent().getIntExtra(TeamSelectActivity.EXTRA_TEAM_INDEX, 0);
        Team selectedTeam = TeamData.getAllTeams()[teamIndex];
        int totalLaps = billingManager.isSeasonPassOwned() ? TOTAL_LAPS_PREMIUM : TOTAL_LAPS_FREE;
        raceEngine = new RaceEngine(selectedTeam, totalLaps);

        tvLap        = findViewById(R.id.tv_lap);
        tvPosition   = findViewById(R.id.tv_position);
        tvTeamName   = findViewById(R.id.tv_team_name);
        tvTire       = findViewById(R.id.tv_tire);
        tvEvent      = findViewById(R.id.tv_event);
        tvLastResult = findViewById(R.id.tv_last_result);
        btnDecision1 = findViewById(R.id.btn_decision_1);
        btnDecision2 = findViewById(R.id.btn_decision_2);
        btnDecision3 = findViewById(R.id.btn_decision_3);
        bannerAdView = findViewById(R.id.adView);

        MobileAds.initialize(this, s -> {});
        if (!billingManager.isAdsRemoved()) {
            bannerAdView.loadAd(new AdRequest.Builder().build());
        } else {
            bannerAdView.setVisibility(View.GONE);
        }

        tvTeamName.setText("Team: " + selectedTeam.name);
        tvTeamName.setTextColor(selectedTeam.color);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US);
        });

        nextLap();
    }

    private void speak(String text) {
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void nextLap() {
        if (raceEngine.isRaceOver()) {
            goToResults();
            return;
        }

        RaceEngine.Event event = raceEngine.generateEvent();
        String announcement = raceEngine.getEventAnnouncement();

        tvLap.setText("LAP " + raceEngine.currentLap + " / " + raceEngine.totalLaps);
        tvPosition.setText("P" + raceEngine.position);
        tvTire.setText(raceEngine.getTireStatus());
        tvEvent.setText(announcement);
        tvLastResult.setText("");

        speak("Lap " + raceEngine.currentLap + ". " + announcement);
        setupDecisionButtons(event);
    }

    private void setupDecisionButtons(RaceEngine.Event event) {
        boolean tireWorn  = raceEngine.tireWear > 60;
        boolean raining   = raceEngine.isRaining;

        if (event == RaceEngine.Event.RAIN
                || event == RaceEngine.Event.SAFETY_CAR
                || event == RaceEngine.Event.TIRE_WARNING
                || tireWorn) {
            currentDecisions = new RaceEngine.Decision[]{
                raining ? RaceEngine.Decision.PIT_WET   : RaceEngine.Decision.PIT_MEDIUM,
                RaceEngine.Decision.PIT_HARD,
                RaceEngine.Decision.STAY_NORMAL
            };
            btnDecision1.setText(raining ? "RAIN PIT — WET TIRES" : "PIT — MEDIUM TIRES");
            btnDecision2.setText("PIT — HARD TIRES (slower but lasts)");
            btnDecision3.setText("STAY OUT (risky)");

        } else if (event == RaceEngine.Event.DRS_ZONE || event == RaceEngine.Event.ENGINE_ISSUE) {
            currentDecisions = new RaceEngine.Decision[]{
                RaceEngine.Decision.STAY_PUSH,
                RaceEngine.Decision.STAY_SAVE,
                RaceEngine.Decision.PIT_MEDIUM
            };
            btnDecision1.setText("PUSH ENGINE — ATTACK");
            btnDecision2.setText("SAVE ENGINE — PROTECT POSITION");
            btnDecision3.setText("PIT — MEDIUM TIRES");

        } else {
            currentDecisions = new RaceEngine.Decision[]{
                RaceEngine.Decision.STAY_PUSH,
                RaceEngine.Decision.STAY_NORMAL,
                RaceEngine.Decision.PIT_MEDIUM
            };
            btnDecision1.setText("ATTACK — PUSH FOR POSITIONS");
            btnDecision2.setText("MANAGE — CONSERVE TIRES");
            btnDecision3.setText("PIT — MEDIUM TIRES");
        }

        btnDecision1.setOnClickListener(v -> makeDecision(0));
        btnDecision2.setOnClickListener(v -> makeDecision(1));
        btnDecision3.setOnClickListener(v -> makeDecision(2));
    }

    private void makeDecision(int index) {
        setButtonsEnabled(false);

        String result = raceEngine.applyDecision(currentDecisions[index]);
        tvLastResult.setText(result);
        speak(result);

        // Brief pause so player can read the result before advancing to next lap
        tvLap.postDelayed(() -> {
            setButtonsEnabled(true);
            nextLap();
        }, 3500);
    }

    private void setButtonsEnabled(boolean enabled) {
        btnDecision1.setEnabled(enabled);
        btnDecision2.setEnabled(enabled);
        btnDecision3.setEnabled(enabled);
    }

    private void goToResults() {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(ResultActivity.EXTRA_POSITION,  raceEngine.position);
        intent.putExtra(ResultActivity.EXTRA_POINTS,    raceEngine.getChampionshipPoints());
        intent.putExtra(ResultActivity.EXTRA_DNF,       raceEngine.dnf);
        intent.putExtra(ResultActivity.EXTRA_TEAM_NAME, raceEngine.team.name);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (bannerAdView != null) bannerAdView.destroy();
        billingManager.destroy();
        super.onDestroy();
    }
}
