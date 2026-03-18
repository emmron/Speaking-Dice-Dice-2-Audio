package com.emmetthoolahan.dicetospeech;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.Locale;

public class GameActivity extends AppCompatActivity {

    private static final int TOTAL_LAPS_FREE    = 5;
    private static final int TOTAL_LAPS_PREMIUM = 10;
    private static final int NEXT_LAP_DELAY_MS  = 3500;

    // Event background colours (dark variants so white text is readable)
    private static final int COLOR_EVENT_DEFAULT      = 0xFF252525;
    private static final int COLOR_EVENT_SAFETY_CAR   = 0xFF4D3B00; // amber
    private static final int COLOR_EVENT_RAIN         = 0xFF003366; // blue
    private static final int COLOR_EVENT_DRS          = 0xFF003B00; // green
    private static final int COLOR_EVENT_TIRE_WARNING = 0xFF4D2200; // orange
    private static final int COLOR_EVENT_ENGINE       = 0xFF4D0000; // red

    // Tire wear bar colours
    private static final int COLOR_TIRE_GOOD     = 0xFF4CAF50; // green
    private static final int COLOR_TIRE_WORN     = 0xFFFF9800; // orange
    private static final int COLOR_TIRE_CRITICAL = 0xFFF44336; // red

    private RaceEngine raceEngine;
    private TextToSpeech tts;
    private BillingManager billingManager;
    private AdView bannerAdView;

    private TextView tvLap, tvPosition, tvTeamName, tvTire, tvTirePct, tvEvent, tvLastResult;
    private ProgressBar pbTireWear;
    private Button btnDecision1, btnDecision2, btnDecision3;

    private RaceEngine.Decision[] currentDecisions;
    private int previousPosition = -1;

    // Stored so we can cancel it in onDestroy if the activity is killed mid-delay
    private Runnable nextLapRunnable;

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
        Team[] teams = TeamData.getAllTeams();
        // Clamp index defensively
        teamIndex = Math.max(0, Math.min(teamIndex, teams.length - 1));
        Team selectedTeam = teams[teamIndex];

        int totalLaps = billingManager.isSeasonPassOwned() ? TOTAL_LAPS_PREMIUM : TOTAL_LAPS_FREE;
        raceEngine = new RaceEngine(selectedTeam, totalLaps);

        tvLap        = findViewById(R.id.tv_lap);
        tvPosition   = findViewById(R.id.tv_position);
        tvTeamName   = findViewById(R.id.tv_team_name);
        tvTire       = findViewById(R.id.tv_tire);
        tvTirePct    = findViewById(R.id.tv_tire_pct);
        tvEvent      = findViewById(R.id.tv_event);
        tvLastResult = findViewById(R.id.tv_last_result);
        pbTireWear   = findViewById(R.id.pb_tire_wear);
        btnDecision1 = findViewById(R.id.btn_decision_1);
        btnDecision2 = findViewById(R.id.btn_decision_2);
        btnDecision3 = findViewById(R.id.btn_decision_3);
        bannerAdView = findViewById(R.id.adView);

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
        updatePositionDisplay();
        tvTire.setText(raceEngine.getTireStatus());
        tvEvent.setText(announcement);
        tvEvent.setBackgroundColor(eventColor(event));
        tvLastResult.setText("");
        updateTireBar();

        speak("Lap " + raceEngine.currentLap + ". " + announcement);
        setupDecisionButtons(event);
    }

    private void updatePositionDisplay() {
        int pos = raceEngine.position;
        if (previousPosition < 0) {
            tvPosition.setText("P" + pos);
            tvPosition.setTextColor(0xFFFFD700); // gold
        } else {
            int delta = previousPosition - pos; // positive = gained positions
            if (delta > 0) {
                tvPosition.setText("P" + pos + " +" + delta + " \u2191");
                tvPosition.setTextColor(0xFF4CAF50); // green
            } else if (delta < 0) {
                tvPosition.setText("P" + pos + " " + delta + " \u2193");
                tvPosition.setTextColor(0xFFF44336); // red
            } else {
                tvPosition.setText("P" + pos);
                tvPosition.setTextColor(0xFFFFD700); // gold
            }
        }
        previousPosition = pos;
    }

    private void updateTireBar() {
        int wear = raceEngine.tireWear;
        pbTireWear.setProgress(wear);
        tvTirePct.setText(wear + "%");

        int tireColor;
        if (wear < 60)      tireColor = COLOR_TIRE_GOOD;
        else if (wear < 80) tireColor = COLOR_TIRE_WORN;
        else                tireColor = COLOR_TIRE_CRITICAL;
        pbTireWear.setProgressTintList(ColorStateList.valueOf(tireColor));
    }

    private int eventColor(RaceEngine.Event event) {
        switch (event) {
            case SAFETY_CAR:   return COLOR_EVENT_SAFETY_CAR;
            case RAIN:         return COLOR_EVENT_RAIN;
            case DRS_ZONE:     return COLOR_EVENT_DRS;
            case TIRE_WARNING: return COLOR_EVENT_TIRE_WARNING;
            case ENGINE_ISSUE: return COLOR_EVENT_ENGINE;
            default:           return COLOR_EVENT_DEFAULT;
        }
    }

    private void setupDecisionButtons(RaceEngine.Event event) {
        boolean tireWorn = raceEngine.tireWear > 60;
        boolean raining  = raceEngine.isRaining;

        if (event == RaceEngine.Event.RAIN
                || event == RaceEngine.Event.SAFETY_CAR
                || event == RaceEngine.Event.TIRE_WARNING
                || tireWorn) {
            currentDecisions = new RaceEngine.Decision[]{
                raining ? RaceEngine.Decision.PIT_WET : RaceEngine.Decision.PIT_MEDIUM,
                RaceEngine.Decision.PIT_HARD,
                RaceEngine.Decision.STAY_NORMAL
            };
            btnDecision1.setText(raining ? "PIT — WET TIRES" : "PIT — MEDIUM TIRES");
            btnDecision2.setText("PIT — HARD TIRES (lasts longer)");
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

        int[] indices = {0, 1, 2};
        Button[] buttons = {btnDecision1, btnDecision2, btnDecision3};
        for (int i = 0; i < buttons.length; i++) {
            final int idx = i;
            buttons[i].setOnClickListener(v -> makeDecision(idx));
        }
    }

    private void makeDecision(int index) {
        setButtonsEnabled(false);

        String result = raceEngine.applyDecision(currentDecisions[index]);
        tvLastResult.setText(result);
        speak(result);

        nextLapRunnable = () -> {
            nextLapRunnable = null;
            setButtonsEnabled(true);
            nextLap();
        };
        tvLap.postDelayed(nextLapRunnable, NEXT_LAP_DELAY_MS);
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
        // Cancel any pending lap transition to prevent callback firing on a destroyed activity
        if (nextLapRunnable != null) {
            tvLap.removeCallbacks(nextLapRunnable);
            nextLapRunnable = null;
        }
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (bannerAdView != null) bannerAdView.destroy();
        billingManager.destroy();
        super.onDestroy();
    }
}
