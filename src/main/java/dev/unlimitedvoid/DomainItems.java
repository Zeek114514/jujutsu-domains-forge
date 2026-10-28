package dev.unlimitedvoid;

import net.minecraft.world.item.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class DomainItems {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,UnlimitedVoid.ID);
    public static final RegistryObject<Item> STUDENT_GOJO=ITEMS.register("student_gojo_spawn_egg",()->new ForgeSpawnEggItem(DomainEntities.STUDENT_GOJO,0x182033,0xf0f3ff,new Item.Properties()));
    public static final RegistryObject<Item> SUKUNA_16=ITEMS.register("sukuna_16_spawn_egg",()->new ForgeSpawnEggItem(DomainEntities.SUKUNA_16,0x951f32,0xd99b95,new Item.Properties()));
    public static final RegistryObject<Item> BLINDFOLD=ITEMS.register("blindfold",()->new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> SUKUNA_FINGER=ITEMS.register("sukuna_finger",()->new Item(new Item.Properties().stacksTo(20)));
    @SubscribeEvent public static void creative(BuildCreativeModeTabContentsEvent event){
        if(event.getTabKey()==CreativeModeTabs.INGREDIENTS){event.accept(BLINDFOLD);event.accept(SUKUNA_FINGER);}
        if(event.getTabKey()==CreativeModeTabs.SPAWN_EGGS){event.accept(STUDENT_GOJO);event.accept(SUKUNA_16);}
    }
}
