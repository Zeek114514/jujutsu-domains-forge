package dev.unlimitedvoid;

import net.minecraft.world.phys.Vec3;

/** Shared timing and swept collision for the airborne Blue/Red combination. */
public final class PurpleDetonation {
    public static final int AIR=3,GROWING=4,BLAST=5,HOLD=160,GROW_TICKS=44,BLAST_TICKS=40,HIT_TICKS=28;
    public static final int SCAN_BUDGET=128,TARGET_LIMIT=512;
    public static final double CONTACT=2.4,RED_AIR_RANGE=32,CORE_RADIUS=12;
    private PurpleDetonation(){}
    public static Vec3 closest(Vec3 from,Vec3 to,Vec3 point){
        Vec3 line=to.subtract(from);double length=line.lengthSqr();
        double t=length<1e-12?0:Math.max(0,Math.min(1,point.subtract(from).dot(line)/length));
        return from.add(line.scale(t));
    }
    public static boolean touches(Vec3 from,Vec3 to,Vec3 center){return closest(from,to,center).distanceToSqr(center)<=CONTACT*CONTACT;}
    public static double core(double age){return 1.4+(CORE_RADIUS-1.4)*LimitlessKind.smooth((float)(age/GROW_TICKS));}
    public static double front(double age,double radius){return Math.min(CORE_RADIUS,radius)+(radius-Math.min(CORE_RADIUS,radius))*LimitlessKind.smooth((float)(age/16));}
    public static float damage(double distance,double radius,double maximum){
        if(!Double.isFinite(distance)||!Double.isFinite(radius)||!Double.isFinite(maximum)||distance<0||radius<=0||maximum<=0||distance>radius)return 0;
        return (float)(maximum*(1-.75*distance/radius));
    }
}
