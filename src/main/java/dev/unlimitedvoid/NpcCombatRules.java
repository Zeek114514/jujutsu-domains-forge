package dev.unlimitedvoid;
/** Deterministic server policy; no random decisions are made on the client. */
public final class NpcCombatRules {
    private NpcCombatRules(){}
    public static LimitlessKind choose(boolean awakened,int roll){
        if(roll<0||roll>=100)throw new IllegalArgumentException("roll");
        return !awakened||roll<50?LimitlessKind.BLUE:roll<95?LimitlessKind.RED:LimitlessKind.PURPLE;
    }
    public static boolean lastResort(float health,float maximum){return Float.isFinite(health)&&Float.isFinite(maximum)&&health>0&&maximum>0&&health<maximum*.05f;}
}
