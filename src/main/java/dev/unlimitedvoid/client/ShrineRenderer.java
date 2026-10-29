package dev.unlimitedvoid.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.unlimitedvoid.DomainKind;
import dev.unlimitedvoid.network.DomainNetwork.DomainState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import java.util.List;

/** Temporary client geometry: never places or removes world blocks. */
public final class ShrineRenderer {
    private static final int RED=0x85202D, DARK=0x211920, ROOF=0x25292B, BONE=0xD5C6A4;
    static int slashCount;
    public static void render(RenderLevelStageEvent event,List<DomainState> states) {
        if (states.stream().noneMatch(s->s.kind()==DomainKind.SHRINE)) return;
        boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),blend=GL11.glIsEnabled(GL11.GL_BLEND),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcA=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstA=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        ShaderInstance previous=RenderSystem.getShader();
        float[] color=RenderSystem.getShaderColor().clone();
        PoseStack poses=event.getPoseStack();
        try {
            RenderSystem.enableDepthTest(); RenderSystem.depthMask(true);
            RenderSystem.disableBlend(); RenderSystem.disableCull();
            RenderSystem.setShaderColor(1,1,1,1);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            slashCount=0;
            for (DomainState state:states) {
                if (state.kind()!=DomainKind.SHRINE || state.age()<dev.unlimitedvoid.DomainTimeline.formed(state.kind())) continue;
                float fade=dev.unlimitedvoid.DomainTimeline.fade(state.closing());
                RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,fade);
                // The open domain is indicated by a ground ring, never a blocking sphere.
                poses.pushPose();
                Vec3 offset=state.center().add(0,-0.96,0).subtract(event.getCamera().getPosition());
                poses.translate(offset.x,offset.y,offset.z);
                Matrix4f matrix=poses.last().pose();
                BufferBuilder buffer=Tesselator.getInstance().getBuilder();
                buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
                for (int i=0;i<192;i++) {
                    if (i%12==0) continue;
                    double a=i*Math.PI*2/192,b=(i+1)*Math.PI*2/192;
                    double r=state.radius();
                    quad(buffer,matrix,new double[][]{{Math.cos(a)*r,0,Math.sin(a)*r},{Math.cos(b)*r,0,Math.sin(b)*r},
                            {Math.cos(b)*(r-0.12),0,Math.sin(b)*(r-0.12)},{Math.cos(a)*(r-0.12),0,Math.sin(a)*(r-0.12)}},0xA2162E,1);
                }
                RenderSystem.depthMask(false);
                if(state.closing()<0){slashes(buffer,matrix,state,event.getCamera().getPosition(),event.getPartialTick());
                fieldSlashes(buffer,matrix,state,event.getCamera().getPosition(),event.getPartialTick());}
                BufferUploader.drawWithShader(buffer.end());
                poses.popPose();
            }
        } finally {
            RenderSystem.depthMask(mask);
            RenderSystem.blendFuncSeparate(src,dst,srcA,dstA);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
            RenderSystem.setShader(()->previous);
        }
    }
    public static void renderEntityModel(VertexConsumer vertices,Matrix4f matrix) { shrine(vertices,matrix); }
    private static void shrine(VertexConsumer b,Matrix4f m) {
        // The lower "second mouth" in the reference is a reflection, not another storey.
        box(b,m,-5.8,0,-4.45,5.8,0.24,4.45,0x19151B);
        box(b,m,-5.55,0.24,-4.2,5.55,0.47,4.2,0x57232D);
        box(b,m,-5.28,0.47,-3.95,5.28,0.70,3.95,0x252229);
        box(b,m,-5.0,0.70,-3.70,5.0,0.86,3.70,0x70232D);
        for(int i=0;i<4;i++)box(b,m,-2.8,i*0.18,3.5,2.8,(i+1)*0.18,5.05-i*0.38,0x5D5251);

        // One tall gate body, with an identical rear facade and solid side walls.
        box(b,m,-3.65,0.86,-2.65,3.65,7.05,2.65,0x641A2C);
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
            box(b,m,x*3.65-0.27,0.86,z*2.68-0.27,x*3.65+0.27,7.03,z*2.68+0.27,0x7B2430);
            box(b,m,x*3.65-0.39,6.56,z*2.68-0.39,x*3.65+0.39,7.02,z*2.68+0.39,BONE);
        }
        for(int side:new int[]{-1,1}){
            Matrix4f face=side==1?m:new Matrix4f(m).rotateY((float)Math.PI);
            maw(b,face);
            for(int x:new int[]{-1,1}){
                // Low side pedestals with red pyramidal caps and swept ivory tusks.
                box(b,face,x*4.75-0.27,0.85,2.65,x*4.75+0.27,2.55,3.35,0x29262B);
                box(b,face,x*4.75-0.48,2.45,2.42,x*4.75+0.48,2.64,3.55,BONE);
                pyramid(b,face,x*4.75,2.64,3.0,0.52,0.68,0xAF2336);
                tusk(b,face,new Vec3(x*3.95,1.65,3.05),new Vec3(x*5.45,2.05,3.14),
                        new Vec3(x*5.65,3.85,3.05),new Vec3(x*4.95,5.02,2.94),0.29);
                tusk(b,face,new Vec3(x*5.12,2.52,2.95),new Vec3(x*6.20,2.85,3.00),
                        new Vec3(x*6.16,3.75,2.98),new Vec3(x*5.70,4.25,2.90),0.16);
            }
            // Three hanging skulls instead of dense rows covering the gate.
            for(int i=-1;i<=1;i++)skull(b,face,i*1.38,0.92,3.77,i==0?0.38:0.35);
        }
        // A broad main eave, a short red neck, then the smaller crown roof.
        box(b,m,-4.12,6.92,-3.12,4.12,7.18,3.12,0x3A2028);
        roof(b,m,0,dev.unlimitedvoid.ShrineGeometry.MAIN_ROOF.y(),0,
                dev.unlimitedvoid.ShrineGeometry.MAIN_ROOF.halfX(),dev.unlimitedvoid.ShrineGeometry.MAIN_ROOF.halfZ(),dev.unlimitedvoid.ShrineGeometry.MAIN_ROOF.height());
        box(b,m,-2.60,7.65,-1.85,2.60,8.90,1.85,0x6D2030);
        box(b,m,-2.75,8.45,-2.00,2.75,8.61,2.00,0xA24649);
        roof(b,m,0,dev.unlimitedvoid.ShrineGeometry.CROWN_ROOF.y(),0,
                dev.unlimitedvoid.ShrineGeometry.CROWN_ROOF.halfX(),dev.unlimitedvoid.ShrineGeometry.CROWN_ROOF.halfZ(),dev.unlimitedvoid.ShrineGeometry.CROWN_ROOF.height());
        box(b,m,-2.26,10.10,-0.14,2.26,10.27,0.14,BONE);
        for(int x:new int[]{-1,1})tusk(b,m,new Vec3(x*2.18,10.15,0),new Vec3(x*2.78,10.12,0),
                new Vec3(x*3.20,10.46,0),new Vec3(x*2.94,10.94,0),0.11);
        // Restrained side ribs keep the silhouette readable from an oblique angle.
        for(int x:new int[]{-1,1}){
            for(int z=-2;z<=2;z++)box(b,m,x*3.68-0.07,1.55,z*0.92-0.07,x*3.68+0.07,6.35,z*0.92+0.07,0x9C7B69);
            box(b,m,x*3.70-0.10,1.28,-2.62,x*3.70+0.10,1.48,2.62,0xB6A183);
        }
    }
    private static void maw(VertexConsumer b,Matrix4f m) {
        box(b,m,-3.04,1.34,2.67,3.04,6.28,2.76,0x040306);
        box(b,m,-0.085,2.08,2.79,0.085,5.45,2.84,0xC32536);
        box(b,m,-1.13,3.20,2.79,1.13,3.39,2.84,0xC32536);
        for(int x:new int[]{-1,1}){
            box(b,m,x*3.08-0.16,1.22,2.80,x*3.08+0.16,6.45,3.10,BONE);
            box(b,m,x*3.36-0.07,1.22,2.76,x*3.36+0.07,6.50,2.97,0x967B66);
            // Paired rectangular ivory/red plaques above the mouth, as in the reference.
            box(b,m,x*1.65-0.74,6.50,2.78,x*1.65+0.74,6.98,3.06,BONE);
            box(b,m,x*1.65-0.39,6.61,3.07,x*1.65+0.39,6.85,3.10,0xB34A4A);
        }
        box(b,m,-3.26,1.15,2.75,3.26,1.44,3.22,BONE);
        box(b,m,-3.26,6.16,2.75,3.26,6.45,3.22,BONE);
        for(int i=0;i<15;i++){
            double x=-2.86+i*0.409,len=0.42+0.23*(0.5+0.5*Math.sin(i*2.1));
            fang(b,m,x,1.44,3.02,0.15,len,false);
            fang(b,m,x,6.16,3.02,0.17,len+(i%4==0?0.18:0.06),true);
        }
    }
    private static void pyramid(VertexConsumer b,Matrix4f m,double x,double y,double z,double width,double height,int color){
        double[][] base={{x-width,y,z-width},{x+width,y,z-width},{x+width,y,z+width},{x-width,y,z+width}};
        for(int i=0;i<4;i++)quad(b,m,new double[][]{base[i],base[(i+1)%4],{x,y+height,z},{x,y+height,z}},color,0.72f+i*0.07f);
    }
    private static Vec3 bezier(Vec3 a,Vec3 b,Vec3 c,Vec3 d,double t){double u=1-t;return a.scale(u*u*u).add(b.scale(3*u*u*t)).add(c.scale(3*u*t*t)).add(d.scale(t*t*t));}
    private static void tusk(VertexConsumer b,Matrix4f m,Vec3 a,Vec3 c1,Vec3 c2,Vec3 end,double radius){
        for(int i=0;i<12;i++){
            double t0=i/12.0,t1=(i+1)/12.0;
            Vec3 p0=bezier(a,c1,c2,end,t0),p1=bezier(a,c1,c2,end,t1);
            Vec3 tangent=p1.subtract(p0).normalize(),right=tangent.cross(new Vec3(0,0,1)).normalize(),up=tangent.cross(right).normalize();
            double r0=radius*Math.pow(1-t0,0.8)+0.002,r1=radius*Math.pow(1-t1,0.8)+0.002;
            for(int j=0;j<8;j++){
                double a0=j*Math.PI/4,a1=(j+1)*Math.PI/4;
                Vec3 v0=p0.add(right.scale(Math.cos(a0)*r0)).add(up.scale(Math.sin(a0)*r0));
                Vec3 v1=p0.add(right.scale(Math.cos(a1)*r0)).add(up.scale(Math.sin(a1)*r0));
                Vec3 v2=p1.add(right.scale(Math.cos(a1)*r1)).add(up.scale(Math.sin(a1)*r1));
                Vec3 v3=p1.add(right.scale(Math.cos(a0)*r1)).add(up.scale(Math.sin(a0)*r1));
                quad(b,m,new double[][]{{v0.x,v0.y,v0.z},{v1.x,v1.y,v1.z},{v2.x,v2.y,v2.z},{v3.x,v3.y,v3.z}},BONE,(float)(0.77+0.20*Math.cos(a0)));
            }
        }
    }
    private static void skull(VertexConsumer b,Matrix4f m,double x,double y,double z,double size) {
        box(b,m,x-size,y,z-size*0.35,x+size,y+size*1.25,z+size*0.35,0xC8BA9D);
        box(b,m,x-size*0.72,y+size*0.55,z+size*0.36,x-size*0.18,y+size*0.95,z+size*0.40,0x1A0C15);
        box(b,m,x+size*0.18,y+size*0.55,z+size*0.36,x+size*0.72,y+size*0.95,z+size*0.40,0x1A0C15);
        box(b,m,x-size*0.12,y+size*0.32,z+size*0.36,x+size*0.12,y+size*0.55,z+size*0.40,0x301523);
        for(int t=-1;t<=1;t++) box(b,m,x+t*size*0.4-size*0.11,y-size*0.12,z+size*0.05,x+t*size*0.4+size*0.11,y+size*0.28,z+size*0.38,BONE);
    }
    private static void fang(VertexConsumer b,Matrix4f m,double x,double y,double z,double width,double length,boolean down) {
        double tip=y+(down?-length:length);
        quad(b,m,new double[][]{{x-width,y,z-width},{x+width,y,z-width},{x,tip,z},{x,tip,z}},0xA89576,1);
        quad(b,m,new double[][]{{x+width,y,z-width},{x+width,y,z+width},{x,tip,z},{x,tip,z}},BONE,1);
        quad(b,m,new double[][]{{x+width,y,z+width},{x-width,y,z+width},{x,tip,z},{x,tip,z}},0xEADBB7,1);
        quad(b,m,new double[][]{{x-width,y,z+width},{x-width,y,z-width},{x,tip,z},{x,tip,z}},BONE,0.8f);
    }
    private static void roof(VertexConsumer b,Matrix4f m,double cx,double y,double cz,double halfX,double halfZ,double height) {
        // Piecewise curved slopes rise at the corners, rather than a flat cuboid roof.
        for (int side:new int[]{-1,1}) for(int i=0;i<12;i++) {
            double a=i/12.0,c=(i+1)/12.0;
            double z0=cz+side*a*halfZ,z1=cz+side*c*halfZ;
            double y0=y+height*(1-a)+0.65*Math.pow(a,5),y1=y+height*(1-c)+0.65*Math.pow(c,5);
            double x0=halfX*(0.72+0.28*a),x1=halfX*(0.72+0.28*c);
            quad(b,m,new double[][]{{cx-x0,y0,z0},{cx+x0,y0,z0},{cx+x1,y1,z1},{cx-x1,y1,z1}},ROOF,1);
            // Parallel ivory/red tile seams make the roof readable at game scale.
            for (int tile=-7;tile<=7;tile++) {
                double t=tile/8.0,w=0.025;
                quad(b,m,new double[][]{{cx+t*x0-w,y0+0.012,z0},{cx+t*x0+w,y0+0.012,z0},
                        {cx+t*x1+w,y1+0.012,z1},{cx+t*x1-w,y1+0.012,z1}},tile%2==0?0x715D5C:0x40383B,1);
            }
            if(i==11) box(b,m,cx-x1,y1-0.16,Math.min(z0,z1),cx+x1,y1-0.01,Math.max(z0,z1),RED);
        }
    }
    private static void slashes(VertexConsumer b,Matrix4f m,DomainState state,Vec3 camera,float partial) {
        var mc=Minecraft.getInstance();
        if(mc.level==null) return;
        double radius=state.radius();
        AABB box=new AABB(state.center().subtract(radius,radius,radius),state.center().add(radius,radius,radius));
        Vec3 origin=state.center().add(0,-.96,0),viewer=camera.subtract(origin);int count=0;
        for(LivingEntity entity:mc.level.getEntitiesOfClass(LivingEntity.class,box)) {
            if(entity.getUUID().equals(state.owner()) || entity.getBoundingBox().getCenter().distanceToSqr(state.center())>radius*radius)continue;
            if(count++>=48)break;
            double clock=(state.age()+partial+entity.getId()*.73)/6,life=clock-Math.floor(clock);
            if(life>.86)continue;
            float alpha=slashFade(life);double seed=entity.getId()*2.37+Math.floor(clock)*1.63;
            Vec3 p=entity.getBoundingBox().getCenter().subtract(origin);
            double size=Math.max(1.6,entity.getBbHeight()*1.15);
            // Cleave appears as a brief crossing group close to each struck body.
            for(int i=0;i<3;i++) {
                Vec3 direction=new Vec3(Math.sin(seed+i*2.2),Math.cos(seed+i*1.7),Math.sin(seed+i*.9)).normalize();
                Vec3 shift=new Vec3(Math.sin(seed)*.16,(i-1)*.24+(life-.4)*.7,Math.cos(seed)*.16);
                blade(b,m,p.add(shift),direction,viewer,size*(1.1+.45*i),.028+.014*i,alpha,i==2);
            }
        }
    }
    private static void fieldSlashes(VertexConsumer b,Matrix4f m,DomainState state,Vec3 camera,float partial) {
        if(camera.distanceToSqr(state.center())>state.radius()*state.radius() || VoidClient.isCameraInside()) return;
        Vec3 viewer=camera.subtract(state.center().add(0,-0.96,0));
        for(int i=0;i<36;i++) {
            double clock=(state.age()+partial+i*.91)/7,life=clock-Math.floor(clock);
            if(life>.84)continue;
            double seed=i*7.93+Math.floor(clock)*1.67;
            Vec3 direction=new Vec3(Math.cos(seed),Math.sin(seed*1.43)*.85,Math.sin(seed)).normalize();
            Vec3 center=viewer.add(Math.sin(seed*1.7)*16,Math.cos(seed*2.2)*10,Math.sin(seed*.73)*16);
            Vec3 sweep=direction.cross(center.subtract(viewer)).normalize().scale((life-.4)*2.6);
            // Dismantle cuts travel through space; tapered blades have no rectangular ends.
            blade(b,m,center.add(sweep),direction,viewer,13+7*(1+Math.sin(seed*.8)),i%3==0?.085:.045,slashFade(life),i%3==0);
        }
    }
    private static float slashFade(double life){return dev.unlimitedvoid.DomainTimeline.smooth((float)(life/.10))*(1-dev.unlimitedvoid.DomainTimeline.smooth((float)((life-.32)/.52)));}
    public static void blade(VertexConsumer b,Matrix4f m,Vec3 center,Vec3 direction,Vec3 viewer,double length,double width,float alpha,boolean black){
        Vec3 cross=direction.cross(center.subtract(viewer)).normalize();if(cross.lengthSqr()<.01)return;
        Vec3 start=center.subtract(direction.scale(length*.5)),end=center.add(direction.scale(length*.5));
        slashCount++;
        diamond(b,m,start,end,center,cross.scale(width*4.8),black?0xFA1239:0xFFD4D8,alpha*(black?.17f:.08f));
        diamond(b,m,start,end,center,cross.scale(width*1.9),black?0xF01432:0xFFE4E4,alpha*(black?.8f:.32f));
        diamond(b,m,start,end,center,cross.scale(width),black?0x080106:0xFFFFFF,alpha);
    }
    private static void diamond(VertexConsumer b,Matrix4f m,Vec3 start,Vec3 end,Vec3 center,Vec3 halfWidth,int color,float alpha){
        for(Vec3 p:new Vec3[]{start,center.add(halfWidth),end,center.subtract(halfWidth)})
            b.vertex(m,(float)p.x,(float)p.y,(float)p.z).color(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,alpha).endVertex();
    }
    private static void box(VertexConsumer b,Matrix4f m,double x0,double y0,double z0,double x1,double y1,double z1,int color) {
        quad(b,m,new double[][]{{x0,y1,z0},{x1,y1,z0},{x1,y1,z1},{x0,y1,z1}},color,1.0f);
        quad(b,m,new double[][]{{x0,y0,z1},{x1,y0,z1},{x1,y0,z0},{x0,y0,z0}},color,0.45f);
        quad(b,m,new double[][]{{x0,y0,z1},{x0,y1,z1},{x1,y1,z1},{x1,y0,z1}},color,0.92f);
        quad(b,m,new double[][]{{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z0}},color,0.6f);
        quad(b,m,new double[][]{{x0,y0,z0},{x0,y1,z0},{x0,y1,z1},{x0,y0,z1}},color,0.65f);
        quad(b,m,new double[][]{{x1,y0,z1},{x1,y1,z1},{x1,y1,z0},{x1,y0,z0}},color,0.8f);
    }
    private static void quad(VertexConsumer b,Matrix4f m,double[][] p,int color,float shade) {
        float r=((color>>16)&255)/255f*shade,g=((color>>8)&255)/255f*shade,bl=(color&255)/255f*shade;
        for(double[] v:p) b.vertex(m,(float)v[0],(float)v[1],(float)v[2]).color(r,g,bl,1).endVertex();
    }
}
