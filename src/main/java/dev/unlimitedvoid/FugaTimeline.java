package dev.unlimitedvoid;

/** Shared authoritative cast timing, rendering bounds and regression-check constants. */
public final class FugaTimeline {
    // Selected source 5.00..7.75s: full draw before the Fuga syllables, release after the line.
    public static final int FULL_DRAW = 30, CHARGE = 54, AFTERMATH = 132, COOLDOWN = 200, MAX_CASTS = 8;
    public static final double RANGE = 64;
    public static final double FIRE_RADIUS = 200, COLUMN_HEIGHT = 240, SHOCK_RADIUS = 300, VIEW_RANGE = 768;
    public static final float BOW_SCALE = 3;
    public static final double CHARGE_RADIUS = 18;
    private FugaTimeline() {}
    public static int flight(double distance) {
        if (!Double.isFinite(distance) || distance < 0) throw new IllegalArgumentException("Invalid flight distance");
        return Math.max(1, Math.min(12, (int)Math.ceil(distance / 6)));
    }
    public static float smooth(float value) {
        float t=Math.max(0, Math.min(1,value)); return t*t*(3-2*t);
    }
    public static float charge(float age) { return smooth(age / FULL_DRAW); }
    public static float travel(float age, int flight) { return Math.max(0,Math.min(1,(age-CHARGE)/flight)); }
    public static float burstAge(float age, int flight) { return age-CHARGE-flight; }
    public static double fireRadius(float burst) { return FIRE_RADIUS*smooth(burst/32); }
    public static double shockRadius(float burst) { return SHOCK_RADIUS*smooth(burst/44); }
    public static float burstFade(float burst) { return 1-smooth((burst-70)/(AFTERMATH-70)); }
}
