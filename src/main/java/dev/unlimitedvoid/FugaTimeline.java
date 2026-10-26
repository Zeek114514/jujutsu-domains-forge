package dev.unlimitedvoid;

/** Pure visual timing shared by the server, renderer and headless checks. */
public final class FugaTimeline {
    public static final int CHARGE = 50, AFTERMATH = 48, COOLDOWN = 200, MAX_CASTS = 8;
    public static final double RANGE = 64;
    private FugaTimeline() {}
    public static int flight(double distance) {
        if (!Double.isFinite(distance) || distance < 0) throw new IllegalArgumentException("Invalid flight distance");
        return Math.max(1, Math.min(12, (int)Math.ceil(distance / 6)));
    }
    public static float smooth(float value) {
        float t=Math.max(0, Math.min(1,value)); return t*t*(3-2*t);
    }
    public static float charge(float age) { return smooth(age / CHARGE); }
    public static float travel(float age, int flight) { return Math.max(0,Math.min(1,(age-CHARGE)/flight)); }
    public static float burstAge(float age, int flight) { return age-CHARGE-flight; }
}
