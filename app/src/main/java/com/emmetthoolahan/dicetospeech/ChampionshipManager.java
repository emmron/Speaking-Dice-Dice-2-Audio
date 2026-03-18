package com.emmetthoolahan.dicetospeech;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class ChampionshipManager {

    private static final String PREF_NAME      = "f1_championship";
    private static final String KEY_RACE_COUNT = "race_count";
    private static final String KEY_POINTS_    = "pts_";

    // AI competitors seeded with realistic starting points
    private static final String[] AI_DRIVERS = {
        "Verstappen", "Perez", "Hamilton", "Alonso", "Leclerc",
        "Norris", "Sainz", "Russell", "Piastri", "Stroll"
    };
    private static final int[] AI_BASE_POINTS = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    private final SharedPreferences prefs;
    private int raceCount;
    private final Map<String, Integer> points = new HashMap<>();

    public ChampionshipManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        raceCount = prefs.getInt(KEY_RACE_COUNT, 0);
        loadPoints();
    }

    private void loadPoints() {
        for (int i = 0; i < AI_DRIVERS.length; i++) {
            points.put(AI_DRIVERS[i], prefs.getInt(KEY_POINTS_ + AI_DRIVERS[i], AI_BASE_POINTS[i]));
        }
        points.put("YOU", prefs.getInt(KEY_POINTS_ + "YOU", 0));
    }

    /** Call after each race to record results. */
    public void addRaceResult(int playerPoints) {
        points.put("YOU", getPlayerPoints() + playerPoints);
        // Simulate AI earning points each race
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
        return new HashMap<>(points);
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
