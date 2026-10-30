package dev.unlimitedvoid.client;

import dev.unlimitedvoid.UnlimitedVoid;
import dev.unlimitedvoid.entity.InnocentSunDog;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Standard player UVs normalized across a 1024-square photographic skin. */
public final class InnocentSunDogRenderer extends HumanoidMobRenderer<InnocentSunDog, PlayerModel<InnocentSunDog>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(UnlimitedVoid.ID,
            "textures/entity/innocent_sun_dog.png");

    public InnocentSunDogRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), .5f);
    }

    @Override public ResourceLocation getTextureLocation(InnocentSunDog entity) {
        return TEXTURE;
    }
}
