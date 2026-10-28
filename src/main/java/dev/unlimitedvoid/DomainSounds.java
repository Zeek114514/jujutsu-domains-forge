package dev.unlimitedvoid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.*;

public final class DomainSounds {
    public static final DeferredRegister<SoundEvent> EVENTS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,UnlimitedVoid.ID);
    public static final RegistryObject<SoundEvent> VOID_EXPAND=event("void_expand");
    public static final RegistryObject<SoundEvent> SHRINE_EXPAND=event("shrine_expand");
    public static final RegistryObject<SoundEvent> FUGA_OPEN=event("fuga_open");
    public static final RegistryObject<SoundEvent> FUGA_LAUNCH=event("fuga_launch");
    public static final RegistryObject<SoundEvent> FUGA_IMPACT=event("fuga_impact");
    public static final RegistryObject<SoundEvent> SLASH_WIND=event("slash_wind");
    public static final RegistryObject<SoundEvent> SLASH_HIT=event("slash_hit");
    public static final RegistryObject<SoundEvent> COLLAPSE=event("domain_collapse");
    public static final RegistryObject<SoundEvent> SHELL_BREAK=event("shell_break");
    public static final RegistryObject<SoundEvent> VOID_AMBIENCE=event("void_ambience");
    public static final RegistryObject<SoundEvent> SHRINE_AMBIENCE=event("shrine_ambience");
    public static final RegistryObject<SoundEvent> MAHORAGA_TURN=event("mahoraga_turn");
    private static RegistryObject<SoundEvent> event(String name) {
        return EVENTS.register(name,()->SoundEvent.createVariableRangeEvent(new ResourceLocation(UnlimitedVoid.ID,name)));
    }
    public static float gain(float base) { return base*DomainConfig.SOUND_VOLUME.get().floatValue(); }
}
