package dev.unlimitedvoid.client;

import dev.unlimitedvoid.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class DomainSealHands {
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hand(RenderHandEvent event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getCameraEntity()!=mc.player||mc.player.isInvisible())return;
        var seal=DomainSeals.of(mc.player.getUUID(),event.getPartialTick());if(seal==null)return;
        if(!(mc.getEntityRenderDispatcher().getRenderer(mc.player) instanceof PlayerRenderer renderer))return;
        event.setCanceled(true);if(event.getHand()!=InteractionHand.MAIN_HAND)return;
        var poses=event.getPoseStack();
        DomainSeals.renderingHands(true);
        try{
            for(boolean right:new boolean[]{true,false}){
                if(!right&&seal.kind()==DomainKind.VOID)continue;
                poses.pushPose();
                try{
                    float sign=right?1:-1;
                    boolean single=seal.kind()==DomainKind.VOID;
                    poses.translate(single?.65:sign*.57,-.83-(1-seal.weight())*.65,-.80);
                    poses.mulPose(Axis.XP.rotationDegrees(-12));
                    poses.mulPose(Axis.ZP.rotationDegrees(single?-8:sign*12));
                    if(right)renderer.renderRightHand(poses,event.getMultiBufferSource(),event.getPackedLight(),mc.player);
                    else renderer.renderLeftHand(poses,event.getMultiBufferSource(),event.getPackedLight(),mc.player);
                }finally{poses.popPose();}
            }
        }finally{DomainSeals.renderingHands(false);}
    }
}
