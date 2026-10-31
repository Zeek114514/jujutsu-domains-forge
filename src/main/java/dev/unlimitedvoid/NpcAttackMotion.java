package dev.unlimitedvoid;

/** Shared server contact frames and vanilla block-limb choreography. Angles are radians. */
public final class NpcAttackMotion {
    private NpcAttackMotion(){}
    public static int contact(int step,int kind){return kind>=3?7:step==2?6:step==1?5:4;}
    public static int duration(int step,int kind){return kind>=3||step==2?16:12;}
    public record Pose(float left,float right,float twist,float lean,float step,float guard){}
    private static final Pose REST=new Pose(0,0,0,0,0,0);
    private static float ease(float x){x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);}
    public static Pose pose(boolean gojo,int step,int kind,float age){
        int hit=contact(step,kind),end=duration(step,kind);
        if(age<=0||age>=end)return REST;
        float wind=ease(age/Math.max(1,hit-2)),strike=ease((age-hit+2)/2),recover=1-ease((age-hit-1)/(end-hit-1));
        float arm=(.55f*wind-2.05f*strike)*recover;
        float twist=(-.35f*wind+.85f*strike)*recover;
        boolean left=step==0&&kind<3;
        if(kind>=3){arm=(-.45f*wind-1.25f*strike)*recover;twist=(-.65f*wind+1.25f*strike)*recover;}
        if(step==2){arm*=1.08f;twist*=1.15f;}
        if(left)twist=-twist;
        return new Pose(left?arm:-.65f*wind*recover,left?-.65f*wind*recover:arm,
                twist,(gojo?-.12f:.12f)*strike*recover,.30f*strike*recover,(left?1:-1)*.12f*wind*recover);
    }
}
