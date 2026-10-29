package dev.unlimitedvoid;
public final class MahoragaRulesCheck {
    static int checks;static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
    public static void main(String[] args){
        int packed=0;for(var kind:MahoragaRules.Kind.values()){packed=MahoragaRules.setStage(packed,kind,4);check(MahoragaRules.stage(packed,kind)==4,"independent category bit packing");}for(var kind:MahoragaRules.Kind.values())check(MahoragaRules.stage(packed,kind)==4,"neighbor categories retained");
        for(var kind:MahoragaRules.Kind.values())for(int stage=0;stage<4;stage++){long age=(stage+1)*(kind==MahoragaRules.Kind.PURPLE?160:60);check(!MahoragaRules.advance(kind,stage,(stage+1)*3-1,age,40),"requires repeated exposure");check(!MahoragaRules.advance(kind,stage,(stage+1)*3,age-1,40),"requires adaptation time");check(!MahoragaRules.advance(kind,stage,(stage+1)*3,age,39),"no instant chained adaptation");check(MahoragaRules.advance(kind,stage,(stage+1)*3,age,40),"threshold advances");}
        check(MahoragaRules.damageFactor(0)==1,"first encounter takes full damage");check(MahoragaRules.damageFactor(4)>0&&MahoragaRules.damageFactor(4)<=.1f,"adaptation never universally cancels lethal damage");check(MahoragaRules.forceFactor(4)==0,"full Blue Red adaptation cancels displacement");for(int i=1;i<=4;i++)check(MahoragaRules.damageFactor(i)<MahoragaRules.damageFactor(i-1),"progressively lower damage");
        check(MahoragaRules.growth(0)==1,"fresh size remains unchanged");int grow=0;float previous=1;
        for(var kind:MahoragaRules.Kind.values())for(int stage=1;stage<=4;stage++){grow=MahoragaRules.setStage(grow,kind,stage);float next=MahoragaRules.growth(MahoragaRules.adaptations(grow));check(next>previous,"every completed adaptation increases size");previous=next;}
        check(Math.abs(previous-2.6f)<.0001,"32 initial adaptations reach 260 percent");
        check(MahoragaRules.growth(100)>MahoragaRules.growth(32),"growth continues beyond initial resistance stages");check(MahoragaRules.growth(10000)>MahoragaRules.growth(100),"no gameplay growth ceiling");
        for(int type=1;type<=4;type++){
            var before=MahoragaAttackMotion.sample(type,0);
            for(float age=.25f;age<=30;age+=.25f){var pose=MahoragaAttackMotion.sample(type,age);check(Float.isFinite(pose.yaw())&&Float.isFinite(pose.left().pitch())&&Float.isFinite(pose.right().elbow())&&pose.leftLeg().knee()>=0&&pose.leftLeg().knee()<=2.01,"finite bounded articulated pose");check(Math.abs(pose.yaw()-before.yaw())<.45&&Math.abs(pose.left().pitch()-before.left().pitch())<.45&&Math.abs(pose.right().pitch()-before.right().pitch())<.45,"continuous motion without instantaneous pose changes");before=pose;}
            check(MahoragaAttackMotion.sample(type,30).equals(MahoragaAttackMotion.sample(0,0)),"every action returns to rest");
        }
        check(MahoragaAttackMotion.sample(1,6).left().elbow()<-1&&MahoragaAttackMotion.sample(1,10).left().elbow()>-.1,"punch retracts then straightens on actual contact");
        check(MahoragaAttackMotion.sample(2,11).yaw()<-.5&&MahoragaAttackMotion.sample(2,14).yaw()>.5,"sword twists from anticipation through target");
        check(MahoragaAttackMotion.sample(3,7).right().pitch()<-2.5&&MahoragaAttackMotion.sample(3,7).leftLeg().knee()>1.5&&MahoragaAttackMotion.sample(3,21).lean()>.3,"leap tucks legs raises blade and compresses on landing");
        check(MahoragaAttackMotion.contact(1)==10&&MahoragaAttackMotion.contact(2)==14&&MahoragaAttackMotion.contact(3)==18&&MahoragaAttackMotion.contact(4)==18,"contact frames preserve gameplay timing");
        System.out.println("Mahoraga rules: "+checks+" checks PASS");
    }
}
