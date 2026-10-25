package dev.unlimitedvoid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.unlimitedvoid.client.VoidClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep entities visible against the custom domain sky, while suppressing terrain. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    // Both names are verified against the 1.20.1 official -> SRG build mapping.
    // Explicit names keep this version-specific mixin independent of a refmap.
    @Inject(method = {"renderChunkLayer", "m_172993_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void unlimitedvoid$terrain(RenderType type, PoseStack poses, double x, double y, double z,
                                       Matrix4f projection, CallbackInfo callback) {
        if (VoidClient.isCameraInside()) callback.cancel();
    }

    @Inject(method = {"renderClouds", "m_253054_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void unlimitedvoid$clouds(PoseStack poses, Matrix4f projection, float partialTick,
                                      double x, double y, double z, CallbackInfo callback) {
        if (VoidClient.isCameraInside()) callback.cancel();
    }

    @Inject(method = {"renderSnowAndRain", "m_109703_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void unlimitedvoid$weather(LightTexture light, float partialTick, double x, double y, double z,
                                       CallbackInfo callback) {
        if (VoidClient.isCameraInside()) callback.cancel();
    }
}
