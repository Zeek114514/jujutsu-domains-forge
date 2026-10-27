package dev.unlimitedvoid;

/** Deterministic combat limits shared with the regression checks. */
public final class FugaCombatRules {
    public static final int DUST_REQUIRED=100, TARGET_LIMIT=512, HIT_BUDGET=32;
    private FugaCombatRules(){}
    public static double front(float burst,double radius){return radius*FugaTimeline.smooth(burst/32);}
    public static float damage(double distance,double radius,double maximum){
        if(!Double.isFinite(distance)||!Double.isFinite(radius)||!Double.isFinite(maximum)||distance<0||radius<=0||maximum<=0||distance>radius)return 0;
        return (float)(maximum*(1-.75*Math.min(1,distance/radius)));
    }
    public static boolean hitWindow(float burst){return burst>=0&&burst<FugaTimeline.AFTERMATH;}
}
