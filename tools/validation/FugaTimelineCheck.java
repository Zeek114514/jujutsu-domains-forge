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
        for(double bad:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY}) {
            boolean rejected=false;try{FugaTimeline.flight(bad);}catch(IllegalArgumentException e){rejected=true;}
            require(rejected,"reject invalid range");checks++;
        }
        require(FugaTimeline.COOLDOWN>FugaTimeline.CHARGE+12+FugaTimeline.AFTERMATH,"bounded overlap per caster");checks++;
        System.out.println("Fuga timing/bounds: "+checks+" checks PASS (no Minecraft runtime started)");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
