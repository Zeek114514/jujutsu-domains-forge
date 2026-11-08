package dev.unlimitedvoid.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.unlimitedvoid.FugaTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import java.util.Collection;

/** Continuous incandescent bow underneath the moving fire particles; ordinary world occlusion. */
final class FugaBowRenderer {
    private static int quads;
    static void render(RenderLevelStageEvent event,Collection<FugaClient.Effect> effects){
        var mc=Minecraft.getInstance();if(mc.level==null||effects.stream().noneMatch(e->!e.released))return;
        Vec3 camera=event.getCamera().getPosition();
        boolean blend=GL11.glIsEnabled(GL11.GL_BLEND),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int sa=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),da=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        var shader=RenderSystem.getShader();float[] color=RenderSystem.getShaderColor().clone();
        try{
            RenderSystem.enableBlend();RenderSystem.blendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);
            RenderSystem.enableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShader(GameRenderer::getPositionColorShader);
            var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            Matrix4f m=event.getPoseStack().last().pose();quads=0;
            for(var e:effects){
                if(e.released||e.from.distanceToSqr(camera)>ClientQuality.distance()*ClientQuality.distance())continue;
                float age=e.age(mc.level,event.getPartialTick()),q=FugaTimeline.charge(age);if(q<.02)continue;
                Vec3 p=e.origin(mc.level,event.getPartialTick()),d=e.direction(mc.level,event.getPartialTick());
                if(mc.player!=null&&e.owner.equals(mc.player.getUUID())&&mc.getCameraEntity()==mc.player&&mc.options.getCameraType().isFirstPerson())p=p.add(d.scale(1.4));
                Vec3 side=d.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
                Vec3 up=side.cross(d).normalize(),previous=null;
                int segments=ClientQuality.particlesPerTick()<100?32:72;
                for(int i=0;i<=segments;i++){
                    double a=-1.4+i*2.8/segments;
                    Vec3 point=p.add(d.scale((.20+Math.cos(a)*.85)*q)).add(up.scale(Math.sin(a)*3.5*q)).add(side.scale((Math.cos(a)-1)*1.25*q));
                    if(previous!=null)glow(b,m,camera,previous,point,(.10+.025*Math.sin(age*.5+a*7))*q,q);
                    previous=point;
                }
                Vec3 pull=p.add(d.scale((.345-1.05*q)*q));
                for(int sign:new int[]{-1,1}){
                    Vec3 end=p.add(d.scale(.345*q)).add(up.scale(sign*3.45*q)).add(side.scale(-1.04*q));
                    glow(b,m,camera,pull,end,.025*q,q);
                }
                Vec3 tip=p.add(d.scale(4.8*q));
                glow(b,m,camera,p.add(d.scale(-.7*q)),tip,.065*q,q);
                for(Vec3 axis:new Vec3[]{up,side})for(int sign:new int[]{-1,1})
                    glow(b,m,camera,tip,tip.add(d.scale(-.95*q)).add(axis.scale(sign*.43*q)),.06*q,q);
            }
            BufferUploader.drawWithShader(b.end());
        }finally{
            RenderSystem.setShader(()->shader);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
            RenderSystem.depthMask(mask);RenderSystem.blendFuncSeparate(src,dst,sa,da);
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
        }
    }
    private static void glow(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 a,Vec3 z,double width,float alpha){
        Vec3 normal=z.subtract(a).cross(camera.subtract(a.add(z).scale(.5))).normalize();
        if(normal.lengthSqr()<.001)return;
        strip(b,m,camera,a,z,normal.scale(width*2.6),1,.12f,.005f,alpha*.22f);
        strip(b,m,camera,a,z,normal.scale(width),1,.48f,.025f,alpha*.9f);
        strip(b,m,camera,a,z,normal.scale(width*.28),1,.96f,.72f,alpha);
    }
    private static void strip(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 a,Vec3 z,Vec3 w,float r,float g,float blue,float alpha){
        if(quads++>=4096)return;
        for(Vec3 p:new Vec3[]{a.subtract(w),z.subtract(w),z.add(w),a.add(w)}){
            Vec3 v=p.subtract(camera);b.vertex(m,(float)v.x,(float)v.y,(float)v.z).color(r,g,blue,alpha).endVertex();
        }
    }
}
