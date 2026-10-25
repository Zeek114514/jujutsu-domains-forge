package dev.unlimitedvoid;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Shared server/client collision geometry, aligned with the rendered shrine. */
public final class ShrineGeometry {
    public record RoofSpec(double y,double halfX,double halfZ,double height){}
    public static final RoofSpec MAIN_ROOF=new RoofSpec(7.05,5.9,3.95,0.95);
    public static final RoofSpec CROWN_ROOF=new RoofSpec(8.75,3.65,2.9,1.35);
    public static Vec3 anchor(Vec3 center,Vec3 axis) {
        Vec3 horizontal=new Vec3(axis.x,0,axis.z).normalize();
        if(horizontal.lengthSqr()<0.01)horizontal=new Vec3(0,0,1);
        return center.add(0,-1,0);
    }
    public static List<AABB> localBoxes() {
        List<AABB> boxes=new ArrayList<>();
        boxes.add(new AABB(-5.8,0,-4.45,5.8,0.24,4.45));
        boxes.add(new AABB(-5.55,0.24,-4.2,5.55,0.47,4.2));
        boxes.add(new AABB(-5.28,0.47,-3.95,5.28,0.70,3.95));
        boxes.add(new AABB(-5.0,0.70,-3.70,5.0,0.86,3.70));
        for(int i=0;i<4;i++)boxes.add(new AABB(-2.8,i*0.18,3.5,2.8,(i+1)*0.18,5.05-i*0.38));
        boxes.add(new AABB(-3.65,0.86,-2.65,3.65,7.05,2.65));
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
            boxes.add(new AABB(x*3.65-0.27,0.86,z*2.68-0.27,x*3.65+0.27,7.03,z*2.68+0.27));
            boxes.add(new AABB(x*3.65-0.39,6.56,z*2.68-0.39,x*3.65+0.39,7.02,z*2.68+0.39));
        }
        boxes.add(new AABB(-4.12,6.92,-3.12,4.12,7.18,3.12));
        roof(boxes,MAIN_ROOF.y,MAIN_ROOF.halfX,MAIN_ROOF.halfZ,MAIN_ROOF.height);
        boxes.add(new AABB(-2.60,7.65,-1.85,2.60,8.90,1.85));
        roof(boxes,CROWN_ROOF.y,CROWN_ROOF.halfX,CROWN_ROOF.halfZ,CROWN_ROOF.height);
        boxes.add(new AABB(-2.26,10.10,-0.14,2.26,10.27,0.14));
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})boxes.add(new AABB(x*4.75-0.27,0.85,z*3.0-0.35,x*4.75+0.27,2.64,z*3.0+0.35));
        return List.copyOf(boxes);
    }
    private static void roof(List<AABB> out,double y,double halfX,double halfZ,double height) {
        for(int side:new int[]{-1,1})for(int i=0;i<12;i++) {
            double a=i/12.0,c=(i+1)/12.0;
            double y0=y+height*(1-a)+0.65*Math.pow(a,5),y1=y+height*(1-c)+0.65*Math.pow(c,5);
            double width=halfX*(0.72+0.28*c);
            out.add(new AABB(-width,Math.min(y0,y1)-0.16,Math.min(side*a*halfZ,side*c*halfZ),
                    width,Math.max(y0,y1),Math.max(side*a*halfZ,side*c*halfZ)));
        }
    }
    public static List<AABB> worldBoxes(Vec3 anchor,float yaw) {
        double angle=Math.toRadians(yaw),sin=Math.sin(angle),cos=Math.cos(angle);
        List<AABB> result=new ArrayList<>();
        for(AABB b:localBoxes()) {
            // Subdivide wide strips before rotating so diagonal shrine corners
            // have tight collision instead of one large enclosing rectangle.
            int slices=Math.max(1,(int)Math.ceil((b.maxX-b.minX)/2));
            for(int i=0;i<slices;i++) {
                double x0=b.minX+(b.maxX-b.minX)*i/slices,x1=b.minX+(b.maxX-b.minX)*(i+1)/slices;
                double minX=Double.POSITIVE_INFINITY,maxX=-minX,minZ=minX,maxZ=-minX;
                for(double x:new double[]{x0,x1})for(double z:new double[]{b.minZ,b.maxZ}) {
                    double rx=x*cos+z*sin,rz=-x*sin+z*cos;
                    minX=Math.min(minX,rx);maxX=Math.max(maxX,rx);minZ=Math.min(minZ,rz);maxZ=Math.max(maxZ,rz);
                }
                result.add(new AABB(minX+anchor.x,b.minY+anchor.y,minZ+anchor.z,maxX+anchor.x,b.maxY+anchor.y,maxZ+anchor.z));
            }
        }
        return List.copyOf(result);
    }
}
