package dev.unlimitedvoid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.unlimitedvoid.client.LimitlessRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Run after the shader pipeline's world composite, before GUI rendering. */
@Mixin(value=GameRenderer.class,priority=500)
public abstract class LimitlessLateRenderMixin {
    @Inject(method={"renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V","m_109089_(FJLcom/mojang/blaze3d/vertex/PoseStack;)V"},at=@At("RETURN"),remap=false)
    private void unlimitedvoid$lateOrbs(float partial,long deadline,PoseStack poses,CallbackInfo callback){LimitlessRenderer.renderLate();}
}
