package dev.unlimitedvoid.client;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.item.CursedToolItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class CursedToolModels {
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->ItemProperties.register(DomainItems.PLAYFUL_CLOUD.get(),new ResourceLocation(UnlimitedVoid.ID,"cloud_form"),(stack,level,entity,seed)->CursedToolItem.cloudForm(stack)));}
}
