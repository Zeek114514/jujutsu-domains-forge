package dev.unlimitedvoid;
/** Close combat policy is independent of the rare ranged-technique roll. */
public final class NpcMeleeRules {
    public static final double REACH=2.3;
    public static final int INTERVAL=12,WINDUP=3,ANIMATION=10,REMOTE_INTERVAL=160;
    private NpcMeleeRules(){}
    public static boolean preferPunch(int roll){if(roll<0||roll>=100)throw new IllegalArgumentException("roll");return roll<80;}
    public static LimitlessKind punch(boolean advanced,int roll){if(roll<0||roll>=100)throw new IllegalArgumentException("roll");return !advanced||roll<70?LimitlessKind.BLUE:LimitlessKind.RED;}
    public static float damage(LimitlessKind kind){return kind==LimitlessKind.RED?28:18;}
}
