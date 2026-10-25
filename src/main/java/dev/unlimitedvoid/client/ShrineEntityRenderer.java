package dev.unlimitedvoid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.ShrineEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ShrineEntityRenderer extends EntityRenderer<ShrineEntity> {
    public ShrineEntityRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public ResourceLocation getTextureLocation(ShrineEntity entity){return new ResourceLocation("minecraft","textures/atlas/blocks.png");}
    @Override public void render(ShrineEntity entity,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light){
        poses.pushPose();
        poses.mulPose(Axis.YP.rotationDegrees(entity.facing()));
        ShrineRenderer.renderEntityModel(buffers.getBuffer(RenderType.debugQuads()),poses.last().pose());
        poses.popPose();
    }
    @Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(DomainEntities.SHRINE.get(),ShrineEntityRenderer::new);}
    }
}
