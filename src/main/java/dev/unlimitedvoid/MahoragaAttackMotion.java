package dev.unlimitedvoid;

/** Shared contact frames and continuous anticipation/contact/follow-through poses. */
public final class MahoragaAttackMotion {
    public static final int DURATION=30;
    public record Arm(float pitch,float yaw,float roll,float elbow) {}
    public record Leg(float hip,float knee) {}
    public record Pose(float yaw,float lean,Arm left,Arm right,Leg leftLeg,Leg rightLeg,float step,float head) {}
    private record Key(float time,Pose pose) {}
    private static Arm a(float x,float y,float z,float bend){return new Arm(x,y,z,bend);}
    private static Pose p(float yaw,float lean,Arm left,Arm right,float knee,float step){return new Pose(yaw,lean,left,right,new Leg(-knee*.55f,knee),new Leg(-knee*.45f,knee*.9f),step,-lean*.35f);}
    private static final Pose REST=p(0,0,a(0,0,-.1f,0),a(0,0,.1f,0),0,0);
    private static Key k(float time,Pose pose){return new Key(time,pose);}
    private static final Key[][] MOVES={ {},
        {k(0,REST),k(6,p(-.35f,-.07f,a(-.6f,-.35f,-.12f,-1.15f),a(-.65f,.1f,.2f,.6f),.55f,1.4f)),
         k(10,p(.32f,.20f,a(-1.48f,.06f,-.08f,-.04f),a(-.55f,-.18f,.2f,.45f),.45f,2.2f)),
         k(13,p(.42f,.24f,a(-1.35f,.13f,-.1f,-.08f),a(-.55f,-.18f,.2f,.45f),.5f,2.3f)),
         k(20,p(.1f,.04f,a(-.8f,.05f,-.12f,-.8f),a(-.4f,0,.15f,.3f),.3f,1)),k(30,REST)},
        {k(0,REST),k(8,p(-.6f,-.1f,a(-.7f,.25f,-.5f,-.9f),a(-2.15f,-.45f,.65f,-.7f),.7f,1.7f)),
         k(11,p(-.7f,-.06f,a(-.7f,.25f,-.5f,-.9f),a(-2.35f,-.6f,.85f,-.5f),.7f,1.7f)),
         k(14,p(.55f,.28f,a(-.5f,-.25f,-.5f,-.9f),a(-1.1f,.7f,-.4f,-.1f),.65f,2.5f)),
         k(18,p(.75f,.32f,a(-.4f,-.25f,-.45f,-.7f),a(-.55f,.95f,-.6f,-.3f),.7f,2.7f)),
         k(24,p(.18f,.12f,a(-.3f,0,-.1f,-.3f),a(-.5f,.3f,.2f,-.6f),.4f,1)),k(30,REST)},
        {k(0,p(0,.1f,a(-.6f,0,-.15f,-.65f),a(-.6f,0,.15f,-.65f),.85f,0)),
         k(7,p(-.12f,-.15f,a(-2.7f,.12f,-.35f,-.5f),a(-2.85f,-.1f,.35f,-.5f),2.0f,0)),
         k(13,p(-.18f,-.2f,a(-2.8f,.12f,-.35f,-.2f),a(-2.95f,-.1f,.35f,-.15f),1.6f,0)),
         k(18,p(.12f,.4f,a(-.9f,-.1f,-.35f,-.2f),a(-.3f,.15f,.2f,-.1f),1.15f,0)),
         k(21,p(.15f,.45f,a(-.5f,-.1f,-.35f,-.3f),a(.05f,.2f,.2f,-.15f),1.2f,0)),
         k(26,p(.04f,.15f,a(-.4f,0,-.15f,-.3f),a(-.35f,0,.15f,-.4f),.65f,0)),k(30,REST)},
        {k(0,REST),k(9,p(-.3f,-.04f,a(-.9f,.2f,-.2f,-.9f),a(-1.7f,-.5f,.45f,-.7f),.35f,1)),
         k(15,p(-.15f,.02f,a(-.9f,.2f,-.2f,-.9f),a(-1.9f,-.2f,.2f,-.2f),.4f,1.2f)),
         k(18,p(.35f,.18f,a(-.6f,-.2f,-.3f,-.8f),a(-1.4f,.65f,-.25f,0),.45f,1.8f)),
         k(22,p(.45f,.2f,a(-.5f,-.2f,-.3f,-.7f),a(-1,.85f,-.4f,-.1f),.45f,1.8f)),k(30,REST)}
    };
    public static int contact(int type){return type==1?10:type==2?14:18;}
    private static float mix(float a,float b,float t){return a+(b-a)*t;}
    private static Arm mix(Arm a,Arm b,float t){return new Arm(mix(a.pitch,b.pitch,t),mix(a.yaw,b.yaw,t),mix(a.roll,b.roll,t),mix(a.elbow,b.elbow,t));}
    private static Leg mix(Leg a,Leg b,float t){return new Leg(mix(a.hip,b.hip,t),mix(a.knee,b.knee,t));}
    private static Pose mix(Pose a,Pose b,float t){return new Pose(mix(a.yaw,b.yaw,t),mix(a.lean,b.lean,t),mix(a.left,b.left,t),mix(a.right,b.right,t),mix(a.leftLeg,b.leftLeg,t),mix(a.rightLeg,b.rightLeg,t),mix(a.step,b.step,t),mix(a.head,b.head,t));}
    public static Pose sample(int type,float age){
        if(type<1||type>4||!Float.isFinite(age)||age>=DURATION)return REST;
        Key[] keys=MOVES[type];if(age<=0)return keys[0].pose;
        for(int i=1;i<keys.length;i++)if(age<=keys[i].time){float t=(age-keys[i-1].time)/(keys[i].time-keys[i-1].time);return mix(keys[i-1].pose,keys[i].pose,t*t*(3-2*t));}
        return REST;
    }
    private MahoragaAttackMotion(){}
}
