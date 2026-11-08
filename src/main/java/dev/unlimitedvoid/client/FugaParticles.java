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
    private static final List<Puff> LIVE=new ArrayList<>();
    private static int budget;
    static int peak;
    static void clear(){LIVE.forEach(Particle::remove);LIVE.clear();peak=0;}
    static void stop(UUID owner){LIVE.removeIf(p->{if(p.owner.equals(owner)){p.remove();return true;}return false;});}
    static void begin(){LIVE.removeIf(p->!p.isAlive());while(LIVE.size()>ClientQuality.aliveParticles())LIVE.remove(LIVE.size()-1).remove();budget=ClientQuality.particlesPerTick();}
    static int alive(){return LIVE.size();}
    static void tick(FugaClient.Effect e,int castCount){
        var mc=Minecraft.getInstance();var level=mc.level;if(level==null)return;
        float age=e.age(level,0),burst=FugaTimeline.burstAge(age,e.flight);
        if(e.from.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition())>ClientQuality.distance()*ClientQuality.distance()&&e.to.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition())>ClientQuality.distance()*ClientQuality.distance())return;
        int stride=Math.max(1,castCount);if(mc.options.particles().get().getId()>0)stride*=2;
        var r=level.random;
        Vec3 d=e.direction(level,0),side=d.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
        Vec3 up=side.cross(d).normalize();
        if(!e.released){
            stride*=Math.max(1,(240+ClientQuality.particlesPerTick()-1)/ClientQuality.particlesPerTick());
            float q=FugaTimeline.charge(age);Vec3 p=e.origin(level,0);
            if(mc.player!=null&&e.owner.equals(mc.player.getUUID())&&mc.options.getCameraType().isFirstPerson()&&mc.getCameraEntity()==mc.player)p=p.add(d.scale(1.4));
            // Oversized bow with a stable incandescent spine and short flames streaming along its limbs.
            // Keep the silhouette legible: the old 18-block gathering cloud obscured the actual weapon.
            for(int i=0;i<88/stride;i++){
                double a=-1.4+r.nextDouble()*2.8;
                Vec3 bow=p.add(d.scale((.20+Math.cos(a)*.85)*q)).add(up.scale(Math.sin(a)*3.5*q)).add(side.scale((Math.cos(a)-1)*1.25*q));
                Vec3 tangent=up.scale(Math.cos(a)).add(d.scale(-Math.sin(a)*.24)).normalize();
                boolean core=i%3==0;
                Vec3 flame=tangent.scale(Math.copySign(.07,a)).add(side.scale(Math.sin(age*.25+a*5)*.025));
                emitKind(e,bow.add(jitter(level,core?.012:.055)),core?Vec3.ZERO:flame,
                        core?.055f:.10f+r.nextFloat()*.09f,core?6:5+r.nextInt(4),core?2:0);
            }
            for(int i=0;i<46/stride;i++){
                Vec3 arrow=p.add(d.scale((-.7+r.nextDouble()*5.5)*q));
                emitKind(e,arrow.add(jitter(level,.025)),d.scale(.025),i%2==0?.045f:.09f,6+r.nextInt(3),i%2==0?2:0);
            }
            for(int i=0;i<40/stride;i++){
                double t=r.nextDouble()*2-1;
                Vec3 string=p.add(up.scale(t*3.45*q)).add(d.scale((.345-1.05*(1-Math.abs(t))*q)*q)).add(side.scale(-1.04*Math.abs(t)*q));
                emitKind(e,string,jitter(level,.008),.035f+r.nextFloat()*.015f,5+r.nextInt(3),2);
            }
            Vec3 tip=p.add(d.scale(4.8*q));
            for(int i=0;i<30/stride;i++){
                double t=r.nextDouble(),sign=i%2==0?1:-1;
                Vec3 head=tip.add(d.scale(-t*.95*q)).add((i%4<2?up:side).scale(sign*t*.43*q));
                emitKind(e,head,jitter(level,.015),.07f+r.nextFloat()*.035f,5+r.nextInt(3),i%3==0?2:0);
            }
            // Small, directed wisps feed the grip; no body-sized random particle storm.
            for(int i=0;i<24/stride;i++){
                double a=r.nextDouble()*Math.PI*2,rad=.5+r.nextDouble()*1.4;
                Vec3 point=p.add(side.scale(Math.cos(a)*rad)).add(up.scale(Math.sin(a)*rad*.6)).add(d.scale(-.6+r.nextDouble()));
                Vec3 velocity=p.subtract(point).normalize().scale(.12+.10*q).add(jitter(level,.02));
                emit(e,point,velocity,.04f+r.nextFloat()*.05f,6+r.nextInt(5),false);
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
            // Allocate every layer even on economy mode, instead of spending its budget on the stem.
            stride*=Math.max(1,(300+ClientQuality.particlesPerTick()-1)/ClientQuality.particlesPerTick());
            float rise=FugaTimeline.smooth(burst/34),spread=FugaTimeline.smooth((burst-8)/42);
            double height=FugaTimeline.COLUMN_HEIGHT*rise;
            // Brief white-hot ignition grows into a fireball, then gives way to the ascending column.
            if(burst<18)for(int i=0;i<36/stride;i++){
                double a=r.nextDouble()*Math.PI*2,z=r.nextDouble()*2-1,rad=(8+burst*4)*Math.cbrt(r.nextDouble());
                double horizontal=Math.sqrt(1-z*z);
                emitKind(e,e.to.add(Math.cos(a)*horizontal*rad,Math.abs(z)*rad,Math.sin(a)*horizontal*rad),
                        new Vec3(Math.cos(a)*1.3,1.1,Math.sin(a)*1.3),14+r.nextFloat()*17,10+r.nextInt(5),2);
            }
            // Helical hot stem: a dense, narrow upward jet with a darker turbulent sheath.
            for(int i=0;i<54/stride;i++){
                double h=r.nextDouble()*height,a=r.nextDouble()*Math.PI*2+h*.055-burst*.12;
                boolean skin=r.nextFloat()<.24;double radius=(skin?28:17)*(.55+.45*rise)*Math.sqrt(r.nextDouble());
                emitKind(e,e.to.add(Math.cos(a)*radius,h,Math.sin(a)*radius),
                        new Vec3(-Math.sin(a)*.45,1.5+r.nextDouble()*2,Math.cos(a)*.45),
                        (skin?16:14)+r.nextFloat()*12,16+r.nextInt(9),skin?1:(r.nextFloat()<.5?2:0));
            }
            // Outward rolling torus. Its incandescent underside and ash skin give the crown depth.
            if(burst>7)for(int i=0;i<62/stride;i++){
                double a=r.nextDouble()*Math.PI*2,b=r.nextDouble()*Math.PI*2;
                double tube=24+19*spread,ring=35+70*spread,rad=ring+Math.cos(b)*tube;
                boolean skin=Math.sin(b)>.18&&r.nextFloat()<.78;
                emitKind(e,e.to.add(Math.cos(a)*rad,height+Math.sin(b)*tube,Math.sin(a)*rad),
                        new Vec3(Math.cos(a)*1.35,-Math.cos(b)*.8+.4,Math.sin(a)*1.35),
                        17+r.nextFloat()*17,18+r.nextInt(9),skin?1:(r.nextFloat()<.45?2:0));
            }
            // Ignition propagates across actual terrain; no replacement blocks or terrain writes.
            var tiles=e.ground(level);double front=FugaTimeline.fireRadius(burst);
            for(int i=0;i<34/stride;i++){
                Vec3 p=tiles[r.nextInt(tiles.length)];if(p.distanceToSqr(e.to)>front*front)continue;
                emitKind(e,p.add(jitter(level,6)).add(0,3,0),new Vec3(0,.9+r.nextDouble(),0),
                        8+r.nextFloat()*10,16+r.nextInt(8),burst>58&&r.nextBoolean()?1:0);
            }
            // Two travelling pressure fronts roll dust out over the full blast footprint.
            for(int i=0;i<42/stride;i++){
                float wave=burst-(i%2)*9;if(wave<0||wave>65)continue;
                double radius=FugaTimeline.SHOCK_RADIUS*FugaTimeline.smooth(wave/55);
                double a=r.nextDouble()*Math.PI*2;
                emitKind(e,e.to.add(Math.cos(a)*radius,3+r.nextDouble()*8,Math.sin(a)*radius),
                        new Vec3(Math.cos(a)*2.4,.25,Math.sin(a)*2.4),10+r.nextFloat()*12,12+r.nextInt(7),wave<15?2:3);
            }
            for(int i=0;i<20/stride;i++){
                double a=r.nextDouble()*Math.PI*2,rad=Math.sqrt(r.nextDouble())*80*rise;
                emit(e,e.to.add(Math.cos(a)*rad,3+r.nextDouble()*height,Math.sin(a)*rad),
                        new Vec3(Math.cos(a)*1.5,1+r.nextDouble()*2,Math.sin(a)*1.5),.20f+r.nextFloat()*.45f,15+r.nextInt(12),false);
            }
        }
    }
    private static Vec3 jitter(ClientLevel level,double size){var r=level.random;return new Vec3((r.nextDouble()-.5)*size*2,(r.nextDouble()-.5)*size*2,(r.nextDouble()-.5)*size*2);}
    private static void emit(FugaClient.Effect e,Vec3 p,Vec3 v,float size,int life,boolean smoke){
        emitKind(e,p,v,size,life,smoke?1:0);
    }
    private static void emitKind(FugaClient.Effect e,Vec3 p,Vec3 v,float size,int life,int kind){
        if(budget<=0||LIVE.size()>=ClientQuality.aliveParticles())return;
        var mc=Minecraft.getInstance();var texture=mc.getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
        if(!(texture instanceof TextureAtlas atlas))return;
        float opacity=1;
        if(e.released){
            float burst=FugaTimeline.burstAge(e.age(mc.level,0),e.flight);
            if(burst>=0){opacity=FugaTimeline.burstFade(burst);life=Math.min(life,Math.max(1,(int)(FugaTimeline.AFTERMATH-burst)));}
        }
        var particle=new Puff(mc.level,e.owner,p,v,size,life,kind,opacity,atlas);
        budget--;LIVE.add(particle);peak=Math.max(peak,LIVE.size());mc.particleEngine.add(particle);
    }
    private static final class Puff extends TextureSheetParticle {
        final UUID owner;final float baseSize,opacity;final boolean smoke;final int kind;
        Puff(ClientLevel level,UUID owner,Vec3 p,Vec3 v,float size,int life,int kind,float opacity,TextureAtlas atlas){
            super(level,p.x,p.y,p.z);this.owner=owner;this.baseSize=size;this.kind=kind;this.opacity=opacity;this.smoke=kind==1||kind==3;
            xd=v.x;yd=v.y;zd=v.z;lifetime=life;hasPhysics=false;friction=.975f;gravity=smoke?-.008f:.012f;
            alpha=0;quadSize=size;roll=random.nextFloat()*6.283f;oRoll=roll;setSprite(atlas.getSprite(new ResourceLocation("unlimitedvoid",smoke?"fuga_smoke":"fuga_particle")));color();
        }
        @Override public void tick(){super.tick();oRoll=roll;roll+=smoke?.012f:.055f;float t=age/(float)lifetime;
            quadSize=baseSize*(smoke?1+t*.65f:(.80f+t*.55f));
            alpha=opacity*(smoke?.85f:1f)*Math.min(1,(1-t)*3)*Math.min(1,age/2f);
            color();
        }
        private void color(){float t=age/(float)lifetime;
            if(kind==1){setColor(.095f,.065f,.055f);return;}
            if(kind==3){setColor(.36f,.26f,.18f);return;}
            if(kind==2){setColor(1f,.92f-t*.35f,.66f-t*.55f);return;}
            if(baseSize>.65f)setColor(1f,Math.max(.16f,.63f-t*.40f),.025f);
            else setColor(1f,Math.max(.22f,.98f-t*.72f),Math.max(.025f,.63f-t*.80f));
        }
        @Override protected int getLightColor(float partial){return smoke?super.getLightColor(partial):0xF000F0;}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    }
}
