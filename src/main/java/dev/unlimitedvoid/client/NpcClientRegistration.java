package dev.unlimitedvoid.client;
import dev.unlimitedvoid.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Keep event registration independent of the renderer superclass initialization. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class NpcClientRegistration {
    @SubscribeEvent public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event){
        event.registerLayerDefinition(JujutsuNpcRenderer.GOJO,()->JujutsuNpcRenderer.layer(true));
        event.registerLayerDefinition(JujutsuNpcRenderer.SUKUNA,()->JujutsuNpcRenderer.layer(false));
    }
    @SubscribeEvent public static void registerNpcRenderers(EntityRenderersEvent.RegisterRenderers event){
        com.mojang.logging.LogUtils.getLogger().info("NPC renderers registering");
        event.registerEntityRenderer(DomainEntities.STUDENT_GOJO.get(),c->new JujutsuNpcRenderer(c,true));
        event.registerEntityRenderer(DomainEntities.SUKUNA_16.get(),c->new JujutsuNpcRenderer(c,false));
    }
}
