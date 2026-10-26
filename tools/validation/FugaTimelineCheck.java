package dev.unlimitedvoid;

/** No client/world startup; protects timing and bounds of the pure visual cast. */
public final class FugaTimelineCheck {
    public static void main(String[] args) {
        int checks=0;
        for(double range:new double[]{0,.1,1,6,12,32,64}) {
            int flight=FugaTimeline.flight(range);
            require(flight>=1&&flight<=12,"bounded flight");checks++;
            require(FugaTimeline.travel(FugaTimeline.CHARGE,flight)==0,"release begins at origin");checks++;
            require(FugaTimeline.travel(FugaTimeline.CHARGE+flight,flight)==1,"flight reaches endpoint");checks++;
            float prior=-1;
            for(int i=0;i<=400;i++) {
                float age=i*.25f,travel=FugaTimeline.travel(age,flight);
                require(Float.isFinite(travel)&&travel>=prior&&travel>=0&&travel<=1,"continuous monotonic travel");prior=travel;
            }checks++;
            require(FugaTimeline.burstAge(FugaTimeline.CHARGE+flight,flight)==0,"one impact boundary");checks++;
        }
        require(FugaTimeline.charge(0)==0&&FugaTimeline.charge(FugaTimeline.CHARGE)==1,"charge endpoints");checks++;
        require(FugaTimeline.charge(-1)==0&&FugaTimeline.charge(1000)==1,"clamped charge");checks++;
        require(FugaTimeline.FULL_DRAW<FugaTimeline.CHARGE&&FugaTimeline.charge(FugaTimeline.FULL_DRAW)==1,"full draw precedes release during spoken cue");checks++;
        for(int t=FugaTimeline.FULL_DRAW;t<=FugaTimeline.CHARGE;t++)require(FugaTimeline.charge(t)==1&&FugaTimeline.travel(t,12)==0,"hold full draw throughout Fuga before launch");checks++;
        for(double bad:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY}) {
            boolean rejected=false;try{FugaTimeline.flight(bad);}catch(IllegalArgumentException e){rejected=true;}
            require(rejected,"reject invalid range");checks++;
        }
        require(FugaTimeline.COOLDOWN>FugaTimeline.CHARGE+12+FugaTimeline.AFTERMATH,"bounded overlap per caster");checks++;
        require(FugaTimeline.FIRE_RADIUS>=200&&FugaTimeline.COLUMN_HEIGHT>=240,"district scale footprint and height");checks++;
        require(FugaTimeline.SHOCK_RADIUS>FugaTimeline.FIRE_RADIUS&&FugaTimeline.VIEW_RANGE>FugaTimeline.SHOCK_RADIUS+FugaTimeline.RANGE,"far observer visibility includes full wave");checks++;
        require(FugaTimeline.fireRadius(0)==0&&FugaTimeline.fireRadius(32)==FugaTimeline.FIRE_RADIUS,"fire front endpoints");checks++;
        require(FugaTimeline.shockRadius(0)==0&&FugaTimeline.shockRadius(44)==FugaTimeline.SHOCK_RADIUS,"shock front endpoints");checks++;
        require(FugaTimeline.burstFade(70)==1&&FugaTimeline.burstFade(FugaTimeline.AFTERMATH)==0,"long burn fades before cleanup");checks++;
        double fire=-1,shock=-1;
        for(int i=0;i<=600;i++){
            float age=i*.25f;
            double f=FugaTimeline.fireRadius(age),s=FugaTimeline.shockRadius(age);
            require(Double.isFinite(f)&&f>=fire&&f<=FugaTimeline.FIRE_RADIUS,"bounded continuous fire front");
            require(Double.isFinite(s)&&s>=shock&&s<=FugaTimeline.SHOCK_RADIUS,"bounded continuous shock front");fire=f;shock=s;
        }checks++;
        System.out.println("Fuga timing/bounds: "+checks+" checks PASS (no Minecraft runtime started)");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
