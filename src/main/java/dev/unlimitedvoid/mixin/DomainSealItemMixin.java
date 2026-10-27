package dev.unlimitedvoid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.unlimitedvoid.client.DomainSeals;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide held items during the seal without touching equipment or inventories. */
@Mixin(ItemInHandLayer.class)
public abstract class DomainSealItemMixin {
    @Inject(method={"render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V","m_6494_(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V"},at=@At("HEAD"),cancellable=true,remap=false)
    private void unlimitedvoid$seal(PoseStack poses,MultiBufferSource buffers,int light,LivingEntity player,float limb,float amount,float partial,float age,float yaw,float pitch,CallbackInfo callback){
        if(DomainSeals.of(player.getUUID(),partial)!=null)callback.cancel();
    }
}
