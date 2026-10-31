package dev.unlimitedvoid.client;

import dev.unlimitedvoid.UnlimitedVoid;
import dev.unlimitedvoid.entity.InnocentSunDog;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Standard player UVs normalized across a 1024-square photographic skin. */
public final class InnocentSunDogRenderer extends HumanoidMobRenderer<InnocentSunDog, PlayerModel<InnocentSunDog>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(UnlimitedVoid.ID,
            "textures/entity/innocent_sun_dog.png");

    private static final ResourceLocation SERVANT_TEXTURE = new ResourceLocation(UnlimitedVoid.ID, "textures/entity/sun_dog_servant.png");

    public InnocentSunDogRenderer(EntityRendererProvider.Context context) {
        super(context, new PrankModel(context.bakeLayer(ModelLayers.PLAYER)), .5f);
    }

    private static final class PrankModel extends PlayerModel<InnocentSunDog> {
        PrankModel(net.minecraft.client.model.geom.ModelPart root){super(root,false);}
        @Override public void setupAnim(InnocentSunDog npc,float swing,float amount,float age,float yaw,float pitch){
            super.setupAnim(npc,swing,amount,age,yaw,pitch);
            body.zRot=0;head.zRot=0;
            float t=npc.kickAge(age-npc.tickCount);
            if(t>=0&&t<InnocentSunDog.KICK_DURATION){
                // Wind up behind the planted leg, snap forward at the server contact frame, recover.
                float leg=t<8?lerp(0,.65f,t/8):t<12?lerp(.65f,-1.30f,(t-8)/4):t<16?lerp(-1.30f,-1.10f,(t-12)/4):lerp(-1.10f,0,(t-16)/14);
                float weight=(float)Math.sin(Math.PI*t/InnocentSunDog.KICK_DURATION);
                rightLeg.xRot=leg;rightLeg.zRot=-.10f*weight;leftLeg.xRot=.08f*weight;
                body.zRot=.10f*weight;body.xRot=-.15f*weight;head.zRot=-.08f*weight;
                rightArm.xRot=.55f*weight;leftArm.xRot=-.75f*weight;leftArm.zRot=-.18f*weight;
            }
            float recoil=npc.recoilAge(age-npc.tickCount);
            if(recoil>=0&&recoil<16){float w=(float)Math.sin(Math.PI*recoil/16);body.xRot=.40f*w;head.xRot=-.20f*w;leftArm.xRot=-.65f*w;rightArm.xRot=-.65f*w;}
            rightPants.copyFrom(rightLeg);leftPants.copyFrom(leftLeg);rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);jacket.copyFrom(body);hat.copyFrom(head);
        }
        private static float lerp(float a,float b,float t){t=Math.max(0,Math.min(1,t));t=t*t*(3-2*t);return a+(b-a)*t;}
    }

    @Override public ResourceLocation getTextureLocation(InnocentSunDog entity) {
        return entity.getType()==dev.unlimitedvoid.DomainEntities.SUN_DOG_SERVANT.get()?SERVANT_TEXTURE:TEXTURE;
    }
}
