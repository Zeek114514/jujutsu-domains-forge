package dev.unlimitedvoid;
public final class NpcCombatCheck {
    static int checks;
    static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    public static void main(String[] args){
        int[] counts=new int[3];
        for(int roll=0;roll<100;roll++){check(NpcCombatRules.choose(false,roll)==LimitlessKind.BLUE,"student never rolls red/purple");counts[NpcCombatRules.choose(true,roll).ordinal()]++;}
        check(counts[0]==50&&counts[1]==45&&counts[2]==5,"advanced exact 50/45/5 distribution");
        for(float max:new float[]{40,100,200,1000}){check(!NpcCombatRules.lastResort(max*.05f,max),"strictly below 5 percent");check(NpcCombatRules.lastResort(max*.049f,max),"under threshold");check(!NpcCombatRules.lastResort(max,max),"healthy");check(!NpcCombatRules.lastResort(0,max),"dead");}
        check(!NpcCombatRules.lastResort(Float.NaN,40),"NaN rejected");check(!NpcCombatRules.lastResort(1,Float.POSITIVE_INFINITY),"infinite max rejected");check(!NpcCombatRules.lastResort(1,0),"invalid max rejected");
        try{NpcCombatRules.choose(true,100);throw new AssertionError("invalid roll");}catch(IllegalArgumentException expected){checks++;}
        double[] health={NpcVitals.STUDENT,NpcVitals.AWAKENED,NpcVitals.SHINJUKU,NpcVitals.SUKUNA,NpcVitals.TRUE_SUKUNA};
        for(double hp:health){check(hp>40&&hp<=1024,"health increased within attribute limit");check(NpcVitals.preserved(20,40,(float)hp)==hp*.5,"old HP proportion");check(NpcVitals.preserved(0,40,(float)hp)==0,"migration does not resurrect dead NPC");check(NpcVitals.preserved(80,40,(float)hp)==hp,"migration clamps overheal");}
        int fists=0,red=0;
        for(int roll=0;roll<100;roll++){if(NpcMeleeRules.preferPunch(roll))fists++;check(NpcMeleeRules.punch(false,roll)==LimitlessKind.BLUE,"student fist remains Blue");if(NpcMeleeRules.punch(true,roll)==LimitlessKind.RED)red++;}
        check(fists==80&&red==30,"melee bias and advanced fist mix");
        check(NpcMeleeRules.WINDUP>0&&NpcMeleeRules.WINDUP<NpcMeleeRules.ANIMATION&&NpcMeleeRules.ANIMATION<NpcMeleeRules.INTERVAL,"punch lands before recovery and next attack");
        check(NpcMeleeRules.REMOTE_INTERVAL>=160,"remote spells have eight second minimum cooldown");
        System.out.println("NPC combat rules: "+checks+" checks PASS");
    }
}
