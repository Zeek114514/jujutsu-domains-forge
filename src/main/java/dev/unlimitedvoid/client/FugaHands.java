package dev.unlimitedvoid.client;

import com.mojang.math.Axis;
import dev.unlimitedvoid.UnlimitedVoid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Vanilla skin/arm rendering and render types, also compatible with shader pipelines. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class FugaHands {
    private static boolean rendering;
    public static boolean rendering(){return rendering;}
    @SubscribeEvent public static void render(RenderHandEvent event){
        var mc=Minecraft.getInstance();float draw=FugaClient.handProgress();
        if(draw<0||mc.player==null||mc.player.isInvisible()||mc.getCameraEntity()!=mc.player)return;
        if(!(mc.getEntityRenderDispatcher().getRenderer(mc.player) instanceof PlayerRenderer renderer))return;
        event.setCanceled(true);
        if(event.getHand()!=InteractionHand.MAIN_HAND)return;
        boolean leftDominant=mc.player.getMainArm()==HumanoidArm.LEFT;
        for(boolean right:new boolean[]{true,false}){
            boolean pulling=right!=leftDominant;float sign=right?1:-1;
            var pose=event.getPoseStack();pose.pushPose();
            try{
                pose.translate(sign*(pulling?.70-.10*draw:.65),-.58+.13*draw,pulling?-.65+.08*draw:-.78-.12*draw);
                pose.mulPose(Axis.YP.rotationDegrees(sign*(pulling?32+25*draw:16)));
                pose.mulPose(Axis.XP.rotationDegrees(-85+(pulling?25:8)*draw));
                pose.mulPose(Axis.ZP.rotationDegrees(sign*(pulling?8+14*draw:12)));
                rendering=true;
                if(right)renderer.renderRightHand(pose,event.getMultiBufferSource(),event.getPackedLight(),mc.player);
                else renderer.renderLeftHand(pose,event.getMultiBufferSource(),event.getPackedLight(),mc.player);
            }finally{rendering=false;pose.popPose();}
        }
    }
}
