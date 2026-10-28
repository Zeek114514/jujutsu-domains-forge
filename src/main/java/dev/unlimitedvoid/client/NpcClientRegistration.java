package dev.unlimitedvoid.client;
import dev.unlimitedvoid.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Keep event registration independent of the renderer superclass initialization. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class NpcClientRegistration {
    @SubscribeEvent public static void registerNpcRenderers(EntityRenderersEvent.RegisterRenderers event){
        com.mojang.logging.LogUtils.getLogger().info("NPC renderers registering");
        event.registerEntityRenderer(DomainEntities.AWAKENED_GOJO.get(),c->new JujutsuNpcRenderer(c,true));
        event.registerEntityRenderer(DomainEntities.STUDENT_GOJO.get(),c->new JujutsuNpcRenderer(c,true));
        event.registerEntityRenderer(DomainEntities.SUKUNA_16.get(),c->new JujutsuNpcRenderer(c,false));
        event.registerEntityRenderer(DomainEntities.SHINJUKU_GOJO.get(),c->new JujutsuNpcRenderer(c,true));
        event.registerEntityRenderer(DomainEntities.MEGUMI_SUKUNA.get(),c->new JujutsuNpcRenderer(c,false));
        event.registerEntityRenderer(DomainEntities.TRUE_SUKUNA.get(),c->new JujutsuNpcRenderer(c,false));
    }
}
