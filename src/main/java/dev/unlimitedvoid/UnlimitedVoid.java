package dev.unlimitedvoid;

import net.minecraftforge.fml.common.Mod;
import dev.unlimitedvoid.network.DomainNetwork;

@Mod(UnlimitedVoid.ID)
public final class UnlimitedVoid {
    public static final String ID = "unlimitedvoid";
    public UnlimitedVoid() {
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, DomainConfig.SPEC);
        DomainSounds.EVENTS.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        DomainEntities.TYPES.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        DomainNetwork.register();
    }
}
