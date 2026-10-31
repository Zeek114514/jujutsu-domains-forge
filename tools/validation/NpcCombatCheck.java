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
        check(NpcAttackMotion.contact(0,0)>0&&NpcAttackMotion.contact(0,0)<NpcAttackMotion.duration(0,0)&&NpcMeleeRules.INTERVAL>=NpcAttackMotion.duration(0,0),"punch lands before recovery and next attack");
        check(NpcMeleeRules.REMOTE_INTERVAL>=160,"remote spells have eight second minimum cooldown");
        int domains=0,fire=0,summons=0;
        for(int roll=0;roll<1000;roll++){if(NpcBattleRules.domain(roll))domains++;if(NpcBattleRules.fuga(roll))fire++;if(NpcBattleRules.mahoraga(roll))summons++;}
        check(domains==15&&fire==3&&summons==10,"bounded battle rare probabilities");
        check(NpcBattleRules.INITIAL_DELAY>=200&&NpcBattleRules.ROLL_INTERVAL>=40,"rare roll cadence avoids per-tick spam");
        check(NpcVitals.MEGUMI>40&&NpcVitals.MEGUMI<=1024,"Megumi health profile within vanilla limits");
        for(var form:NpcPowerRules.Form.values()){check(NpcPowerRules.attack(form)>0&&NpcPowerRules.taken(form)>0,"finite positive current-form multipliers");check(NpcPowerRules.domainHit(form)>0&&NpcPowerRules.domainHit(form)<400,"domain hit does not execute full-health boss NPC");}
        check(NpcPowerRules.attack(NpcPowerRules.Form.STUDENT)<NpcPowerRules.attack(NpcPowerRules.Form.AWAKENED),"student below awakened");
        check(NpcPowerRules.attack(NpcPowerRules.Form.AWAKENED)<NpcPowerRules.attack(NpcPowerRules.Form.YUJI),"awakened below Yuji");
        check(NpcPowerRules.attack(NpcPowerRules.Form.YUJI)<NpcPowerRules.attack(NpcPowerRules.Form.SHINJUKU),"Yuji below Shinjuku");
        check(NpcPowerRules.attack(NpcPowerRules.Form.MEGUMI)<=NpcPowerRules.attack(NpcPowerRules.Form.TRUE)&&NpcPowerRules.taken(NpcPowerRules.Form.TRUE)<=NpcPowerRules.taken(NpcPowerRules.Form.MEGUMI),"true form at least Megumi");
        double[] values={400,800,800,1000,900,1000};double[] ratings=new double[6];for(var f:NpcPowerRules.Form.values())ratings[f.ordinal()]=values[f.ordinal()]*NpcPowerRules.attack(f)/NpcPowerRules.taken(f);
        check(ratings[0]<ratings[1]&&ratings[1]<ratings[2]&&ratings[2]<ratings[3],"requested lower-form power ordering");check(Math.abs(ratings[3]-ratings[4])<.01,"Shinjuku and Megumi share a baseline power rating");check(ratings[4]<=ratings[5],"true form is the highest baseline");
        System.out.println("NPC combat rules: "+checks+" checks PASS");
    }
}
