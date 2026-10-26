package dev.unlimitedvoid.client;

import dev.unlimitedvoid.DomainSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashMap;
import java.util.UUID;

/** Three independent preloaded cues: dialogue, launch and impact; all owned by the cast. */
public final class ClientFugaAudio {
    private static final java.util.Map<UUID,Tracks> TRACKS=new LinkedHashMap<>();
    private ClientFugaAudio() {}
    private static Tracks tracks(UUID owner){
        if(!TRACKS.containsKey(owner)&&TRACKS.size()>=8)stop(TRACKS.keySet().iterator().next());
        return TRACKS.computeIfAbsent(owner,id->new Tracks());
    }
    public static void play(UUID owner,Vec3 position){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        stop(owner);
        float distanceGain=owner.equals(mc.player.getUUID())?1:(float)Math.max(0,1-mc.player.getEyePosition().distanceTo(position)/96);
        float volume=DomainSounds.gain(.95f*distanceGain);if(volume<=0)return;
        var sound=new SimpleSoundInstance(DomainSounds.FUGA_OPEN.get().getLocation(),SoundSource.VOICE,volume,1,
                RandomSource.create(),false,0,SoundInstance.Attenuation.NONE,0,0,0,true);
        tracks(owner).voice=sound;mc.getSoundManager().play(sound);
    }
    public static void launch(UUID owner,Vec3 position){
        var t=tracks(owner);if(t.launch!=null)return;
        t.launch=positional(DomainSounds.FUGA_LAUNCH.get().getLocation(),position,.95f);
    }
    public static void impact(UUID owner,Vec3 position){
        var t=tracks(owner);if(t.impact!=null)return;
        stopSound(t.launch);t.impact=positional(DomainSounds.FUGA_IMPACT.get().getLocation(),position,1);
    }
    private static SoundInstance positional(net.minecraft.resources.ResourceLocation event,Vec3 position,float volume){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return null;
        if(DomainSounds.gain(volume)<=0)return null;
        // Keep the exact instance submitted to SoundEngine so stop(owner) also stops positional cues.
        var sound=new PositionalCue(event,position,volume);
        mc.getSoundManager().play(sound);return sound;
    }
    private static void stopSound(SoundInstance sound){if(sound!=null)Minecraft.getInstance().getSoundManager().stop(sound);}
    public static void stop(UUID owner){var t=TRACKS.remove(owner);if(t!=null){stopSound(t.voice);stopSound(t.launch);stopSound(t.impact);}}
    public static void clear(){for(var t:TRACKS.values()){stopSound(t.voice);stopSound(t.launch);stopSound(t.impact);}TRACKS.clear();}
    private static final class Tracks {SoundInstance voice,launch,impact;}
    static final class PositionalCue extends SimpleSoundInstance {
        PositionalCue(net.minecraft.resources.ResourceLocation event,Vec3 p,float volume){
            super(event,SoundSource.PLAYERS,DomainSounds.gain(volume),1,RandomSource.create(),false,0,
                    SoundInstance.Attenuation.LINEAR,p.x,p.y,p.z,false);
        }
    }
}
