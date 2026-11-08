package dev.unlimitedvoid;
import net.minecraft.world.effect.*;
import net.minecraftforge.registries.*;
/** Soul perception is a timed, synchronized vanilla effect rather than an always-on free damage bypass. */
public final class CursedToolEffects {
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,UnlimitedVoid.ID);
    public static final RegistryObject<MobEffect> SOUL_SIGHT=EFFECTS.register("soul_sight",()->new SoulSight());
    private static final class SoulSight extends MobEffect { SoulSight(){super(MobEffectCategory.BENEFICIAL,0x9be8e8);} }
}
