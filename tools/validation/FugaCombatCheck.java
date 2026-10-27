package dev.unlimitedvoid;

public final class FugaCombatCheck {
    public static void main(String[] args){
        int checks=0;
        require(FugaCombatRules.damage(0,16,32)==32,"center damage");checks++;
        require(FugaCombatRules.damage(16,16,32)==8,"edge falloff");checks++;
        require(FugaCombatRules.damage(16.01,16,32)==0,"outside");checks++;
        for(double bad:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY}){require(FugaCombatRules.damage(bad,16,32)==0,"invalid distance");checks++;}
        require(FugaCombatRules.damage(1,0,32)==0&&FugaCombatRules.damage(1,16,0)==0,"disabled dimensions");checks++;
        double previous=Double.MAX_VALUE;
        for(int i=0;i<=160;i++){float value=FugaCombatRules.damage(i/10.,16,32);require(value<=previous&&value>=8,"monotonic falloff");previous=value;}checks++;
        require(FugaCombatRules.front(0,200)==0&&FugaCombatRules.front(32,200)==200,"powder wave timing");checks++;
        require(!FugaCombatRules.hitWindow(-1)&&FugaCombatRules.hitWindow(0)&&!FugaCombatRules.hitWindow(FugaTimeline.AFTERMATH),"single bounded damage window");checks++;
        for(int casts=1;casts<=8;casts++)for(double distance:new double[]{0,200*200,500*500})for(int particles=0;particles<3;particles++){
            int stride=FugaRenderBudget.stride(casts,distance,particles);require(stride>=1&&stride<=16,"bounded LOD");checks++;
        }
        require(FugaRenderBudget.stride(8,0,0)>FugaRenderBudget.stride(1,0,0),"multiple blasts reduce detail");checks++;
        System.out.println("Fuga combat/render rules: "+checks+" checks PASS");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
