package com.emmetthoolahan.dicetospeech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TeamSelectActivity extends AppCompatActivity {

    public static final String EXTRA_TEAM_INDEX = "team_index";

    private BillingManager billingManager;
    private Team[] teams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_select);

        billingManager = new BillingManager(this, new BillingManager.PurchaseListener() {
            @Override
            public void onPremiumTeamsUnlocked() {
                runOnUiThread(() -> {
                    Toast.makeText(TeamSelectActivity.this,
                            "Premium teams unlocked!", Toast.LENGTH_LONG).show();
                    buildTeamList();
                });
            }
            @Override public void onSeasonPassUnlocked() {}
        });

        teams = TeamData.getAllTeams();
        buildTeamList();
    }

    private void buildTeamList() {
        LinearLayout container = findViewById(R.id.ll_teams);
        container.removeAllViews();

        for (int i = 0; i < teams.length; i++) {
            final int index = i;
            Team team = teams[i];
            boolean locked = team.isPremium && !billingManager.isPremiumTeamsUnlocked();

            Button btn = new Button(this);
            btn.setTextSize(15);
            btn.setTextColor(Color.WHITE);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(24, 0, 24, 0);
            btn.setBackgroundColor(locked ? 0xFF2A2A2A : team.color);

            String label = (locked ? "\uD83D\uDD12 " : "") + team.name
                    + " (" + team.country + ")"
                    + (locked ? "  \u2014 Unlock Premium Pack" : "  \u2605 " + team.carRating + "/10");
            btn.setText(label);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 130);
            params.setMargins(0, 10, 0, 10);
            btn.setLayoutParams(params);

            if (locked) {
                btn.setOnClickListener(v ->
                        billingManager.launchPurchase(this, BillingManager.PRODUCT_PREMIUM_TEAMS));
            } else {
                btn.setOnClickListener(v -> {
                    Intent intent = new Intent(this, GameActivity.class);
                    intent.putExtra(EXTRA_TEAM_INDEX, index);
                    startActivity(intent);
                });
            }

            container.addView(btn);
        }
    }

    @Override
    protected void onDestroy() {
        billingManager.destroy();
        super.onDestroy();
    }
}
