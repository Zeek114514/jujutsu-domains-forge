package dev.unlimitedvoid;

import net.minecraft.world.phys.Vec3;

/** Collision and damage boundaries that are easy to miss with fast, airborne projectiles. */
public final class PurpleDetonationCheck {
    static int checks;
    public static void main(String[] args){
        var from=new Vec3(0,0,0);var to=new Vec3(10,0,0);
        check(PurpleDetonation.touches(from,to,new Vec3(5,0,0)),"sweep catches a sphere between endpoints");
        check(PurpleDetonation.touches(from,to,new Vec3(5,2.4,0)),"contact boundary inclusive");
        check(!PurpleDetonation.touches(from,to,new Vec3(5,2.401,0)),"near miss rejected");
        check(!PurpleDetonation.touches(from,to,new Vec3(13,0,0)),"projection cannot extend past endpoint");
        check(PurpleDetonation.touches(from,from,new Vec3(1,0,0)),"stationary overlap finite");
        check(!PurpleDetonation.touches(from,from,new Vec3(3,0,0)),"stationary non-overlap");
        check(PurpleDetonation.closest(to,from,new Vec3(4,7,0)).equals(new Vec3(4,0,0)),"reverse order sweep");
        check(PurpleDetonation.core(0)==1.4&&PurpleDetonation.core(44)==12,"visible growth endpoints");
        double previous=0;for(int i=0;i<70;i++){double r=PurpleDetonation.core(i);check(r>=previous&&r<=12,"monotonic bounded core");previous=r;}
        for(double radius:new double[]{8,64,128}){
            check(PurpleDetonation.front(0,radius)<=radius&&PurpleDetonation.front(16,radius)==radius,"wave reaches configured radius");
            check(PurpleDetonation.front(40,radius)==radius,"wave never overshoots");
        }
        check(PurpleDetonation.damage(0,64,480)==480,"center damage");
        check(PurpleDetonation.damage(64,64,480)==120,"edge damage");
        check(PurpleDetonation.damage(64.01,64,480)==0,"outside radius");
        check(PurpleDetonation.damage(Double.NaN,64,480)==0&&PurpleDetonation.damage(-1,64,480)==0,"invalid distance");
        check(PurpleDetonation.damage(2,0,480)==0&&PurpleDetonation.damage(2,64,0)==0,"disabled damage");
        System.out.println("Purple detonation collision/timing/damage: "+checks+" checks PASS");
    }
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
}
