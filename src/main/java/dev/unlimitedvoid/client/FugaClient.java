package dev.unlimitedvoid.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.unlimitedvoid.FugaTimeline;
import dev.unlimitedvoid.UnlimitedVoid;
import dev.unlimitedvoid.network.DomainNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class FugaClient {
    private static final KeyMapping PREVIEW=new KeyMapping("key.unlimitedvoid.fuga",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_H,"category.unlimitedvoid");
    private static final KeyMapping RESET=new KeyMapping("key.unlimitedvoid.fuga_reset",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_O,"category.unlimitedvoid");
    private static final Map<UUID,Effect> EFFECTS=new LinkedHashMap<>();
    private static ClientLevel lastLevel;
    private static int cooldown;
    private static boolean casting;
    private FugaClient() {}
    private static void clear(ClientLevel level){ClientFugaAudio.clear();EFFECTS.clear();cooldown=0;casting=false;lastLevel=level;}
    public static void status(int ticks,boolean active){var level=Minecraft.getInstance().level;if(level!=lastLevel)clear(level);cooldown=ticks;casting=active;}
    /** Negative means no bow pose; the mixin is client-only and leaves other entities alone. */
    public static float drawProgress(UUID owner){
        var mc=Minecraft.getInstance();var e=EFFECTS.get(owner);
        return mc.level==null||e==null||e.released?-1:FugaTimeline.charge(e.age(mc.level,mc.getFrameTime()));
    }
    public static void receive(UUID owner,int phase,Vec3 from,Vec3 to,int seed,int flight) {
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(lastLevel!=mc.level)clear(mc.level);
        if(phase==2){EFFECTS.remove(owner);ClientFugaAudio.stop(owner);return;}
        Effect e=EFFECTS.get(owner);
        if(phase==0||e==null){
            if(EFFECTS.size()>=FugaTimeline.MAX_CASTS){var oldest=EFFECTS.keySet().iterator().next();EFFECTS.remove(oldest);ClientFugaAudio.stop(oldest);}
            e=new Effect(owner,from,to,seed,mc.level.getGameTime());EFFECTS.put(owner,e);
            if(phase==0)ClientFugaAudio.play(owner,from);
        }
        if(phase==1){e.from=from;e.to=to;e.flight=flight;e.released=true;e.start=mc.level.getGameTime()-FugaTimeline.CHARGE;}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.level!=lastLevel)clear(mc.level);
        if(mc.level==null||mc.player==null){clear(mc.level);return;}
        while(PREVIEW.consumeClick())if(mc.screen==null&&!mc.isPaused())DomainNetwork.requestFuga();
        while(RESET.consumeClick())if(mc.screen==null&&!mc.isPaused())DomainNetwork.resetFuga();
        if(mc.isPaused())return;
        if(cooldown>0)cooldown--;
        EFFECTS.values().removeIf(e->e.age(mc.level,0)>FugaTimeline.CHARGE+e.flight+FugaTimeline.AFTERMATH+20);
        for(Effect e:EFFECTS.values()){
            float age=e.age(mc.level,0),burst=FugaTimeline.burstAge(age,e.flight);
            if(e.lastParticleTick==mc.level.getGameTime())continue;
            e.lastParticleTick=mc.level.getGameTime();
            if(e.released&&burst<0){
                Vec3 p=e.from.lerp(e.to,FugaTimeline.travel(age,e.flight));
                mc.level.addParticle(ParticleTypes.FLAME,p.x,p.y,p.z,0,.012,0);
            }else if(e.released&&burst>=0&&burst<30&&((int)burst)%3==0){
                var r=mc.level.random;
                for(int i=0;i<5;i++){
                    double angle=r.nextDouble()*Math.PI*2,rad=.3+r.nextDouble()*2;
                    mc.level.addParticle(burst<12?ParticleTypes.FLAME:ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            e.to.x+Math.cos(angle)*rad,e.to.y+.4+r.nextDouble()*2,e.to.z+Math.sin(angle)*rad,
                            Math.cos(angle)*.06,.05+r.nextDouble()*.08,Math.sin(angle)*.06);
                }
            }
        }
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.options.hideGui)return;
        Component state=Component.translatable(casting?"hud.unlimitedvoid.fuga_casting":cooldown>0?"hud.unlimitedvoid.fuga_cooldown":"hud.unlimitedvoid.fuga_ready",(cooldown+19)/20);
        event.getGuiGraphics().drawString(mc.font,Component.translatable("hud.unlimitedvoid.fuga",PREVIEW.getTranslatedKeyMessage(),state,RESET.getTranslatedKeyMessage()),8,68,0xFFC381,true);
    }
    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles event){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.isPaused())return;
        double shake=0;
        for(var e:EFFECTS.values())if(e.released){
            float burst=FugaTimeline.burstAge(e.age(mc.level,mc.getFrameTime()),e.flight);
            if(burst>=0&&burst<54){
                double distance=e.to.distanceTo(mc.gameRenderer.getMainCamera().getPosition());
                double arrival=distance/FugaTimeline.SHOCK_RADIUS*44;
                double wave=Math.max(0,1-Math.abs(burst-arrival-3)/10);
                shake=Math.max(shake,wave*Math.max(0,1-distance/(FugaTimeline.SHOCK_RADIUS+64))*1.15);
            }
        }
        // Respect the vanilla reduced-screen-motion preference; no random displacement or flash.
        shake*=mc.options.screenEffectScale().get();
        float time=mc.level.getGameTime()+mc.getFrameTime();
        event.setPitch(event.getPitch()+(float)(Math.sin(time*2.1)*shake));
        event.setRoll(event.getRoll()+(float)(Math.sin(time*1.7)*shake*.6));
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||EFFECTS.isEmpty())return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        FugaRenderer.render(event,EFFECTS.values());
    }
    static final class Effect {
        final UUID owner;final int seed;Vec3 from,to;long start,lastParticleTick=Long.MIN_VALUE;int flight=1;boolean released;
        private Vec3[] ground;
        Effect(UUID owner,Vec3 from,Vec3 to,int seed,long start){this.owner=owner;this.from=from;this.to=to;this.seed=seed;this.start=start;}
        float age(ClientLevel level,float partial){return Math.max(0,level.getGameTime()-start+partial);}
        Vec3[] ground(ClientLevel level){
            if(ground!=null)return ground;
            ground=new Vec3[320];
            // One bounded sampling pass. Unloaded terrain uses the impact plane without loading chunks.
            for(int i=0;i<ground.length;i++){
                double angle=i*2.3999632297+(seed&255)*.0245;
                double radius=FugaTimeline.FIRE_RADIUS*Math.sqrt((i+.5)/ground.length);
                double x=to.x+Math.cos(angle)*radius,z=to.z+Math.sin(angle)*radius,y=to.y;
                if(level.hasChunkAt(BlockPos.containing(x,y,z)))y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(int)Math.floor(x),(int)Math.floor(z));
                ground[i]=new Vec3(x,y+.12,z);
            }
            return ground;
        }
        Vec3 origin(ClientLevel level,float partial){
            if(released)return from;
            var p=level.getPlayerByUUID(owner);if(p==null)return from;
            Vec3 d=p.getViewVector(partial),right=d.cross(new Vec3(0,1,0));
            if(right.lengthSqr()<.001)right=new Vec3(1,0,0);else right=right.normalize();
            return p.getEyePosition(partial).add(d.scale(1.2)).add(right.scale(.25)).add(0,-.22,0);
        }
        Vec3 direction(ClientLevel level,float partial){
            if(!released){var p=level.getPlayerByUUID(owner);if(p!=null)return p.getViewVector(partial);}
            Vec3 d=to.subtract(from);return d.lengthSqr()<.00001?new Vec3(0,0,1):d.normalize();
        }
    }
    @Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event){event.register(PREVIEW);event.register(RESET);}
    }
}
