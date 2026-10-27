package dev.unlimitedvoid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.unlimitedvoid.*;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

public final class DomainSealLayer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    public DomainSealLayer(PlayerRenderer parent){super(parent);}
    @Override public void render(PoseStack poses,MultiBufferSource buffers,int light,AbstractClientPlayer player,float limb,float amount,float partial,float age,float yaw,float pitch){
        if(player.isInvisible())return;var seal=DomainSeals.of(player.getUUID(),partial);if(seal==null)return;
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(player.getSkinTextureLocation()));
        for(boolean right:new boolean[]{true,false}){
            if(!right&&seal.kind()==DomainKind.VOID)continue;
            ModelPart arm=right?getParentModel().rightArm:getParentModel().leftArm;
            poses.pushPose();
            try{
                arm.translateAndRotate(poses);poses.translate(right?-.0625:.0625,.54,0);
                poses.mulPose(new Quaternionf().rotationZYX(arm.zRot,arm.yRot,arm.xRot).conjugate());
                SealHandMesh.hand(poses,out,light,seal.kind(),right);
            }finally{poses.popPose();}
        }
    }
    @Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers event){for(String skin:event.getSkins()){PlayerRenderer renderer=event.getSkin(skin);if(renderer!=null)renderer.addLayer(new DomainSealLayer(renderer));}}
    }
}
