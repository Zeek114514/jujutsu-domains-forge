package dev.unlimitedvoid.client;

import dev.unlimitedvoid.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
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
        event.setCanceled(true);if(event.getHand()!=InteractionHand.MAIN_HAND)return;
        var poses=event.getPoseStack();var out=event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(mc.player.getSkinTextureLocation()));
        DomainSeals.renderingHands(true);
        try{
            for(boolean right:new boolean[]{true,false}){
                if(!right&&seal.kind()==DomainKind.VOID)continue;
                poses.pushPose();
                try{
                    float sign=right?1:-1;
                    double x=seal.kind()==DomainKind.VOID?.22:sign*.08;
                    poses.translate(x,-.15-(1-seal.weight())*.65,-.72);
                    poses.scale(-1,-1,-1);
                    SealHandMesh.forearm(poses,out,event.getPackedLight(),right);
                    SealHandMesh.hand(poses,out,event.getPackedLight(),seal.kind(),right);
                }finally{poses.popPose();}
            }
        }finally{DomainSeals.renderingHands(false);}
    }
}
