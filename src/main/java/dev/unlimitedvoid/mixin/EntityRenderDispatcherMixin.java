package dev.unlimitedvoid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.unlimitedvoid.client.VoidClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Entities outside this sphere should not appear inside its cosmic space. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = {"render", "m_114384_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void unlimitedvoid$entities(Entity entity, double x, double y, double z, float yaw,
                                        float partialTick, PoseStack poses, MultiBufferSource buffers,
                                        int packedLight, CallbackInfo callback) {
        if (VoidClient.shouldHideEntity(entity)) callback.cancel();
    }

    @Inject(method = {"renderShadow", "m_114457_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private static void unlimitedvoid$shadows(PoseStack poses, MultiBufferSource buffers, Entity entity,
                                              float strength, float partialTick, LevelReader level,
                                              float radius, CallbackInfo callback) {
        if (VoidClient.isCameraInside() || dev.unlimitedvoid.client.ShrineFloorRenderer.isReflecting()) callback.cancel();
    }
}
