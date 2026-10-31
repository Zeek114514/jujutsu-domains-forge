package dev.unlimitedvoid;

import dev.unlimitedvoid.entity.ShrineEntity;
import dev.unlimitedvoid.entity.JujutsuNpc;
import net.minecraft.world.entity.*;
import net.minecraftforge.registries.*;

public final class DomainEntities {
    public static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,UnlimitedVoid.ID);
    public static final RegistryObject<EntityType<dev.unlimitedvoid.entity.InnocentSunDog>> INNOCENT_SUN_DOG=TYPES.register("innocent_sun_dog",()->EntityType.Builder.<dev.unlimitedvoid.entity.InnocentSunDog>of(dev.unlimitedvoid.entity.InnocentSunDog::new,MobCategory.CREATURE).sized(.6f,1.8f).clientTrackingRange(10).updateInterval(3).build(UnlimitedVoid.ID+":innocent_sun_dog"));
    public static final RegistryObject<EntityType<dev.unlimitedvoid.entity.InnocentSunDog>> SUN_DOG_SERVANT=TYPES.register("sun_dog_servant",()->EntityType.Builder.<dev.unlimitedvoid.entity.InnocentSunDog>of(dev.unlimitedvoid.entity.InnocentSunDog::new,MobCategory.CREATURE).sized(.6f,1.8f).clientTrackingRange(10).updateInterval(3).build(UnlimitedVoid.ID+":sun_dog_servant"));
    public static final RegistryObject<EntityType<ShrineEntity>> SHRINE=TYPES.register("shrine",
            ()->EntityType.Builder.<ShrineEntity>of(ShrineEntity::new,MobCategory.MISC).sized(18,11).noSave()
                    .clientTrackingRange(16).updateInterval(200).build("unlimitedvoid:shrine"));
    public static final RegistryObject<EntityType<JujutsuNpc>> STUDENT_GOJO=npc("student_gojo");
    public static final RegistryObject<EntityType<JujutsuNpc>> SUKUNA_16=npc("sukuna_16");
    public static final RegistryObject<EntityType<JujutsuNpc>> SHINJUKU_GOJO=npc("shinjuku_gojo");
    public static final RegistryObject<EntityType<JujutsuNpc>> TRUE_SUKUNA=npc("true_sukuna");
    public static final RegistryObject<EntityType<JujutsuNpc>> AWAKENED_GOJO=npc("awakened_gojo");
    public static final RegistryObject<EntityType<JujutsuNpc>> MEGUMI_SUKUNA=npc("megumi_sukuna");
    public static final RegistryObject<EntityType<dev.unlimitedvoid.entity.MahoragaEntity>> MAHORAGA=TYPES.register("mahoraga",()->EntityType.Builder.<dev.unlimitedvoid.entity.MahoragaEntity>of(dev.unlimitedvoid.entity.MahoragaEntity::new,MobCategory.CREATURE).sized(1.4f,4f).clientTrackingRange(12).updateInterval(2).build(UnlimitedVoid.ID+":mahoraga"));
    private static RegistryObject<EntityType<JujutsuNpc>> npc(String name){
        return TYPES.register(name,()->EntityType.Builder.<JujutsuNpc>of(JujutsuNpc::new,MobCategory.CREATURE)
            .sized(.6f,1.95f).clientTrackingRange(10).updateInterval(3).build(UnlimitedVoid.ID+":"+name));
    }
}
