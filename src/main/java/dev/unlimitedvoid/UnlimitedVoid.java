package dev.unlimitedvoid;

import net.minecraftforge.fml.common.Mod;
import dev.unlimitedvoid.network.DomainNetwork;

@Mod(UnlimitedVoid.ID)
public final class UnlimitedVoid {
    public static final String ID = "unlimitedvoid";
    public UnlimitedVoid() {
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, DomainConfig.SPEC);
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT,dev.unlimitedvoid.client.ClientQuality.SPEC));
        DomainSounds.EVENTS.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        DomainEntities.TYPES.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        DomainItems.ITEMS.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        CursedToolEffects.EFFECTS.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        DomainItems.TABS.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        dev.unlimitedvoid.network.NpcControlNetwork.register();
        dev.unlimitedvoid.network.ProgressionNetwork.register();
        DomainNetwork.register();
        dev.unlimitedvoid.network.LimitlessNetwork.register();
    }
}
