package dev.unlimitedvoid;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class FugaBalanceMigration {
    @SubscribeEvent public static void loaded(ModConfigEvent.Loading event){
        if(event.getConfig().getSpec()!=DomainConfig.SPEC||DomainConfig.FUGA_BALANCE_REVISION.get()!=0)return;
        if(DomainConfig.FUGA_RADIUS.get()==16&&DomainConfig.FUGA_DAMAGE.get()==32&&DomainConfig.FUGA_DUST_DAMAGE.get()==80){
            DomainConfig.FUGA_RADIUS.set(200.0);DomainConfig.FUGA_DAMAGE.set(240.0);DomainConfig.FUGA_DUST_DAMAGE.set(480.0);
        }
        DomainConfig.FUGA_BALANCE_REVISION.set(1);DomainConfig.SPEC.save();
    }
}
