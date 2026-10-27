package dev.unlimitedvoid.client;

import dev.unlimitedvoid.FugaTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** ParticleEngine fire, embers and smoke with bounded spawn counts and per-cast cleanup. */
final class FugaParticles {
    static final boolean ENABLED=true;
    private static final int MAX_ALIVE=4500,MAX_SPAWN=300;
    private static final List<Puff> LIVE=new ArrayList<>();
    private static int budget;
    static int peak;
    static void clear(){LIVE.forEach(Particle::remove);LIVE.clear();peak=0;}
    static void stop(UUID owner){LIVE.removeIf(p->{if(p.owner.equals(owner)){p.remove();return true;}return false;});}
    static void begin(){LIVE.removeIf(p->!p.isAlive());budget=MAX_SPAWN;}
    static int alive(){return LIVE.size();}
    static void tick(FugaClient.Effect e,int castCount){
        var mc=Minecraft.getInstance();var level=mc.level;if(level==null)return;
        float age=e.age(level,0),burst=FugaTimeline.burstAge(age,e.flight);
        if(e.from.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition())>768*768&&e.to.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition())>768*768)return;
        int stride=Math.max(1,castCount);if(mc.options.particles().get().getId()>0)stride*=2;
        var r=level.random;
        Vec3 d=e.direction(level,0),side=d.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
        Vec3 up=side.cross(d).normalize();
        if(!e.released){
            float q=FugaTimeline.charge(age);Vec3 p=e.origin(level,0);
            if(mc.player!=null&&e.owner.equals(mc.player.getUUID())&&mc.options.getCameraType().isFirstPerson()&&mc.getCameraEntity()==mc.player)p=p.add(d.scale(2.8));
            // Short-lived particles continuously outline the bow, arrow, and inward flame streams.
            for(int i=0;i<42/stride;i++){
                double a=-1.4+r.nextDouble()*2.8;
                Vec3 bow=p.add(d.scale((.1+Math.cos(a)*.65)*3)).add(up.scale(Math.sin(a)*2.85*q));
                emit(e,bow,jitter(level,.07).add(up.scale(.06)),.10f+r.nextFloat()*.16f,12+r.nextInt(8),false);
            }
            for(int i=0;i<30/stride;i++){
                Vec3 arrow=p.add(d.scale((-1.1+r.nextDouble()*4.2)*3*q));
                emit(e,arrow.add(jitter(level,.12)),d.scale(.06).add(jitter(level,.035)),.10f+r.nextFloat()*.13f,9+r.nextInt(6),false);
            }
            var caster=level.getPlayerByUUID(e.owner);if(caster==null)return;
            Vec3 base=caster.position();
            for(int i=0;i<48/stride;i++){
                double a=r.nextDouble()*Math.PI*2,t=r.nextDouble(),rad=(1-t)*18*q;
                Vec3 point=base.add(Math.cos(a)*rad,t*t*9*q+.1,Math.sin(a)*rad);
                Vec3 velocity=p.subtract(point).normalize().scale(.22+.35*t).add(jitter(level,.08));
                emit(e,point,velocity,.10f+r.nextFloat()*.28f,16+r.nextInt(13),false);
            }
        }else if(burst<0){
            Vec3 now=e.from.lerp(e.to,FugaTimeline.travel(age,e.flight));
            Vec3 before=e.from.lerp(e.to,FugaTimeline.travel(age-1,e.flight));
            for(int i=0;i<135/stride;i++){
                Vec3 p=before.lerp(now,r.nextDouble()).add(jitter(level,.25));
                emit(e,p,d.scale(-.10).add(jitter(level,.12)),.12f+r.nextFloat()*.30f,12+r.nextInt(14),false);
            }
        }else if(burst<FugaTimeline.AFTERMATH){
            float fade=FugaTimeline.burstFade(burst);if(fade<=.02)return;
            float grow=FugaTimeline.smooth(burst/24);
            // Continuous turbulent fire stem and rolling cap; each puff moves independently.
            for(int i=0;i<155/stride;i++){
                double h=r.nextDouble()*240*grow,cap=h>170*grow?1:0;
                double radius=(cap>0?57:20)*( .35+.65*grow)*Math.sqrt(r.nextDouble());
                double angle=r.nextDouble()*Math.PI*2+burst*.045+h*.025;
                Vec3 p=e.to.add(Math.cos(angle)*radius,h,Math.sin(angle)*radius);
                Vec3 v=new Vec3(Math.cos(angle)*(.08+cap*.50),.45+r.nextDouble()*1.3,Math.sin(angle)*(.08+cap*.50));
                boolean smoke=burst>35&&r.nextFloat()<.32;
                emit(e,p,v,(smoke?6.0f:4.0f)+r.nextFloat()*(smoke?6:5),22+r.nextInt(20),smoke);
            }
            var tiles=e.ground(level);double front=FugaTimeline.fireRadius(burst);
            for(int i=0;i<80/stride;i++){
                Vec3 p=tiles[r.nextInt(tiles.length)];if(p.distanceToSqr(e.to)>front*front)continue;
                emit(e,p.add(jitter(level,5)).add(0,1,0),new Vec3(0,.3+r.nextDouble()*.7,0),2.5f+r.nextFloat()*3.5f,20+r.nextInt(14),burst>55&&r.nextBoolean());
            }
            for(int i=0;i<55/stride;i++){
                double a=r.nextDouble()*Math.PI*2,rad=Math.sqrt(r.nextDouble())*45*grow;
                Vec3 p=e.to.add(Math.cos(a)*rad,2+r.nextDouble()*10,Math.sin(a)*rad);
                double speed=burst<20?1.8:.6;
                emit(e,p,new Vec3(Math.cos(a)*speed,1+r.nextDouble()*1.6,Math.sin(a)*speed),.08f+r.nextFloat()*.14f,18+r.nextInt(22),false);
            }
        }
    }
    private static Vec3 jitter(ClientLevel level,double size){var r=level.random;return new Vec3((r.nextDouble()-.5)*size*2,(r.nextDouble()-.5)*size*2,(r.nextDouble()-.5)*size*2);}
    private static void emit(FugaClient.Effect e,Vec3 p,Vec3 v,float size,int life,boolean smoke){
        if(budget<=0||LIVE.size()>=MAX_ALIVE)return;
        var mc=Minecraft.getInstance();var texture=mc.getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
        if(!(texture instanceof TextureAtlas atlas))return;
        var particle=new Puff(mc.level,e.owner,p,v,size,life,smoke,atlas);
        budget--;LIVE.add(particle);peak=Math.max(peak,LIVE.size());mc.particleEngine.add(particle);
    }
    private static final class Puff extends TextureSheetParticle {
        final UUID owner;final float baseSize;final boolean smoke;final TextureAtlas atlas;
        Puff(ClientLevel level,UUID owner,Vec3 p,Vec3 v,float size,int life,boolean smoke,TextureAtlas atlas){
            super(level,p.x,p.y,p.z);this.owner=owner;this.baseSize=size;this.smoke=smoke;this.atlas=atlas;
            xd=v.x;yd=v.y;zd=v.z;lifetime=life;hasPhysics=false;friction=.975f;gravity=smoke?-.008f:.012f;
            quadSize=size;roll=random.nextFloat()*6.283f;oRoll=roll;setSprite(atlas.getSprite(new ResourceLocation("unlimitedvoid",smoke?"fuga_smoke":"fuga_particle")));color();
        }
        @Override public void tick(){super.tick();oRoll=roll;roll+=smoke?.012f:.055f;float t=age/(float)lifetime;
            quadSize=baseSize*(smoke?1+t*.9f:(.72f+t*.7f));alpha=(smoke?.34f:.90f)*(1-t)*(Math.min(1,age/2f));
            color();
        }
        private void color(){float t=age/(float)lifetime;if(smoke){setColor(.12f,.10f,.105f);return;}
            if(baseSize>.65f)setColor(1f,Math.max(.16f,.50f-t*.32f),.025f);
            else setColor(1f,Math.max(.22f,.98f-t*.72f),Math.max(.025f,.63f-t*.80f));
        }
        @Override protected int getLightColor(float partial){return smoke?super.getLightColor(partial):0xF000F0;}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    }
}
