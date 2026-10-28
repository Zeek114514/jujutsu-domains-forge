package dev.unlimitedvoid;
/** Rolls happen at most once per two seconds of an engaged battle, never per frame. */
public final class NpcBattleRules {
    public static final int INITIAL_DELAY=200,ROLL_INTERVAL=40;
    private static void valid(int roll){if(roll<0||roll>=1000)throw new IllegalArgumentException("roll");}
    public static boolean domain(int roll){valid(roll);return roll<15;}
    public static boolean fuga(int roll){valid(roll);return roll<3;}
    public static boolean mahoraga(int roll){valid(roll);return roll<10;}
    private NpcBattleRules(){}
}
