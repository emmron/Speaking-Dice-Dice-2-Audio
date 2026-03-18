package com.emmetthoolahan.dicetospeech;

public class Team {
    public final String name;
    public final String country;
    public final int color;       // Android color int
    public final int carRating;   // 1-10, affects base performance
    public final boolean isPremium;

    public Team(String name, String country, int color, int carRating, boolean isPremium) {
        this.name = name;
        this.country = country;
        this.color = color;
        this.carRating = carRating;
        this.isPremium = isPremium;
    }
}
