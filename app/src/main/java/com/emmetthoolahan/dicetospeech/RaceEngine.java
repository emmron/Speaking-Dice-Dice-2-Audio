package com.emmetthoolahan.dicetospeech;

import java.util.Random;

public class RaceEngine {

    public enum TireCompound { SOFT, MEDIUM, HARD, WET }
    public enum EngineMode   { NORMAL, PUSH, SAVE }
    public enum Event        { NORMAL, SAFETY_CAR, RAIN, DRS_ZONE, TIRE_WARNING, ENGINE_ISSUE }
    public enum Decision     { PIT_SOFT, PIT_MEDIUM, PIT_HARD, PIT_WET, STAY_PUSH, STAY_SAVE, STAY_NORMAL }

    private static final Random random = new Random();

    public int position;
    public final int totalLaps;
    public int currentLap;
    public int tireWear;        // 0–100 (100 = blown)
    public TireCompound tireCompound;
    public EngineMode engineMode;
    public boolean isRaining;
    public boolean dnf;
    public Event currentEvent;
    public int gapAhead;        // tenths of a second
    public final Team team;

    public RaceEngine(Team team, int totalLaps) {
        this.team = team;
        this.totalLaps = totalLaps;
        this.currentLap = 1;
        this.tireWear = 0;
        this.tireCompound = TireCompound.MEDIUM;
        this.engineMode = EngineMode.NORMAL;
        this.isRaining = false;
        this.dnf = false;
        this.gapAhead = random.nextInt(30) + 5;
        // Starting position: better car rating = better grid slot + small random variance
        this.position = Math.max(1, Math.min(20, 21 - team.carRating + random.nextInt(5) - 2));
    }

    public Event generateEvent() {
        int roll = random.nextInt(10);
        if (isRaining) {
            if (roll <= 1)      currentEvent = Event.TIRE_WARNING;
            else if (roll <= 3) currentEvent = Event.SAFETY_CAR;
            else                currentEvent = Event.NORMAL;
        } else {
            switch (roll) {
                case 0:            currentEvent = Event.ENGINE_ISSUE;  break;
                case 1: case 2:    currentEvent = Event.SAFETY_CAR;    break;
                case 3:            currentEvent = Event.RAIN;           break;
                case 4: case 5:    currentEvent = Event.DRS_ZONE;      break;
                case 6:            currentEvent = Event.TIRE_WARNING;   break;
                default:           currentEvent = Event.NORMAL;         break;
            }
        }
        return currentEvent;
    }

    /** Applies the player's decision and returns a human-readable result string. */
    public String applyDecision(Decision decision) {
        StringBuilder result = new StringBuilder();
        int positionChange = 0;

        // Base tire wear this lap
        int tireWearThisLap;
        switch (tireCompound) {
            case SOFT:   tireWearThisLap = 12 + random.nextInt(5); break;
            case MEDIUM: tireWearThisLap = 7  + random.nextInt(4); break;
            case HARD:   tireWearThisLap = 4  + random.nextInt(3); break;
            case WET:    tireWearThisLap = isRaining ? 5 + random.nextInt(3) : 15 + random.nextInt(5); break;
            default:     tireWearThisLap = 7;
        }

        // Handle pit decisions
        switch (decision) {
            case PIT_SOFT:
                tireCompound = TireCompound.SOFT;
                tireWear = 0;
                positionChange = 2 + random.nextInt(3); // lose positions in pit lane
                result.append("Pitted for SOFT tires. Lost ").append(positionChange).append(" positions in pit lane.");
                break;

            case PIT_MEDIUM:
                tireCompound = TireCompound.MEDIUM;
                tireWear = 0;
                positionChange = 2 + random.nextInt(2);
                result.append("Pitted for MEDIUM tires.");
                break;

            case PIT_HARD:
                tireCompound = TireCompound.HARD;
                tireWear = 0;
                positionChange = 1 + random.nextInt(2);
                result.append("Pitted for HARD tires. Minimal time loss.");
                break;

            case PIT_WET:
                tireCompound = TireCompound.WET;
                tireWear = 0;
                positionChange = 1;
                if (isRaining) result.append("Smart call — wet tires in the rain!");
                else           result.append("Gamble on wets. Risky if it dries up!");
                break;

            case STAY_PUSH:
                engineMode = EngineMode.PUSH;
                positionChange = -(1 + random.nextInt(2)); // gain positions
                tireWearThisLap += 5;
                if (currentEvent == Event.ENGINE_ISSUE && random.nextInt(4) == 0) {
                    dnf = true;
                    currentLap++;
                    return "ENGINE FAILURE! DNF. The engine gave out under pressure!";
                }
                result.append("Pushed hard! Gained ").append(Math.abs(positionChange)).append(" position(s).");
                break;

            case STAY_SAVE:
                engineMode = EngineMode.SAVE;
                positionChange = random.nextInt(2); // may lose one
                tireWearThisLap -= 2;
                result.append("Saving tires and engine. Managed the gap.");
                break;

            case STAY_NORMAL:
            default:
                engineMode = EngineMode.NORMAL;
                positionChange = random.nextInt(3) - 1; // -1, 0, or +1
                result.append("Maintained position on current tires.");
                break;
        }

        // Event modifiers
        boolean isPitting = decision == Decision.PIT_SOFT
                || decision == Decision.PIT_MEDIUM
                || decision == Decision.PIT_HARD
                || decision == Decision.PIT_WET;

        if (currentEvent == Event.SAFETY_CAR && isPitting) {
            // Free pit under safety car — undo some position loss
            positionChange -= 2;
            result.append(" Safety car made your pit FREE — gained positions!");
            tireWear = 0;
        }

        if (currentEvent == Event.RAIN && tireCompound != TireCompound.WET) {
            tireWearThisLap += 8;
            positionChange += 1;
            isRaining = true;
            result.append(" Rain on slicks — high tire stress!");
        }

        if (currentEvent == Event.RAIN && decision == Decision.PIT_WET) {
            isRaining = true;
        }

        if (currentEvent == Event.TIRE_WARNING && tireWear >= 80) {
            if (random.nextInt(3) == 0) {
                dnf = true;
                currentLap++;
                return "TIRE BLOWOUT! Car beached in the gravel. DNF!";
            }
        }

        if (currentEvent == Event.DRS_ZONE && decision == Decision.STAY_PUSH) {
            positionChange -= 1; // extra position gain
            result.append(" DRS activated! Extra position gained.");
        }

        // Apply state changes
        tireWear = Math.min(100, tireWear + tireWearThisLap);
        position = Math.max(1, Math.min(20, position + positionChange));
        gapAhead = (positionChange < 0) ? 0 : Math.max(0, gapAhead + random.nextInt(10) - 3);

        currentLap++;
        return result.toString();
    }

    public boolean isRaceOver() {
        return currentLap > totalLaps || dnf;
    }

    /** Returns championship points based on finish position. */
    public int getChampionshipPoints() {
        int[] pointsTable = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
        if (dnf || position > 10) return 0;
        return pointsTable[position - 1];
    }

    public String getTireStatus() {
        String wear;
        if      (tireWear < 30) wear = "FRESH";
        else if (tireWear < 60) wear = "USED";
        else if (tireWear < 80) wear = "WORN";
        else                    wear = "CRITICAL";
        return tireCompound.name() + " — " + wear + " (" + tireWear + "%)";
    }

    public String getEventAnnouncement() {
        switch (currentEvent) {
            case SAFETY_CAR:   return "Safety car deployed! Free pit window is open!";
            case RAIN:         return "Rain starting! Pit for wet tires or stay on slicks?";
            case DRS_ZONE:     return "DRS zone ahead! Overtake opportunity!";
            case TIRE_WARNING: return "Tire wear warning! Tires degrading fast!";
            case ENGINE_ISSUE: return "Engine temperature rising! Push through or save it?";
            default:
                return isRaining
                    ? "Lap " + currentLap + " in wet conditions."
                    : "Lap " + currentLap + ". Racing normally. Stay focused.";
        }
    }
}
