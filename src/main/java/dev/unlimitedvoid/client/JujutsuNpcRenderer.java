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
            body.zRot=0;head.zRot=0;
            // Restore pivots before each frame; animated poses must not accumulate offsets.
            rightArm.x=-5;leftArm.x=5;rightArm.z=0;leftArm.z=0;rightLeg.z=0;leftLeg.z=0;
            if(npc.staggered()){
                float t=npc.staggerAge(age-npc.tickCount);float w=(float)Math.sin(Math.PI*Math.min(1,t/Math.max(1,npc.staggerLength())));
                body.xRot=-.22f*w;body.zRot=.12f*w;head.xRot-=.18f*w;rightArm.xRot=-.6f*w;leftArm.xRot=-.6f*w;
                copyLayers();return;
            }
            if(npc.summoningMahoraga()){rightArm.xRot=-1.8f;leftArm.xRot=-1.8f;rightArm.yRot=-.55f;leftArm.yRot=.55f;rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);return;}
            if(npc.punchKind()>=0){
                var p=NpcAttackMotion.pose(npc.isGojo(),npc.comboStep(),npc.punchKind(),npc.punchAge(age-npc.tickCount));
                body.yRot=p.twist();body.xRot=p.lean();head.yRot-=p.twist()*.6f;
                leftArm.xRot=p.left();rightArm.xRot=p.right();leftArm.yRot=.10f-p.twist()*.3f;rightArm.yRot=-.10f-p.twist()*.3f;
                leftArm.zRot=-.12f-p.guard();rightArm.zRot=.12f-p.guard();
                // Move shoulder pivots with the torso turn, maintaining native block hands.
                leftArm.x=5*(float)Math.cos(p.twist());leftArm.z=5*(float)Math.sin(p.twist());
                rightArm.x=-leftArm.x;rightArm.z=-leftArm.z;
                leftLeg.xRot=-p.step();rightLeg.xRot=p.step();leftLeg.z=-p.step()*2;
                copyLayers();return;
            }
            var pose=LimitlessClient.pose(npc.getUUID());if(pose==null)return;
            float weight=pose.weight();rightArm.xRot=-1.35f*weight;rightArm.yRot=-.22f*weight;
            if(pose.kind()==LimitlessKind.PURPLE){leftArm.xRot=-1.35f*weight;leftArm.yRot=.22f*weight;}
            rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);
        }
        private void copyLayers(){leftSleeve.copyFrom(leftArm);rightSleeve.copyFrom(rightArm);leftPants.copyFrom(leftLeg);rightPants.copyFrom(rightLeg);jacket.copyFrom(body);hat.copyFrom(head);}
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
