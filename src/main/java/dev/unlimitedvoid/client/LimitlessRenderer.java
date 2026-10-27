package dev.unlimitedvoid.client;

import com.mojang.blaze3d.vertex.*;
import dev.unlimitedvoid.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import java.util.Collection;

/** Full-bright vanilla render type, camera-facing cores and world-space energy ribbons. */
final class LimitlessRenderer {
    private static final RenderType TYPE=RenderType.entityTranslucentEmissive(new ResourceLocation(UnlimitedVoid.ID,"textures/effect/limitless_glow.png"));
    private static final float[][] COLORS={{.12f,.56f,1},{1,.10f,.16f},{.65f,.16f,1}};
    private static int quads;
    static int quads(){return quads;}
    static void render(RenderLevelStageEvent event,Collection<LimitlessClient.Effect> effects){
        var mc=Minecraft.getInstance();if(mc.level==null)return;var buffers=mc.renderBuffers().bufferSource();var out=buffers.getBuffer(TYPE);var poses=event.getPoseStack();var camera=event.getCamera().getPosition();
        var left=event.getCamera().getLeftVector();var top=event.getCamera().getUpVector();Vec3 right=new Vec3(-left.x,-left.y,-left.z),up=new Vec3(top.x,top.y,top.z);
        float partial=mc.isPaused()?0:event.getPartialTick();quads=0;
        for(var e:effects){
            var s=e.state;if(s.position().distanceToSqr(camera)>256*256)continue;float age=mc.level.getGameTime()+partial-s.start(),phaseAge=mc.level.getGameTime()+partial-s.phaseStart();Vec3 p=e.position(partial),dir=s.direction();
            Vec3 side=dir.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);Vec3 vertical=side.cross(dir).normalize();
            float power=LimitlessKind.smooth(age/s.kind().charge),fade=s.phase()==2&&s.kind()!=LimitlessKind.BLUE?1-LimitlessKind.smooth(phaseAge/16):1;
            if(s.phase()==1&&mc.player!=null&&s.owner().equals(mc.player.getUUID()))fade*=.1f+.9f*LimitlessKind.smooth((float)(p.distanceTo(camera)/12));
            float[] color=COLORS[s.kind().ordinal()];double radius=s.phase()==0?.25+.65*power:s.kind()==LimitlessKind.PURPLE?4.5:s.kind()==LimitlessKind.BLUE?1.2+LimitlessKind.power(s.charge())*.6:.8;
            if(s.phase()==0&&s.kind()==LimitlessKind.PURPLE){
                double merge=LimitlessKind.smooth((age-35)/28),gap=2.6*(1-merge);Vec3 a=p.add(side.scale(gap)),b=p.subtract(side.scale(gap));
                orb(poses,out,camera,a,right,up,.65,age,COLORS[0],1);orb(poses,out,camera,b,right,up,.65,-age,COLORS[1],1);
                ribbon(poses,out,camera,a,b,up,.04,COLORS[2],.7f);
                if(merge>.3)orb(poses,out,camera,p.subtract(dir.scale(.05)),right,up,1.5*merge,age,color,(float)merge);
            }else orb(poses,out,camera,p,right,up,radius,age,color,fade);
            double orbit=s.phase()==2&&s.kind()==LimitlessKind.BLUE?s.kind().radius(s.charge()):radius*1.8;
            for(int ring=0;ring<3;ring++){
                Vec3 axis=ring==0?right:side,normal=ring==0?up:vertical;
                for(int i=0;i<40;i++){
                    double a=i*Math.PI*2/40+age*.09*(ring%2==0?1:-1),b=a+Math.PI*2/40;
                    double wobble=1+.08*Math.sin(i*1.7+age*.2);
                    Vec3 from=p.add(axis.scale(Math.cos(a)*orbit*wobble)).add(normal.scale(Math.sin(a)*orbit)),to=p.add(axis.scale(Math.cos(b)*orbit)).add(normal.scale(Math.sin(b)*orbit));
                    ribbon(poses,out,camera,from,to,dir,.035+radius*.015,color,fade*.75f);
                }
                orbit*=.85;
            }
            for(int i=0;i<32;i++){
                double a=i*2.399,cycle=((age*.028+i*.137)%1+1)%1;
                double r=s.kind()==LimitlessKind.BLUE?(.2+1-cycle)*orbit:cycle*orbit*1.7;
                Vec3 spark=p.add(side.scale(Math.cos(a)*r)).add(vertical.scale(Math.sin(a)*r)).add(dir.scale(Math.sin(i*3.1)*r*.5));
                billboard(poses,out,camera,spark,right,up,.04+radius*.025,color,fade*.8f);
            }
            if(s.phase()==1){for(int i=1;i<=18;i++){if(i*.7>phaseAge*s.kind().speed)break;Vec3 tail=p.subtract(dir.scale(i*.7));billboard(poses,out,camera,tail,right,up,radius*(1-i/20.),color,fade*(1-i/19f)*.55f);}}
            if(s.phase()==2&&s.kind()==LimitlessKind.RED){double shock=1+phaseAge*.7;for(int i=0;i<64;i++){double a=i*Math.PI*2/64,b=(i+1)*Math.PI*2/64;ribbon(poses,out,camera,p.add(side.scale(Math.cos(a)*shock)).add(vertical.scale(Math.sin(a)*shock)),p.add(side.scale(Math.cos(b)*shock)).add(vertical.scale(Math.sin(b)*shock)),dir,.13,color,fade);}}
        }
        buffers.endBatch(TYPE);
    }
    private static void orb(PoseStack p,VertexConsumer out,Vec3 camera,Vec3 center,Vec3 right,Vec3 up,double size,float age,float[] color,float alpha){
        billboard(p,out,camera,center,right,up,size*1.8,color,alpha*.55f);
        billboard(p,out,camera,center.add(camera.subtract(center).normalize().scale(.015)),right,up,size,color,alpha);
        billboard(p,out,camera,center.add(camera.subtract(center).normalize().scale(.025)),right,up,size*.38,new float[]{.9f,.9f,1},alpha*.95f);
    }
    private static void billboard(PoseStack p,VertexConsumer out,Vec3 camera,Vec3 center,Vec3 right,Vec3 up,double size,float[] color,float alpha){if(center.distanceToSqr(camera)<size*size*.5)return;quad(p,out,camera,center.subtract(right.scale(size)).subtract(up.scale(size)),center.add(right.scale(size)).subtract(up.scale(size)),center.add(right.scale(size)).add(up.scale(size)),center.subtract(right.scale(size)).add(up.scale(size)),color,alpha,false);}
    private static void ribbon(PoseStack p,VertexConsumer out,Vec3 camera,Vec3 a,Vec3 b,Vec3 normal,double width,float[] color,float alpha){var side=b.subtract(a).cross(camera.subtract(a)).normalize().scale(width);if(side.lengthSqr()<1e-9)side=normal.scale(width);quad(p,out,camera,a.subtract(side),b.subtract(side),b.add(side),a.add(side),color,alpha,true);}
    private static void quad(PoseStack p,VertexConsumer out,Vec3 camera,Vec3 a,Vec3 b,Vec3 c,Vec3 d,float[] color,float alpha,boolean ribbon){if(quads>=2400)return;quads++;vertex(p,out,a.subtract(camera),ribbon?.48f:0,1,color,alpha);vertex(p,out,b.subtract(camera),ribbon?.52f:1,1,color,alpha);vertex(p,out,c.subtract(camera),ribbon?.52f:1,0,color,alpha);vertex(p,out,d.subtract(camera),ribbon?.48f:0,0,color,alpha);}
    private static void vertex(PoseStack p,VertexConsumer out,Vec3 a,float u,float v,float[] color,float alpha){var pose=p.last();out.vertex(pose.pose(),(float)a.x,(float)a.y,(float)a.z).color(color[0],color[1],color[2],Math.max(0,alpha)).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(pose.normal(),0,1,0).endVertex();}
}
