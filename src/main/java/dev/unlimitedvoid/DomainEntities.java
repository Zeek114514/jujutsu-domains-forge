package dev.unlimitedvoid;

import dev.unlimitedvoid.entity.ShrineEntity;
import dev.unlimitedvoid.entity.JujutsuNpc;
import net.minecraft.world.entity.*;
import net.minecraftforge.registries.*;

public final class DomainEntities {
    public static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,UnlimitedVoid.ID);
    public static final RegistryObject<EntityType<ShrineEntity>> SHRINE=TYPES.register("shrine",
            ()->EntityType.Builder.<ShrineEntity>of(ShrineEntity::new,MobCategory.MISC).sized(18,11).noSave()
                    .clientTrackingRange(16).updateInterval(200).build("unlimitedvoid:shrine"));
    public static final RegistryObject<EntityType<JujutsuNpc>> STUDENT_GOJO=npc("student_gojo");
    public static final RegistryObject<EntityType<JujutsuNpc>> SUKUNA_16=npc("sukuna_16");
    public static final RegistryObject<EntityType<JujutsuNpc>> SHINJUKU_GOJO=npc("shinjuku_gojo");
    public static final RegistryObject<EntityType<JujutsuNpc>> TRUE_SUKUNA=npc("true_sukuna");
    public static final RegistryObject<EntityType<JujutsuNpc>> AWAKENED_GOJO=npc("awakened_gojo");
    private static RegistryObject<EntityType<JujutsuNpc>> npc(String name){
        return TYPES.register(name,()->EntityType.Builder.<JujutsuNpc>of(JujutsuNpc::new,MobCategory.CREATURE)
            .sized(.6f,1.95f).clientTrackingRange(10).updateInterval(3).build(UnlimitedVoid.ID+":"+name));
    }
}
