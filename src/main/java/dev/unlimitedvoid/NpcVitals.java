package dev.unlimitedvoid;
/** Default health stays inside the vanilla max-health attribute range. */
public final class NpcVitals {
    public static final double STUDENT=400,AWAKENED=800,SHINJUKU=1000,SUKUNA=800,MEGUMI=900,TRUE_SUKUNA=1000;
    private NpcVitals(){}
    public static double maximum(boolean gojo,boolean ascended,boolean shinjuku,boolean transformed){return gojo?(shinjuku?SHINJUKU:ascended?AWAKENED:STUDENT):(transformed?TRUE_SUKUNA:SUKUNA);}
    public static float preserved(float health,float oldMax,float newMax){if(!Float.isFinite(health)||!Float.isFinite(oldMax)||oldMax<=0)return newMax;return Math.max(0,Math.min(newMax,health/oldMax*newMax));}
}
