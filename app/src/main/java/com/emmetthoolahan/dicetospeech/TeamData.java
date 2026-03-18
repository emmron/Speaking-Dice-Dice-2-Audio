package com.emmetthoolahan.dicetospeech;

import android.graphics.Color;

public class TeamData {
    public static Team[] getAllTeams() {
        return new Team[]{
            // Free teams
            new Team("Whitmore Racing",  "British",   Color.parseColor("#005AFF"), 4, false),
            new Team("Hawk Motorsport",  "American",  Color.parseColor("#B6BABD"), 5, false),
            new Team("Saxe Racing",      "Swiss",     Color.parseColor("#00C853"), 5, false),
            // Premium teams
            new Team("Cerulean Racing",  "French",    Color.parseColor("#FF87BC"), 6, true),
            new Team("Emerald Works",    "British",   Color.parseColor("#006F62"), 7, true),
            new Team("Toro Veloce",      "Italian",   Color.parseColor("#1434CB"), 6, true),
            new Team("Ocelot Racing",    "British",   Color.parseColor("#FF8000"), 8, true),
            new Team("Argent Racing",    "German",    Color.parseColor("#27F4D2"), 8, true),
            new Team("Scarlatti Racing", "Italian",   Color.parseColor("#E8002D"), 9, true),
            new Team("Pinnacle Racing",  "Austrian",  Color.parseColor("#3671C6"), 10, true),
        };
    }
}
