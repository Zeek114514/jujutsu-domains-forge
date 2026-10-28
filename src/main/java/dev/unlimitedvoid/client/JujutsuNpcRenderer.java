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
        super(context,new TechniqueModel(context.bakeLayer(ModelLayers.PLAYER),false),.45f);
        classic=model;slim=new TechniqueModel(context.bakeLayer(ModelLayers.PLAYER_SLIM),true);
    }
    private static final class TechniqueModel extends PlayerModel<JujutsuNpc> {
        TechniqueModel(net.minecraft.client.model.geom.ModelPart root,boolean slim){super(root,slim);}
        @Override public void setupAnim(JujutsuNpc npc,float swing,float amount,float age,float yaw,float pitch){
            super.setupAnim(npc,swing,amount,age,yaw,pitch);
            if(npc.summoningMahoraga()){rightArm.xRot=-1.8f;leftArm.xRot=-1.8f;rightArm.yRot=-.55f;leftArm.yRot=.55f;rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);return;}
            if(npc.punchKind()>=0){float t=npc.punchAge(age-npc.tickCount)/NpcMeleeRules.ANIMATION;float hit=(float)Math.sin(Math.PI*Math.max(0,Math.min(1,t)));rightArm.xRot=-.35f-1.5f*hit;rightArm.yRot=-.18f;body.yRot=-.12f*hit;rightSleeve.copyFrom(rightArm);return;}
            var pose=LimitlessClient.pose(npc.getUUID());if(pose==null)return;
            float weight=pose.weight();rightArm.xRot=-1.35f*weight;rightArm.yRot=-.22f*weight;
            if(pose.kind()==LimitlessKind.PURPLE){leftArm.xRot=-1.35f*weight;leftArm.yRot=.22f*weight;}
            rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);
        }
    }
    @Override public void render(JujutsuNpc entity,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light){
        model=(entity.isGojo()?!entity.isShinjuku():entity.isTrueSukuna())?slim:classic;
        super.render(entity,yaw,partial,poses,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(JujutsuNpc entity){
        String name=entity.isGojo()?(entity.isShinjuku()?"shinjuku_gojo":entity.isAscended()?"awakened_gojo":"student_gojo"):(entity.isTrueSukuna()?"true_sukuna":entity.isMegumi()?"megumi_sukuna":"sukuna_16");
        return new ResourceLocation(UnlimitedVoid.ID,"textures/entity/"+name+".png");
    }
}
