package com.emmetthoolahan.dicetospeech;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChampionshipManager {

    private static final String PREF_NAME      = "rsm_championship";
    private static final String KEY_RACE_COUNT = "race_count";
    private static final String KEY_POINTS_    = "pts_";

    // Fictional AI competitor names — no real driver names used
    private static final String[] AI_DRIVERS = {
        "V. Hartmann", "S. Flores", "L. Sterling", "F. Casanova", "C. Laurent",
        "L. Nash", "C. Moreno", "G. Werner", "O. Perrin", "A. Dubois"
    };

    private final SharedPreferences prefs;
    private int raceCount;
    private final Map<String, Integer> points = new HashMap<>();

    public ChampionshipManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        raceCount = prefs.getInt(KEY_RACE_COUNT, 0);
        loadPoints();
    }

    private void loadPoints() {
        for (String driver : AI_DRIVERS) {
            points.put(driver, prefs.getInt(KEY_POINTS_ + driver, 0));
        }
        points.put("YOU", prefs.getInt(KEY_POINTS_ + "YOU", 0));
    }

    public void addRaceResult(int playerPoints) {
        points.put("YOU", getPlayerPoints() + playerPoints);
        for (String driver : AI_DRIVERS) {
            int aiGain = (int) (Math.random() * 26); // 0–25 pts
            points.put(driver, points.getOrDefault(driver, 0) + aiGain);
        }
        raceCount++;
        savePoints();
    }

    private void savePoints() {
        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt(KEY_RACE_COUNT, raceCount);
        for (Map.Entry<String, Integer> e : points.entrySet()) {
            ed.putInt(KEY_POINTS_ + e.getKey(), e.getValue());
        }
        ed.apply();
    }

    public int getPlayerPoints() {
        return points.getOrDefault("YOU", 0);
    }

    public int getRaceCount() {
        return raceCount;
    }

    public Map<String, Integer> getAllPoints() {
        return Collections.unmodifiableMap(points);
    }

    public int getPlayerChampionshipPosition() {
        int playerPts = getPlayerPoints();
        int pos = 1;
        for (String driver : AI_DRIVERS) {
            if (points.getOrDefault(driver, 0) > playerPts) pos++;
        }
        return pos;
    }

    public void resetSeason() {
        prefs.edit().clear().apply();
        raceCount = 0;
        points.clear();
        loadPoints();
    }
}
