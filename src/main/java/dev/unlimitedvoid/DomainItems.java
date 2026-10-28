package dev.unlimitedvoid;

import net.minecraft.world.item.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class DomainItems {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,UnlimitedVoid.ID);
    public static final RegistryObject<Item> STUDENT_GOJO=ITEMS.register("student_gojo_spawn_egg",()->new ForgeSpawnEggItem(DomainEntities.STUDENT_GOJO,0x182033,0xf0f3ff,new Item.Properties()));
    public static final RegistryObject<Item> SUKUNA_16=ITEMS.register("sukuna_16_spawn_egg",()->new ForgeSpawnEggItem(DomainEntities.SUKUNA_16,0x951f32,0xd99b95,new Item.Properties()));
    public static final RegistryObject<Item> SHINJUKU_GOJO=ITEMS.register("shinjuku_gojo_spawn_egg",()->new ForgeSpawnEggItem(DomainEntities.SHINJUKU_GOJO,0x20252c,0xdde3f3,new Item.Properties()));
    public static final RegistryObject<Item> TRUE_SUKUNA=ITEMS.register("true_sukuna_spawn_egg",()->new ForgeSpawnEggItem(DomainEntities.TRUE_SUKUNA,0xe2ad93,0x542f3b,new Item.Properties()));
    public static final RegistryObject<Item> AWAKENED_GOJO=ITEMS.register("awakened_gojo_spawn_egg",()->new ForgeSpawnEggItem(DomainEntities.AWAKENED_GOJO,0xf5f1f8,0x7185c2,new Item.Properties()));
    public static final RegistryObject<Item> MEGUMI_SUKUNA=ITEMS.register("megumi_sukuna_spawn_egg",()->new ForgeSpawnEggItem(DomainEntities.MEGUMI_SUKUNA,0x171b23,0xe5d9cf,new Item.Properties()));
    public static final RegistryObject<Item> BLINDFOLD=ITEMS.register("blindfold",()->new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> SUKUNA_FINGER=ITEMS.register("sukuna_finger",()->new Item(new Item.Properties().stacksTo(20)));
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,UnlimitedVoid.ID);
    public static final RegistryObject<CreativeModeTab> JUJUTSU_TAB=TABS.register("jujutsu_domains",()->CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.unlimitedvoid.jujutsu_domains"))
        .icon(()->new ItemStack(BLINDFOLD.get()))
        .displayItems((parameters,output)->{output.accept(STUDENT_GOJO.get());output.accept(AWAKENED_GOJO.get());output.accept(SHINJUKU_GOJO.get());output.accept(SUKUNA_16.get());output.accept(MEGUMI_SUKUNA.get());output.accept(TRUE_SUKUNA.get());output.accept(BLINDFOLD.get());output.accept(SUKUNA_FINGER.get());}).build());
}
