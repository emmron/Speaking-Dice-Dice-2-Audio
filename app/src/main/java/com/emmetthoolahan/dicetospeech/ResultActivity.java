package com.emmetthoolahan.dicetospeech;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_POSITION  = "finish_position";
    public static final String EXTRA_POINTS    = "points_earned";
    public static final String EXTRA_DNF       = "dnf";
    public static final String EXTRA_TEAM_NAME = "team_name";

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        int     position = getIntent().getIntExtra(EXTRA_POSITION, 20);
        int     pts      = getIntent().getIntExtra(EXTRA_POINTS, 0);
        boolean dnf      = getIntent().getBooleanExtra(EXTRA_DNF, false);

        ChampionshipManager cm = new ChampionshipManager(this);
        cm.addRaceResult(pts);

        TextView tvMedal     = findViewById(R.id.tv_medal);
        TextView tvFinishPos = findViewById(R.id.tv_finish_position);
        TextView tvPoints    = findViewById(R.id.tv_points_earned);
        TextView tvTotal     = findViewById(R.id.tv_total_points);
        TextView tvChampPos  = findViewById(R.id.tv_championship_pos);

        if (dnf) {
            tvFinishPos.setText("DNF");
            tvFinishPos.setTextColor(0xFFF44336);
        } else {
            tvFinishPos.setText("P" + position);
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
    }

    private String podiumMedal(int position) {
        switch (position) {
            case 1: return "\uD83E\uDD47";
            case 2: return "\uD83E\uDD48";
            case 3: return "\uD83E\uDD49";
            default: return "";
        }
    }

    public void onNextRaceClicked(View view) {
        startActivity(new Intent(this, TeamSelectActivity.class));
        finish();
    }

    public void onMainMenuClicked(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
