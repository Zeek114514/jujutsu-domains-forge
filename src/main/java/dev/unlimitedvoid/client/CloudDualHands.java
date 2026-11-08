package dev.unlimitedvoid.client;
import com.mojang.math.Axis;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.item.CursedToolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Render a second physical blade from the one weapon's form; never create or replace inventory items. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class CloudDualHands {
    @SubscribeEvent public static void hand(RenderHandEvent event){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||mc.getCameraEntity()!=p||event.getHand()!=InteractionHand.MAIN_HAND||!CursedToolItem.dual(p)||p.isInvisible()||FugaClient.handProgress()>=0)return;
        if(!(mc.getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer renderer))return;
        boolean left=p.getMainArm()==HumanoidArm.RIGHT;float sign=left?-1:1;
        var pose=event.getPoseStack();float swing=(float)Math.sin(p.getAttackAnim(event.getPartialTick())*Math.PI);
        pose.pushPose();
        try{
            pose.translate(sign*.54,-.48+swing*.1,-.72);pose.mulPose(Axis.XP.rotationDegrees(-80-swing*25));pose.mulPose(Axis.YP.rotationDegrees(-sign*20));
            if(left)renderer.renderLeftHand(pose,event.getMultiBufferSource(),event.getPackedLight(),p);else renderer.renderRightHand(pose,event.getMultiBufferSource(),event.getPackedLight(),p);
        }finally{pose.popPose();}
        pose.pushPose();
        try{
            pose.translate(sign*.55-swing*sign*.16,-.43+swing*.12,-.8);pose.mulPose(Axis.YP.rotationDegrees(sign*15));pose.mulPose(Axis.ZP.rotationDegrees(-sign*12+swing*sign*30));
            mc.getItemRenderer().renderStatic(p,p.getMainHandItem(),left?ItemDisplayContext.FIRST_PERSON_LEFT_HAND:ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,left,pose,event.getMultiBufferSource(),mc.level,event.getPackedLight(),net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,p.getId());
        }finally{pose.popPose();}
    }
    @Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Layers {
        @SubscribeEvent public static void add(EntityRenderersEvent.AddLayers event){
            for(String skin:event.getSkins()){PlayerRenderer renderer=event.getSkin(skin);if(renderer!=null)renderer.addLayer(new SecondBlade(renderer));}
        }
    }
    private static final class SecondBlade extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        SecondBlade(PlayerRenderer renderer){super(renderer);}
        @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer p,float limb,float amount,float partial,float age,float yaw,float pitch){
            if(!CursedToolItem.dual(p)||p.isInvisible())return;
            boolean left=p.getMainArm()==HumanoidArm.RIGHT;pose.pushPose();
            try{
                getParentModel().translateToHand(left?HumanoidArm.LEFT:HumanoidArm.RIGHT,pose);pose.mulPose(Axis.XP.rotationDegrees(-90));pose.mulPose(Axis.YP.rotationDegrees(180));pose.translate((left?-1:1)/16.0,.125,-.625);
                var mc=Minecraft.getInstance();mc.getItemRenderer().renderStatic(p,p.getMainHandItem(),left?ItemDisplayContext.THIRD_PERSON_LEFT_HAND:ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,left,pose,buffers,mc.level,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,p.getId());
            }finally{pose.popPose();}
        }
    }
}
