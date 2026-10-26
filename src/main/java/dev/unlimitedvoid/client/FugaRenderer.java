package dev.unlimitedvoid.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.unlimitedvoid.FugaTimeline;
import dev.unlimitedvoid.UnlimitedVoid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import java.util.Collection;

/** Bounded full-bright pixel ribbons and crossed flame cards, with ordinary depth occlusion. */
final class FugaRenderer {
    private static final ResourceLocation TEXTURE=new ResourceLocation(UnlimitedVoid.ID,"textures/effect/fuga_flame.png");
    private FugaRenderer() {}
    static void render(RenderLevelStageEvent event,Collection<FugaClient.Effect> effects) {
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        Vec3 camera=event.getCamera().getPosition();
        var r=event.getCamera().getLeftVector();var u=event.getCamera().getUpVector();
        Vec3 right=new Vec3(-r.x,-r.y,-r.z),up=new Vec3(u.x,u.y,u.z);
        boolean blend=GL11.glIsEnabled(GL11.GL_BLEND),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int texture=RenderSystem.getShaderTexture(0);ShaderInstance shader=RenderSystem.getShader();
        float[] color=RenderSystem.getShaderColor().clone();
        try {
            RenderSystem.enableBlend();RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShader(GameRenderer::getPositionTexColorShader);RenderSystem.setShaderTexture(0,TEXTURE);
            BufferBuilder buffer=Tesselator.getInstance().getBuilder();buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f matrix=event.getPoseStack().last().pose();
            float partial=mc.isPaused()?0:event.getPartialTick();
            for(var e:effects) {
                if(e.from.distanceToSqr(camera)>128*128&&e.to.distanceToSqr(camera)>128*128)continue;
                float age=e.age(mc.level,partial);
                if(!e.released) {
                    Vec3 p=e.origin(mc.level,partial),direction=e.direction(mc.level,partial);
                    float charge=FugaTimeline.charge(Math.min(age,FugaTimeline.CHARGE));
                    Vec3 side=direction.cross(new Vec3(0,1,0));if(side.lengthSqr()<.001)side=new Vec3(1,0,0);else side=side.normalize();
                    Vec3 vertical=side.cross(direction).normalize();
                    // Curling fire converges on the hand before the arrow takes shape.
                    for(int i=0;i<16;i++) {
                        double a=i*2.399+age*.16,rad=(1-charge)*.6+.08;
                        Vec3 spark=p.add(side.scale(Math.cos(a)*rad)).add(vertical.scale(Math.sin(a)*rad));
                        flame(buffer,matrix,camera,spark,right,up,.13,.24,age+i,.3f+charge*.3f);
                    }
                    if(charge>.22f) {
                        arrow(buffer,matrix,camera,p,direction,vertical,side,age,charge);
                        Vec3 prior=null;
                        for(int i=0;i<=22;i++) {
                            double a=-1.25+i/22.0*2.5;
                            Vec3 q=p.add(direction.scale(.25+Math.cos(a)*.28)).add(vertical.scale(Math.sin(a)*.82*charge));
                            if(prior!=null)ribbon(buffer,matrix,camera,prior,q,side,.025+charge*.025,age+i,.65f);
                            prior=q;
                        }
                    }
                } else {
                    float burst=FugaTimeline.burstAge(age,e.flight);
                    if(burst<0) {
                        Vec3 direction=e.direction(mc.level,partial),p=e.from.lerp(e.to,FugaTimeline.travel(age,e.flight));
                        Vec3 side=direction.cross(new Vec3(0,1,0));if(side.lengthSqr()<.001)side=new Vec3(1,0,0);else side=side.normalize();
                        Vec3 vertical=side.cross(direction).normalize();
                        arrow(buffer,matrix,camera,p,direction,vertical,side,age,1);
                        double available=e.from.distanceTo(p);
                        for(int i=1;i<=18;i++) {
                            double behind=i*.42;if(behind>available)break;
                            Vec3 trail=p.subtract(direction.scale(behind)).add(vertical.scale(Math.sin(age*.6-i)*.04));
                            flame(buffer,matrix,camera,trail,right,up,.32+i*.015,.52+i*.02,age-i,(1-i/19f)*.6f);
                        }
                    } else if(burst<FugaTimeline.AFTERMATH) {
                        float grow=FugaTimeline.smooth(burst/5),fade=1-FugaTimeline.smooth((burst-12)/36);
                        for(int i=0;i<44;i++) {
                            double a=i*2.399+random(e.seed,i)*.7,rad=(.4+random(e.seed,i+80)*2.9)*grow;
                            double h=2.2+random(e.seed,i+130)*4.8;
                            Vec3 p=e.to.add(Math.cos(a)*rad,(.4+random(e.seed,i+200)*2)*grow+burst*.025,Math.sin(a)*rad);
                            flame(buffer,matrix,camera,p,right,up,(.65+random(e.seed,i+270))*grow,h*grow,age+i,fade*.44f);
                        }
                        // Low, expanding shock front: a textured annulus, not a white sphere.
                        if(burst<24)ring(buffer,matrix,camera,e.to.add(0,.08,0),.3+burst*.34,.25+burst*.015,burst,fade);
                        for(int i=0;i<34;i++) {
                            double a=i*2.399,speed=.04+random(e.seed,i+300)*.14;
                            Vec3 p=e.to.add(Math.cos(a)*speed*burst,.2+burst*(.13+random(e.seed,i+350)*.12)-.004*burst*burst,Math.sin(a)*speed*burst);
                            if(p.y>=e.to.y)flame(buffer,matrix,camera,p,right,up,.07,.13,age+i,fade*.8f);
                        }
                    }
                }
            }
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            RenderSystem.setShaderTexture(0,texture);RenderSystem.setShader(()->shader);
            RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);RenderSystem.depthMask(depthMask);
            RenderSystem.blendFuncSeparate(src,dst,srcAlpha,dstAlpha);
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
        }
    }
    private static void arrow(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 p,Vec3 d,Vec3 up,Vec3 side,float age,float charge) {
        for(int i=0;i<12;i++) {
            double q=-.65+i*.18,spread=(.05+.09*Math.sin(i/12.0*Math.PI))*charge;
            Vec3 c=p.add(d.scale(q)).add(up.scale(Math.sin(age*.7+i)*.025));
            flame(b,m,camera,c,d,up,.4*charge,spread*2,age+i,.7f);
            flame(b,m,camera,c,d,side,.4*charge,spread*2,age+i+2,.6f);
        }
        Vec3 tip=p.add(d.scale(1.65*charge)),base=p.add(d.scale(1.16*charge));
        ribbon(b,m,camera,base.add(up.scale(.18*charge)),tip,side,.05*charge,age,.9f);
        ribbon(b,m,camera,base.add(up.scale(-.18*charge)),tip,side,.05*charge,age+2,.9f);
        ribbon(b,m,camera,p.subtract(d.scale(.7*charge)),tip,up,.023*charge,age+4,.9f);
    }
    private static void ring(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 center,double radius,double width,float age,float alpha) {
        for(int i=0;i<64;i++) {
            double a=i*Math.PI/32,z=(i+1)*Math.PI/32;
            Vec3 x=center.add(Math.cos(a)*radius,0,Math.sin(a)*radius),y=center.add(Math.cos(z)*radius,0,Math.sin(z)*radius);
            Vec3 outward=new Vec3(Math.cos((a+z)/2),0,Math.sin((a+z)/2));
            ribbon(b,m,camera,x,y,outward,width,age+i,alpha*.55f);
        }
    }
    private static void ribbon(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 a,Vec3 z,Vec3 width,double halfWidth,float frame,float alpha) {
        Vec3 w=width.scale(halfWidth);
        quad(b,m,camera,a.subtract(w),z.subtract(w),z.add(w),a.add(w),frame,alpha);
    }
    private static void flame(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 center,Vec3 right,Vec3 up,double width,double height,float frame,float alpha) {
        Vec3 x=right.scale(width*.5),y=up.scale(height*.5);
        quad(b,m,camera,center.subtract(x).subtract(y),center.add(x).subtract(y),center.add(x).add(y),center.subtract(x).add(y),frame,alpha);
    }
    private static void quad(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 a,Vec3 z,Vec3 c,Vec3 d,float frame,float alpha) {
        int f=Math.floorMod((int)(frame*.6f),8);float v0=f/8f,v1=(f+1)/8f;
        vertex(b,m,camera,a,0,v1,alpha);vertex(b,m,camera,z,1,v1,alpha);vertex(b,m,camera,c,1,v0,alpha);vertex(b,m,camera,d,0,v0,alpha);
    }
    private static void vertex(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 p,float u,float v,float alpha) {
        Vec3 q=p.subtract(camera);b.vertex(m,(float)q.x,(float)q.y,(float)q.z).uv(u,v).color(1f,1f,1f,Math.max(0,Math.min(1,alpha))).endVertex();
    }
    private static double random(int seed,int index) {
        int n=seed+index*374761393;n=(n^(n>>>13))*1274126177;return (n&0x7fffffff)/(double)Integer.MAX_VALUE;
    }
}
