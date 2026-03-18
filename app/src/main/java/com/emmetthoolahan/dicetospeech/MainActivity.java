package com.emmetthoolahan.dicetospeech;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private BillingManager billingManager;
    private Button btnRemoveAds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, status -> {});

        btnRemoveAds = findViewById(R.id.btn_remove_ads);

        billingManager = new BillingManager(this, new BillingManager.PurchaseListener() {
            @Override
            public void onAdsRemoved() {
                runOnUiThread(() -> btnRemoveAds.setVisibility(View.GONE));
            }
            @Override
            public void onPremiumTeamsUnlocked() {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "Premium teams unlocked! All 10 constructors available.", Toast.LENGTH_LONG).show());
            }
            @Override
            public void onSeasonPassUnlocked() {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "Season pass unlocked! Full 10-lap races enabled.", Toast.LENGTH_LONG).show());
            }
        });

        if (billingManager.isAdsRemoved()) {
            btnRemoveAds.setVisibility(View.GONE);
        }
    }

    public void onPlayClicked(View view) {
        startActivity(new Intent(this, TeamSelectActivity.class));
    }

    public void onChampionshipClicked(View view) {
        ChampionshipManager cm = new ChampionshipManager(this);
        Map<String, Integer> allPoints = cm.getAllPoints();

        // Sort by points descending
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(allPoints.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());

        StringBuilder sb = new StringBuilder();
        int pos = 1;
        for (Map.Entry<String, Integer> e : entries) {
            sb.append("P").append(pos++).append("  ")
              .append(e.getKey()).append(" — ").append(e.getValue()).append(" pts\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Championship Standings")
                .setMessage(sb.length() > 0 ? sb.toString() : "No races completed yet.")
                .setPositiveButton("OK", null)
                .show();
    }

    public void onRemoveAdsClicked(View view) {
        billingManager.launchPurchase(this, BillingManager.PRODUCT_REMOVE_ADS);
    }

    @Override
    protected void onDestroy() {
        billingManager.destroy();
        super.onDestroy();
    }
}
