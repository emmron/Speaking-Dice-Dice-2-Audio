package com.emmetthoolahan.dicetospeech;

import android.graphics.Color;

public class TeamData {
    public static Team[] getAllTeams() {
        return new Team[]{
            // Free teams
            new Team("Williams",     "British",  Color.parseColor("#005AFF"), 4, false),
            new Team("Haas",         "American", Color.parseColor("#B6BABD"), 5, false),
            new Team("Sauber",       "Swiss",    Color.parseColor("#00E701"), 5, false),
            // Premium teams
            new Team("Alpine",       "French",   Color.parseColor("#FF87BC"), 6, true),
            new Team("Aston Martin", "British",  Color.parseColor("#006F62"), 7, true),
            new Team("RB",           "Italian",  Color.parseColor("#1434CB"), 6, true),
            new Team("McLaren",      "British",  Color.parseColor("#FF8000"), 8, true),
            new Team("Mercedes",     "German",   Color.parseColor("#27F4D2"), 8, true),
            new Team("Ferrari",      "Italian",  Color.parseColor("#E8002D"), 9, true),
            new Team("Red Bull",     "Austrian", Color.parseColor("#3671C6"), 10, true),
        };
    }
}
