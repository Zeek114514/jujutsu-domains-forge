package dev.unlimitedvoid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.unlimitedvoid.client.VoidClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Block entity models belong to the hidden terrain, rather than the domain. */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = {"render", "m_112267_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void unlimitedvoid$blockEntities(BlockEntity entity, float partialTick, PoseStack poses,
                                             MultiBufferSource buffers, CallbackInfo callback) {
        if (VoidClient.isCameraInside()) callback.cancel();
    }
}
