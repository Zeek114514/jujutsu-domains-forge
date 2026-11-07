package dev.unlimitedvoid;
public final class ProgressionRulesCheck {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        check(ProgressionRules.level(-100)==1&&ProgressionRules.level(Integer.MAX_VALUE)==20,"bounded imported saves");
        for(int xp=0;xp<=800;xp++){
            check(ProgressionRules.level(xp)>=1&&ProgressionRules.level(xp)<=20,"bounded level");
            check(ProgressionRules.maximum(xp)>=100&&ProgressionRules.maximum(xp)<=195,"bounded energy");
            check(!ProgressionRules.unlocked(0,xp,0),"unawakened players cannot cast");
            for(int ability=0;ability<6;ability++){
                int path=ability<3||ability==4?1:2;
                check(!ProgressionRules.unlocked(3-path,xp,ability),"opposite path cannot use technique");
                check(ProgressionRules.cost(ability)>0&&ProgressionRules.cost(ability)<=ProgressionRules.maximum(xp),"costs fit available energy pool");
            }
        }
        check(!ProgressionRules.unlocked(1,119,1)&&ProgressionRules.unlocked(1,120,1),"red unlock boundary");
        check(!ProgressionRules.unlocked(1,279,2)&&ProgressionRules.unlocked(1,280,2),"purple unlock boundary");
        check(!ProgressionRules.unlocked(1,439,4)&&ProgressionRules.unlocked(1,440,4),"void unlock boundary");
        check(!ProgressionRules.unlocked(2,279,5)&&ProgressionRules.unlocked(2,280,5),"shrine unlock boundary");
        check(!ProgressionRules.unlocked(1,100000,6),"unknown ability denied");
        System.out.println("Progression rules: "+checks+" checks PASS");
    }
}
