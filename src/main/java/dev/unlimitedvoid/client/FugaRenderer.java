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
            // Alpha compositing keeps the orange silhouette readable when many lobes overlap.
            RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShader(GameRenderer::getPositionTexColorShader);RenderSystem.setShaderTexture(0,TEXTURE);
            BufferBuilder buffer=Tesselator.getInstance().getBuilder();buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f matrix=event.getPoseStack().last().pose();
            float partial=mc.isPaused()?0:event.getPartialTick();
            for(var e:effects) {
                if(e.from.distanceToSqr(camera)>FugaTimeline.VIEW_RANGE*FugaTimeline.VIEW_RANGE&&e.to.distanceToSqr(camera)>FugaTimeline.VIEW_RANGE*FugaTimeline.VIEW_RANGE)continue;
                float age=e.age(mc.level,partial);
                if(!e.released) {
                    Vec3 p=e.origin(mc.level,partial),direction=e.direction(mc.level,partial);
                    // First-person viewmodel spacing; observers retain the hand-aligned world anchor.
                    if(mc.player!=null&&e.owner.equals(mc.player.getUUID())&&mc.options.getCameraType().isFirstPerson()&&mc.getCameraEntity()==mc.player)p=p.add(direction.scale(2.8));
                    float charge=FugaTimeline.charge(Math.min(age,FugaTimeline.CHARGE));
                    chargeAura(buffer,matrix,camera,e,right,up,mc,age,charge,partial,1);
                    float scale=FugaTimeline.BOW_SCALE;
                    Vec3 side=direction.cross(new Vec3(0,1,0));if(side.lengthSqr()<.001)side=new Vec3(1,0,0);else side=side.normalize();
                    Vec3 vertical=side.cross(direction).normalize();
                    // Curling fire converges on the hand before the arrow takes shape.
                    for(int i=0;i<48;i++) {
                        double a=i*2.399+age*.16,rad=((1-charge)*1.25+.12)*scale;
                        Vec3 spark=p.add(side.scale(Math.cos(a)*rad)).add(vertical.scale(Math.sin(a)*rad));
                        flame(buffer,matrix,camera,spark,right,up,.18*scale,.38*scale,age+i,.3f+charge*.35f);
                    }
                    if(charge>.22f) {
                        arrow(buffer,matrix,camera,p,direction,vertical,side,age,charge*scale);
                        Vec3 prior=null,top=null,bottom=null;
                        for(int i=0;i<=32;i++) {
                            double a=-1.4+i/32.0*2.8;
                            Vec3 q=p.add(direction.scale((.1+Math.cos(a)*.65)*scale)).add(vertical.scale(Math.sin(a)*.95*charge*scale));
                            if(i==0)bottom=q;if(i==32)top=q;
                            if(prior!=null){
                                ribbon(buffer,matrix,camera,prior,q,side,(.045+charge*.065)*scale,age+i,.85f);
                                if(i%2==0)flame(buffer,matrix,camera,q,side,vertical,.2*scale,.45*scale,age+i,.6f*charge);
                            }
                            prior=q;
                        }
                        Vec3 draw=p.subtract(direction.scale((.25+.65*charge)*scale));
                        ribbon(buffer,matrix,camera,bottom,draw,side,.018*scale,age,.8f);
                        ribbon(buffer,matrix,camera,draw,top,side,.018*scale,age,.8f);
                        spiral(buffer,matrix,camera,p,direction,vertical,side,age,charge*scale,.7);
                    }
                } else {
                    float burst=FugaTimeline.burstAge(age,e.flight);
                    float afterDraw=age-FugaTimeline.CHARGE;
                    if(afterDraw<10)chargeAura(buffer,matrix,camera,e,right,up,mc,age,1,partial,1-FugaTimeline.smooth(afterDraw/10));
                    if(burst<0) {
                        Vec3 direction=e.direction(mc.level,partial),p=e.from.lerp(e.to,FugaTimeline.travel(age,e.flight));
                        Vec3 side=direction.cross(new Vec3(0,1,0));if(side.lengthSqr()<.001)side=new Vec3(1,0,0);else side=side.normalize();
                        Vec3 vertical=side.cross(direction).normalize();
                        arrow(buffer,matrix,camera,p,direction,vertical,side,age,FugaTimeline.BOW_SCALE);
                        double available=e.from.distanceTo(p);
                        spiral(buffer,matrix,camera,p,direction,vertical,side,age,FugaTimeline.BOW_SCALE,1.3);
                        for(int i=1;i<=32;i++) {
                            double behind=i*.45;if(behind>available)break;
                            Vec3 trail=p.subtract(direction.scale(behind)).add(vertical.scale(Math.sin(age*.6-i)*.04));
                            flame(buffer,matrix,camera,trail,right,up,.48+i*.02,.85+i*.035,age-i,(1-i/33f)*.7f);
                        }
                    } else if(burst<FugaTimeline.AFTERMATH) {
                        float grow=FugaTimeline.smooth(burst/18),fade=FugaTimeline.burstFade(burst);
                        int stride=effects.size()>2||e.to.distanceToSqr(camera)>400*400?2:1;
                        // Ignition travels across a 400-block-wide footprint; bounded cached terrain samples.
                        var tiles=e.ground(mc.level);
                        double front=FugaTimeline.fireRadius(burst);
                        for(int i=0;i<tiles.length;i+=stride){
                            Vec3 root=tiles[i];
                            double radius=Math.sqrt(Math.pow(root.x-e.to.x,2)+Math.pow(root.z-e.to.z,2));
                            if(radius>front)continue;
                            float ignition=FugaTimeline.smooth((float)((front-radius)/18));
                            double h=12+random(e.seed,i+400)*20;
                            double width=22+random(e.seed,i+800)*18;
                            float pulse=.8f+.2f*(float)Math.sin(age*.19+i);
                            flame(buffer,matrix,camera,root.add(0,h*.45,0),right,new Vec3(0,1,0),width,h,age+i,fade*ignition*.68f*pulse);
                            flame(buffer,matrix,camera,root,new Vec3(1,0,0),new Vec3(0,0,1),width,width,age+i+3,fade*ignition*.5f);
                        }
                        // A broad base, narrowed rising stem and billowing crown give the blast a distant silhouette.
                        for(int i=0;i<384;i+=stride){
                            int tier=i%16;
                            double y=(tier+.5)/16.0*FugaTimeline.COLUMN_HEIGHT;
                            double radius=tier<4?65:tier>11?78:38;
                            double a=i*2.399963+age*.006,rad=Math.sqrt(random(e.seed,i+80))*radius*grow;
                            double h=18+random(e.seed,i+130)*16;
                            Vec3 p=e.to.add(Math.cos(a)*rad,(y+Math.sin(age*.1+i)*3)*grow,Math.sin(a)*rad);
                            flame(buffer,matrix,camera,p,right,new Vec3(0,1,0),(15+random(e.seed,i+270)*19)*grow,h*grow,age+i,fade*.72f);
                        }
                        // Three city-scale pressure fronts, followed by a high expanding crown.
                        for(int wave=0;wave<3;wave++){
                            float t=burst-wave*7;
                            if(t>=0&&t<52){
                                double radius=FugaTimeline.shockRadius(t);
                                ring(buffer,matrix,camera,e.to.add(0,.4+wave*.8,0),radius,2.5+t*.06,t,(1-FugaTimeline.smooth((t-30)/22))*fade);
                                if(wave==0)for(int i=0;i<96;i+=stride){
                                    double a=i*Math.PI/48,h=(6+random(e.seed,i+1000)*14)*(1-t/52);
                                    Vec3 p=e.to.add(Math.cos(a)*radius,h*.5,Math.sin(a)*radius);
                                    flame(buffer,matrix,camera,p,right,new Vec3(0,1,0),8,h,age+i,(1-t/52)*fade*.65f);
                                }
                            }
                        }
                        for(int i=0;i<96;i+=stride){
                            double a=i*2.399,speed=.4+random(e.seed,i+300)*1.1;
                            Vec3 p=e.to.add(Math.cos(a)*speed*burst,1+burst*(.9+random(e.seed,i+350)*1.2)-.014*burst*burst,Math.sin(a)*speed*burst);
                            if(p.y>=e.to.y)flame(buffer,matrix,camera,p,right,up,.4,.9,age+i,fade*.8f);
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
    private static void chargeAura(BufferBuilder b,Matrix4f m,Vec3 camera,FugaClient.Effect e,Vec3 right,Vec3 up,Minecraft mc,float age,float charge,float partial,float fade){
        var caster=mc.level.getPlayerByUUID(e.owner);if(caster==null||charge<=0||fade<=0)return;
        Vec3 base=caster.getPosition(partial).add(0,.12,0);
        double radius=FugaTimeline.CHARGE_RADIUS*charge;
        ring(b,m,camera,base,radius,.2+charge*.5,age,charge*fade*.8f);
        ring(b,m,camera,base.add(0,.08,0),radius*.48,.18+charge*.3,age+4,charge*fade*.7f);
        // Three expanding helical streams pull into the raised fire bow, rather than just sparks at the hand.
        for(int strand=0;strand<3;strand++)for(int i=0;i<32;i++){
            double t=i/31.0,a=strand*Math.PI*2/3+t*Math.PI*2-age*.12;
            double r=radius*(1-t)*(.8+.2*Math.sin(age*.1+t*5));
            Vec3 p=base.add(Math.cos(a)*r,t*t*(3+charge*9),Math.sin(a)*r);
            flame(b,m,camera,p,right,up,(.4+charge*1.1)*(1-t*.4),(.8+charge*2.4)*(1-t*.3),age+i+strand*7,charge*fade*.55f);
        }
        for(int i=0;i<24;i++){
            double a=i*Math.PI/12+age*.04,r=radius*.3;
            double height=(1.3+random(e.seed,i+1200)*3)*charge;
            flame(b,m,camera,base.add(Math.cos(a)*r,height*.5,Math.sin(a)*r),right,new Vec3(0,1,0),.8+charge, height,age+i,charge*fade*.45f);
        }
    }
    private static void arrow(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 p,Vec3 d,Vec3 up,Vec3 side,float age,float charge) {
        for(int i=0;i<18;i++) {
            double q=(-1.1+i*.22)*charge,spread=(.08+.17*Math.sin(i/18.0*Math.PI))*charge;
            Vec3 c=p.add(d.scale(q)).add(up.scale(Math.sin(age*.7+i)*.025));
            flame(b,m,camera,c,d,up,.6*charge,spread*2,age+i,.85f);
            flame(b,m,camera,c,d,side,.6*charge,spread*2,age+i+2,.75f);
        }
        Vec3 tip=p.add(d.scale(3.1*charge)),base=p.add(d.scale(2.1*charge));
        ribbon(b,m,camera,base.add(up.scale(.38*charge)),tip,side,.085*charge,age,.95f);
        ribbon(b,m,camera,base.add(up.scale(-.38*charge)),tip,side,.085*charge,age+2,.95f);
        ribbon(b,m,camera,p.subtract(d.scale(1.2*charge)),tip,up,.06*charge,age+4,.95f);
    }
    private static void spiral(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 p,Vec3 d,Vec3 up,Vec3 side,float age,float charge,double scale){
        for(int strand=0;strand<2;strand++){
            Vec3 prior=null;
            for(int i=0;i<=32;i++){
                double a=i*.48-age*.32+strand*Math.PI,r=(.12+.23*Math.sin(i/32.0*Math.PI))*charge*scale;
                Vec3 q=p.add(d.scale((-1.2+i*.12)*charge)).add(side.scale(Math.cos(a)*r)).add(up.scale(Math.sin(a)*r));
                if(prior!=null)ribbon(b,m,camera,prior,q,up,.04*charge,age+i,.65f*charge);
                prior=q;
            }
        }
    }
    private static void ring(BufferBuilder b,Matrix4f m,Vec3 camera,Vec3 center,double radius,double width,float age,float alpha) {
        for(int i=0;i<128;i++) {
            double a=i*Math.PI/64,z=(i+1)*Math.PI/64;
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
