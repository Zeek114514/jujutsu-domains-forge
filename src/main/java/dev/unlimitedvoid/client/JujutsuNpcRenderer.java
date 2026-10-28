package dev.unlimitedvoid.client;

import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.JujutsuNpc;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Original cuboid hair and sunglasses over a vanilla-proportioned, 64x64 pixel skin. */
public final class JujutsuNpcRenderer extends HumanoidMobRenderer<JujutsuNpc,HumanoidModel<JujutsuNpc>> {
    static final ModelLayerLocation GOJO=new ModelLayerLocation(new ResourceLocation(UnlimitedVoid.ID,"student_gojo"),"main");
    static final ModelLayerLocation SUKUNA=new ModelLayerLocation(new ResourceLocation(UnlimitedVoid.ID,"sukuna_16"),"main");
    private final ResourceLocation skin;
    public JujutsuNpcRenderer(EntityRendererProvider.Context context,boolean gojo){
        super(context,new HumanoidModel<>(context.bakeLayer(gojo?GOJO:SUKUNA)),.45f);
        com.mojang.logging.LogUtils.getLogger().info("NPC renderer created gojo={}",gojo);
        skin=new ResourceLocation(UnlimitedVoid.ID,"textures/entity/"+(gojo?"student_gojo":"sukuna_16")+".png");
    }
    @Override public ResourceLocation getTextureLocation(JujutsuNpc entity){return skin;}
    static LayerDefinition layer(boolean gojo){
        var mesh=HumanoidModel.createMesh(CubeDeformation.NONE,0);var head=mesh.getRoot().getChild("head");
        // Tufts sample a dedicated solid hair patch, leaving the face UV unchanged.
        var hair=CubeListBuilder.create().texOffs(0,32).addBox(-4.4f,-8.5f,-4.35f,8.8f,2,8.7f);
        head.addOrReplaceChild("hair_cap",hair,PartPose.ZERO);
        mesh.getRoot().getChild("body").addOrReplaceChild("collar",CubeListBuilder.create().texOffs(36,32).addBox(-4,-.3f,-2.3f,8,2,4.6f),PartPose.ZERO);
        int index=0;
        for(int row=0;row<3;row++)for(int col=0;col<4;col++){
            float x=-3.4f+col*2.2f,z=-2.8f+row*2.5f;
            float height=(gojo?2.2f:1.3f)+((col+row)%3)*.4f;
            head.addOrReplaceChild("tuft"+index++,CubeListBuilder.create().texOffs(0,32).addBox(-.85f,-height,-.85f,1.7f,height+1,1.7f),
                PartPose.offsetAndRotation(x,-8,z,((col%2)*2-1)*.16f,0,((row%2)*2-1)*.20f));
        }
        for(int col=0;col<4;col++)head.addOrReplaceChild("fringe"+col,
            CubeListBuilder.create().texOffs(0,32).addBox(-.8f,0,-.4f,1.6f,gojo?2.3f:1.6f,.9f),
            PartPose.offsetAndRotation(-3.1f+col*2.05f,-7,-4.2f,0,0,col%2==0?.13f:-.13f));
        if(gojo){
            head.addOrReplaceChild("glasses",CubeListBuilder.create().texOffs(40,32)
                .addBox(-3.6f,-4.5f,-4.55f,2.6f,2.1f,.5f).addBox(1,-4.5f,-4.55f,2.6f,2.1f,.5f)
                .addBox(-1,-3.8f,-4.45f,2,.45f,.35f),PartPose.ZERO);
        }
        return LayerDefinition.create(mesh,64,64);
    }
}
