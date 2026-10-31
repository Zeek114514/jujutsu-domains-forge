package dev.unlimitedvoid;
public final class NpcAttackMotionCheck {
    static int checks;
    static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    public static void main(String[] args){
        for(boolean gojo:new boolean[]{false,true})for(int step=0;step<3;step++)for(int kind:new int[]{0,2,4}){
            int hit=NpcAttackMotion.contact(step,kind),end=NpcAttackMotion.duration(step,kind);
            check(hit>=4&&hit<end-3,"windup and follow-through on both sides of contact");
            var rest=NpcAttackMotion.pose(gojo,step,kind,0);
            check(rest.equals(NpcAttackMotion.pose(gojo,step,kind,end))&&rest.equals(NpcAttackMotion.pose(gojo,step,kind,end+1)),"exact neutral on exit");
            var contact=NpcAttackMotion.pose(gojo,step,kind,hit);
            check(Math.abs(contact.left())>1||Math.abs(contact.right())>1,"contact reaches forward");
            check(Math.abs(contact.twist())>.3&&contact.step()>0,"body mechanics accompany contact");
            for(float t=0;t<end;t+=.5f){var p=NpcAttackMotion.pose(gojo,step,kind,t);check(Float.isFinite(p.left()+p.right()+p.twist()+p.lean()+p.step()+p.guard()),"finite interpolated pose");}
        }
        check(NpcAttackMotion.pose(true,0,0,4).left()<NpcAttackMotion.pose(true,0,0,4).right(),"opening left fist");
        check(NpcAttackMotion.pose(true,1,0,5).right()<NpcAttackMotion.pose(true,1,0,5).left(),"second right fist");
        System.out.println("NPC attack motion: "+checks+" checks PASS");
    }
}
