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

/** Preloaded single-shot dialogue, owned by the cast; cancellation/world changes stop it. */
public final class ClientFugaAudio {
    private static final java.util.Map<UUID,SoundInstance> VOICES=new LinkedHashMap<>();
    private ClientFugaAudio() {}
    public static void play(UUID owner,Vec3 position){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        stop(owner);
        VOICES.entrySet().removeIf(e->!mc.getSoundManager().isActive(e.getValue()));
        float distanceGain=owner.equals(mc.player.getUUID())?1:(float)Math.max(0,1-mc.player.getEyePosition().distanceTo(position)/96);
        float volume=DomainSounds.gain(.95f*distanceGain);if(volume<=0)return;
        if(VOICES.size()>=8)stop(VOICES.keySet().iterator().next());
        var sound=new SimpleSoundInstance(DomainSounds.FUGA_OPEN.get().getLocation(),SoundSource.VOICE,volume,1,
                RandomSource.create(),false,0,SoundInstance.Attenuation.NONE,0,0,0,true);
        VOICES.put(owner,sound);mc.getSoundManager().play(sound);
    }
    public static void stop(UUID owner){var sound=VOICES.remove(owner);if(sound!=null)Minecraft.getInstance().getSoundManager().stop(sound);}
    public static void clear(){var mc=Minecraft.getInstance();for(var sound:VOICES.values())mc.getSoundManager().stop(sound);VOICES.clear();}
}
