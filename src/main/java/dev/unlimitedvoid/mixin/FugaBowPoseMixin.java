package dev.unlimitedvoid.mixin;

import dev.unlimitedvoid.client.FugaClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Visual arm draw only; does not replace held items, equipment or gameplay input. */
@Mixin(HumanoidModel.class)
public abstract class FugaBowPoseMixin {
    @Inject(method={"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V","m_6973_(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},at=@At("RETURN"),remap=false)
    private void unlimitedvoid$fugaDraw(LivingEntity entity,float swing,float amount,float age,float yaw,float pitch,CallbackInfo callback){
        float draw=FugaClient.drawProgress(entity.getUUID());if(draw<0)return;
        var model=(HumanoidModel<?>)(Object)this;
        ModelPart leftArm=model.leftArm,rightArm=model.rightArm,head=model.head;
        float weight=Math.min(1,draw*4);
        leftArm.xRot+=(head.xRot-(float)Math.PI/2-leftArm.xRot)*weight;
        leftArm.yRot+=(head.yRot+.15f-leftArm.yRot)*weight;
        rightArm.xRot+=(head.xRot-(float)Math.PI/2-rightArm.xRot)*weight;
        rightArm.yRot+=(head.yRot-.15f-.65f*draw-rightArm.yRot)*weight;
        leftArm.zRot*=1-weight;rightArm.zRot*=1-weight;
    }
}
