package dev.unlimitedvoid;
public final class MahoragaRulesCheck {
    static int checks;static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
    public static void main(String[] args){
        int packed=0;for(var kind:MahoragaRules.Kind.values()){packed=MahoragaRules.setStage(packed,kind,4);check(MahoragaRules.stage(packed,kind)==4,"independent category bit packing");}for(var kind:MahoragaRules.Kind.values())check(MahoragaRules.stage(packed,kind)==4,"neighbor categories retained");
        for(var kind:MahoragaRules.Kind.values())for(int stage=0;stage<4;stage++){long age=(stage+1)*(kind==MahoragaRules.Kind.PURPLE?160:60);check(!MahoragaRules.advance(kind,stage,(stage+1)*3-1,age,40),"requires repeated exposure");check(!MahoragaRules.advance(kind,stage,(stage+1)*3,age-1,40),"requires adaptation time");check(!MahoragaRules.advance(kind,stage,(stage+1)*3,age,39),"no instant chained adaptation");check(MahoragaRules.advance(kind,stage,(stage+1)*3,age,40),"threshold advances");}
        check(MahoragaRules.damageFactor(0)==1,"first encounter takes full damage");check(MahoragaRules.damageFactor(4)>0&&MahoragaRules.damageFactor(4)<=.1f,"adaptation never universally cancels lethal damage");check(MahoragaRules.forceFactor(4)==0,"full Blue Red adaptation cancels displacement");for(int i=1;i<=4;i++)check(MahoragaRules.damageFactor(i)<MahoragaRules.damageFactor(i-1),"progressively lower damage");
        System.out.println("Mahoraga rules: "+checks+" checks PASS");
    }
}
