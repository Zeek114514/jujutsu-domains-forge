package dev.unlimitedvoid;

import dev.unlimitedvoid.entity.ShrineEntity;
import net.minecraft.world.entity.*;
import net.minecraftforge.registries.*;

public final class DomainEntities {
    public static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,UnlimitedVoid.ID);
    public static final RegistryObject<EntityType<ShrineEntity>> SHRINE=TYPES.register("shrine",
            ()->EntityType.Builder.<ShrineEntity>of(ShrineEntity::new,MobCategory.MISC).sized(18,11).noSave()
                    .clientTrackingRange(16).updateInterval(200).build("unlimitedvoid:shrine"));
}
