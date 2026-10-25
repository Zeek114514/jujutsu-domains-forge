package dev.unlimitedvoid.client;

import dev.unlimitedvoid.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import java.util.List;
import java.util.UUID;
import dev.unlimitedvoid.network.DomainNetwork.DomainState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;

/** One ambience stream per client; wind at most once per six client ticks. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class ClientDomainAudio {
    private static Loop loop;
    private static UUID domain;
    private static int windTicks;
    private static SoundInstance expansion;
    private static final java.util.Map<UUID,SoundInstance> voices=new java.util.HashMap<>();
    private static int voiceUntil;
    public static boolean speaking(){return Minecraft.getInstance().player!=null && Minecraft.getInstance().player.tickCount<voiceUntil;}
    public static void playExpansion(UUID caster,DomainKind kind,net.minecraft.world.phys.Vec3 center,float radius){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || mc.level==null || !Float.isFinite(radius) || radius<1 || radius>200 || !Double.isFinite(center.lengthSqr()))return;
        voices.entrySet().removeIf(e->!mc.getSoundManager().isActive(e.getValue()));
        SoundInstance previous=voices.remove(caster);
        if(previous!=null)mc.getSoundManager().stop(previous);
        float volume=DomainSounds.gain(caster.equals(mc.player.getUUID())?1.0f:0.75f);
        if(volume<=0)return;
        expansion=new SimpleSoundInstance((kind==DomainKind.VOID?DomainSounds.VOID_EXPAND:DomainSounds.SHRINE_EXPAND).get().getLocation(),
                SoundSource.VOICE,volume,1,RandomSource.create(),false,0,SoundInstance.Attenuation.NONE,0,0,0,true);
        voiceUntil=mc.player.tickCount+DomainTimeline.chant(kind);
        mc.getSoundManager().play(expansion);
        voices.put(caster,expansion);
    }

    @SubscribeEvent public static void gain(PlaySoundEvent event) {
        SoundInstance sound=event.getSound();
        if(sound==null || !sound.getLocation().getNamespace().equals(UnlimitedVoid.ID)) return;
        float gain=DomainConfig.SOUND_VOLUME.get().floatValue();
        if(gain<=0) {event.setSound(null);return;}
        // Locally generated relative sounds already apply the gain. Server
        // positional cues receive each listener's own gain here.
        if(sound.isRelative() || sound instanceof AbstractTickableSoundInstance) return;
        event.setSound(new GainSound(sound,gain));
    }
    /** Defer volume access until SoundEngine resolves the underlying sample. */
    private record GainSound(SoundInstance base,float gain) implements SoundInstance {
        public net.minecraft.resources.ResourceLocation getLocation(){return base.getLocation();}
        public net.minecraft.client.sounds.WeighedSoundEvents resolve(net.minecraft.client.sounds.SoundManager manager){return base.resolve(manager);}
        public net.minecraft.client.resources.sounds.Sound getSound(){return base.getSound();}
        public SoundSource getSource(){return base.getSource();}
        public boolean isLooping(){return base.isLooping();}
        public boolean isRelative(){return base.isRelative();}
        public int getDelay(){return base.getDelay();}
        public float getVolume(){return base.getVolume()*gain;}
        public float getPitch(){return base.getPitch();}
        public double getX(){return base.getX();}
        public double getY(){return base.getY();}
        public double getZ(){return base.getZ();}
        public Attenuation getAttenuation(){return base.getAttenuation();}
        public boolean canStartSilent(){return base.canStartSilent();}
        public boolean canPlaySound(){return base.canPlaySound();}
    }

    public static boolean hasLoop() { return loop!=null && !loop.isStopped(); }
    public static void clear() {
        if(loop!=null) {loop.finish();Minecraft.getInstance().getSoundManager().stop(loop);}
        if(expansion!=null)Minecraft.getInstance().getSoundManager().stop(expansion);
        for(SoundInstance voice:voices.values())Minecraft.getInstance().getSoundManager().stop(voice);
        voices.clear();
        expansion=null;voiceUntil=0;loop=null;domain=null;windTicks=0;
    }
    public static void tick(List<DomainState> states) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || mc.level==null) {clear();return;}
        DomainState inside=null;
        for(var state:states) if(mc.player.getEyePosition().distanceToSqr(state.center())<state.radius()*state.radius()) {
            if(inside==null || state.kind()==DomainKind.VOID) inside=state;
        }
        if(loop!=null && loop.isStopped()) loop=null;
        boolean enabled=DomainConfig.DOMAIN_AMBIENCE.get() && DomainConfig.SOUND_VOLUME.get()>0;
        if(inside==null) {
            if(loop!=null)loop.fading=true;
            domain=null;windTicks=0;return;
        }
        if(loop!=null && (loop.kind!=inside.kind() || !inside.owner().equals(domain))) {
            mc.getSoundManager().stop(loop);loop.finish();loop=null;
        }
        domain=inside.owner();
        if(enabled && loop==null) {
            loop=new Loop(inside.kind());mc.getSoundManager().play(loop);
        }
        if(loop!=null) loop.fading=!enabled;
        if(inside.kind()==DomainKind.SHRINE && inside.closing()<0 && inside.age()>=DomainTimeline.formed(inside.kind()) && DomainConfig.SOUND_VOLUME.get()>0 && ++windTicks>=6) {
            windTicks=0;
            // These are the audible nearby field slashes. Individual victims add
            // spatial impact sounds, capped separately by the logical server.
            mc.getSoundManager().play(new SimpleSoundInstance(DomainSounds.SLASH_WIND.get().getLocation(),SoundSource.PLAYERS,
                    DomainSounds.gain(speaking()?0.09f:0.38f),0.95f+mc.level.random.nextFloat()*0.10f,RandomSource.create(),false,0,
                    SoundInstance.Attenuation.NONE,0,0,0,true));
        }
    }
    private static final class Loop extends AbstractTickableSoundInstance {
        final DomainKind kind;
        boolean fading;
        Loop(DomainKind kind) {
            super((kind==DomainKind.VOID?DomainSounds.VOID_AMBIENCE:DomainSounds.SHRINE_AMBIENCE).get(),SoundSource.AMBIENT,RandomSource.create());
            this.kind=kind;looping=true;delay=0;relative=true;attenuation=Attenuation.NONE;volume=0.001f;pitch=1;
        }
        public void finish(){stop();}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick() {
            Minecraft mc=Minecraft.getInstance();
            if(mc.level==null || mc.player==null) {stop();return;}
            float target=fading?0:DomainSounds.gain(kind==DomainKind.VOID?0.30f:0.24f);
            if(speaking())target*=0.25f;
            volume+=(target-volume)*0.15f;
            if(fading && volume<0.003f)stop();
        }
    }
}
