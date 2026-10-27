package dev.unlimitedvoid.client;

import com.mojang.math.Axis;
import dev.unlimitedvoid.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class LimitlessHands {
    private static boolean rendering;
    public static boolean rendering(){return rendering;}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void hands(RenderHandEvent e){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getCameraEntity()!=mc.player||mc.player.isInvisible()||DomainSeals.of(mc.player.getUUID(),e.getPartialTick())!=null)return;
        var pose=LimitlessClient.pose(mc.player.getUUID());if(pose==null||!(mc.getEntityRenderDispatcher().getRenderer(mc.player) instanceof PlayerRenderer renderer))return;
        e.setCanceled(true);if(e.getHand()!=InteractionHand.MAIN_HAND)return;
        for(boolean right:new boolean[]{true,false}){
            if(!right&&pose.kind()!=LimitlessKind.PURPLE)continue;float sign=right?1:-1;var stack=e.getPoseStack();stack.pushPose();
            try{
                stack.translate(sign*(pose.kind()==LimitlessKind.PURPLE?.55-.18*pose.progress():.65),-.65-(1-pose.weight())*.6,-.8);
                stack.mulPose(Axis.XP.rotationDegrees(-65));stack.mulPose(Axis.YP.rotationDegrees(sign*18));rendering=true;
                if(right)renderer.renderRightHand(stack,e.getMultiBufferSource(),e.getPackedLight(),mc.player);else renderer.renderLeftHand(stack,e.getMultiBufferSource(),e.getPackedLight(),mc.player);
            }finally{rendering=false;stack.popPose();}
        }
    }
}
