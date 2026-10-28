package dev.unlimitedvoid.client;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.JujutsuNpc;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;

/** Original supplied skin UVs, including outer layers; no custom hair or glasses geometry. */
public final class JujutsuNpcRenderer extends HumanoidMobRenderer<JujutsuNpc,PlayerModel<JujutsuNpc>> {
    private final PlayerModel<JujutsuNpc> classic,slim;
    public JujutsuNpcRenderer(EntityRendererProvider.Context context,boolean gojo){
        super(context,new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER),false),.45f);
        classic=model;slim=new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM),true);
    }
    @Override public void render(JujutsuNpc entity,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light){
        model=(entity.isGojo()?!entity.isAwakened():entity.isAwakened())?slim:classic;
        super.render(entity,yaw,partial,poses,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(JujutsuNpc entity){
        String name=entity.isGojo()?(entity.isAwakened()?"shinjuku_gojo":"student_gojo"):(entity.isAwakened()?"true_sukuna":"sukuna_16");
        return new ResourceLocation(UnlimitedVoid.ID,"textures/entity/"+name+".png");
    }
}
