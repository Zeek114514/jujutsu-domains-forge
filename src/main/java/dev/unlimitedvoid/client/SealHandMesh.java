package dev.unlimitedvoid.client;

import com.mojang.blaze3d.vertex.*;
import dev.unlimitedvoid.DomainKind;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Skin-textured palms and individually jointed voxel fingers. Units are blocks. */
final class SealHandMesh {
    private SealHandMesh(){}
    static void forearm(PoseStack pose,VertexConsumer out,int light,boolean right){
        double sign=right?1:-1;int u=right?44:36,v=right?20:52;
        Vec3 elbow=new Vec3(-sign*.30,.58,-.30),bottom=new Vec3(-sign*.44,1.0,-.55);
        bar(pose,out,light,bottom,elbow,.20,.20,u,v,4,4);
        bar(pose,out,light,elbow,Vec3.ZERO,.18,.18,u,v+4,4,4);
    }
    static void hand(PoseStack pose,VertexConsumer out,int light,DomainKind kind,boolean right){
        double s=right?1:-1;int u=right?44:36,v=right?29:61;
        bar(pose,out,light,Vec3.ZERO,new Vec3(0,-.13,0),.17,.075,u,v,4,2);
        if(kind==DomainKind.VOID){
            // Right index upright, middle finger hooked across it; remaining fingers curled.
            finger(pose,out,light,u,v,s,new double[][]{{.052,-.125,0},{.052,-.23,0},{.045,-.34,0}},.036);
            finger(pose,out,light,u,v,s,new double[][]{{.010,-.125,0},{.006,-.22,-.012},{.060,-.275,-.045},{.079,-.245,-.052}},.038);
            curled(pose,out,light,u,v,s,-.032);curled(pose,out,light,u,v,s,-.071);
            finger(pose,out,light,u,v,s,new double[][]{{.082,-.025,0},{.108,-.077,-.035},{.055,-.12,-.065}},.047);
        }else{
            // Enmaten: middle/ring fingers rise together, index and little fingers fold.
            finger(pose,out,light,u,v,s,new double[][]{{.018,-.125,-.018},{.047,-.23,-.018},{.066,-.335,-.018}},.035);
            finger(pose,out,light,u,v,s,new double[][]{{-.024,-.125,.021},{.026,-.22,.021},{.066,-.31,.021}},.035);
            curled(pose,out,light,u,v,s,.061);curled(pose,out,light,u,v,s,-.068);
            finger(pose,out,light,u,v,s,new double[][]{{.080,-.015,0},{.112,-.07,-.04},{.076,-.11,-.065}},.045);
        }
    }
    private static void curled(PoseStack p,VertexConsumer b,int l,int u,int v,double s,double x){
        finger(p,b,l,u,v,s,new double[][]{{x,-.123,0},{x,-.182,-.015},{x,-.175,-.090},{x,-.115,-.105}},.034);
    }
    private static void finger(PoseStack p,VertexConsumer b,int l,int u,int v,double s,double[][] points,double width){
        for(int i=1;i<points.length;i++)bar(p,b,l,new Vec3(points[i-1][0]*s,points[i-1][1],points[i-1][2]),new Vec3(points[i][0]*s,points[i][1],points[i][2]),width,width,u+(i%2),v,1,1);
    }
    private static void bar(PoseStack p,VertexConsumer out,int light,Vec3 a,Vec3 b,double width,double depth,int u,int v,int tw,int th){
        Vec3 axis=b.subtract(a).normalize();if(axis.lengthSqr()<.1)return;
        Vec3 side=axis.cross(new Vec3(0,0,1));if(side.lengthSqr()<.01)side=axis.cross(new Vec3(0,1,0));side=side.normalize();
        Vec3 normal=side.cross(axis).normalize(),x=side.scale(width/2),z=normal.scale(depth/2);
        Vec3[] q={a.subtract(x).subtract(z),a.add(x).subtract(z),a.add(x).add(z),a.subtract(x).add(z),b.subtract(x).subtract(z),b.add(x).subtract(z),b.add(x).add(z),b.subtract(x).add(z)};
        face(p,out,light,q,0,4,5,1,normal.scale(-1),u,v,tw,th);
        face(p,out,light,q,3,2,6,7,normal,u,v,tw,th);
        face(p,out,light,q,0,3,7,4,side.scale(-1),u,v,tw,th);
        face(p,out,light,q,1,5,6,2,side,u,v,tw,th);
        face(p,out,light,q,0,1,2,3,axis.scale(-1),u,v,tw,th);
        face(p,out,light,q,4,7,6,5,axis,u,v,tw,th);
    }
    private static void face(PoseStack p,VertexConsumer out,int light,Vec3[] q,int a,int b,int c,int d,Vec3 n,int u,int v,int w,int h){
        vertex(p,out,light,q[a],n,u/64f,(v+h)/64f);vertex(p,out,light,q[b],n,(u+w)/64f,(v+h)/64f);
        vertex(p,out,light,q[c],n,(u+w)/64f,v/64f);vertex(p,out,light,q[d],n,u/64f,v/64f);
    }
    private static void vertex(PoseStack p,VertexConsumer out,int light,Vec3 a,Vec3 n,float u,float v){
        var pose=p.last();out.vertex(pose.pose(),(float)a.x,(float)a.y,(float)a.z).color(255,255,255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),(float)n.x,(float)n.y,(float)n.z).endVertex();
    }
}
