package dev.unlimitedvoid;
/** Four gradual adaptations, independent of Minecraft rendering or event ordering. */
public final class MahoragaRules {
    public enum Kind { PHYSICAL,BLUE,RED,SLASH,FIRE,PURPLE,INFINITY,VOID }
    public static int stage(int packed,Kind kind){return Math.min(4,(packed>>>(kind.ordinal()*3))&7);}
    public static int setStage(int packed,Kind kind,int stage){int shift=kind.ordinal()*3;return packed&~(7<<shift)|Math.max(0,Math.min(4,stage))<<shift;}
    public static float damageFactor(int stage){return switch(Math.max(0,Math.min(4,stage))){case 1->.75f;case 2->.5f;case 3->.25f;case 4->.1f;default->1;};}
    public static double forceFactor(int stage){return switch(Math.max(0,Math.min(4,stage))){case 1->.7;case 2->.4;case 3->.15;case 4->0;default->1;};}
    public static boolean advance(Kind kind,int stage,int exposures,long elapsed,long sinceStep){return stage>=0&&stage<4&&exposures>=(stage+1)*3&&elapsed>=(stage+1)*(kind==Kind.PURPLE?160L:60L)&&sinceStep>=40;}
    public static int adaptations(int packed){int count=0;for(var kind:Kind.values())count+=stage(packed,kind);return count;}
    public static float growth(int packed){return 1+adaptations(packed)*.05f;}
    private MahoragaRules(){}
}
