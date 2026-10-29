package dev.unlimitedvoid.client;
import dev.unlimitedvoid.UnlimitedVoid;
import dev.unlimitedvoid.entity.MahoragaEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
/** Cuboid geometry follows the approved concept: broad chest, long arms, temple fins and eight handles. */
public final class MahoragaModel extends HierarchicalModel<MahoragaEntity> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(new ResourceLocation(UnlimitedVoid.ID,"mahoraga"),"main");
    private final ModelPart root,body,head,right,left,rightLeg,leftLeg,wheel,wheelMount;
    public MahoragaModel(ModelPart root){this.root=root;body=root.getChild("body");head=root.getChild("head");right=root.getChild("right_arm");left=root.getChild("left_arm");rightLeg=root.getChild("right_leg");leftLeg=root.getChild("left_leg");wheelMount=root.getChild("wheel_mount");wheel=wheelMount.getChild("wheel");}
    private static CubeListBuilder ivory(){return CubeListBuilder.create().texOffs(0,0);}
    private static CubeListBuilder gray(){return CubeListBuilder.create().texOffs(64,64);}
    private static CubeListBuilder dark(){return CubeListBuilder.create().texOffs(128,0);}
    private static CubeListBuilder cloth(){return CubeListBuilder.create().texOffs(0,128);}
    private static CubeListBuilder bronze(){return CubeListBuilder.create().texOffs(128,128);}
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        var body=root.addOrReplaceChild("body",ivory().addBox(-10,-21,-4.5f,20,8,9).addBox(-8.5f,-13,-4,17,4,8).addBox(-7,-9,-3.6f,14,4,7.2f).addBox(-6,-5,-3.4f,12,7,6.8f),PartPose.offset(0,5,0));
        body.addOrReplaceChild("pectorals",ivory().addBox(-10.4f,-20,-7,10,7,3).addBox(.4f,-20,-7,10,7,3),PartPose.ZERO);
        for(int side:new int[]{-1,1}){
            body.addOrReplaceChild("clavicle_"+side,ivory().addBox(-4,-1,-1,8,2,2),PartPose.offsetAndRotation(side*5,-21,-6.5f,0,0,side*.12f));
            body.addOrReplaceChild("pec_lower_"+side,gray().addBox(-4,-.4f,-.6f,8,.8f,1.2f),PartPose.offsetAndRotation(side*5,-13.2f,-6.5f,0,0,side*-.06f));
            for(int i=0;i<3;i++)body.addOrReplaceChild("serratus_"+side+"_"+i,ivory().addBox(-1.6f,-.65f,-.7f,3.2f,1.3f,1.4f),PartPose.offsetAndRotation(side*(7.3f-i*.7f),-12+i*2,-5.8f,0,0,side*.38f));
        }
        body.addOrReplaceChild("ribs",gray().addBox(-7.8f,-12.5f,-4.9f,2.8f,4,1).addBox(5,-12.5f,-4.9f,2.8f,4,1),PartPose.ZERO);
        var abs=ivory();for(int row=0;row<3;row++){float y=-13+row*3.4f;abs.addBox(-3.9f,y,-5.5f,3.6f,2.8f,1.6f).addBox(.3f,y,-5.5f,3.6f,2.8f,1.6f);}body.addOrReplaceChild("abdominals",abs,PartPose.ZERO);
        body.addOrReplaceChild("trapezius",ivory().addBox(-8,-24,-3.7f,16,5,8).addBox(-2,-21,3.5f,4,15,2),PartPose.ZERO);
        body.addOrReplaceChild("spinal_ridge",gray().addBox(-.7f,-23,5.4f,1.4f,18,.8f),PartPose.ZERO);
        var vertebrae=ivory();for(int i=0;i<7;i++)vertebrae.addBox(-1.2f,-22+i*2.3f,5.5f,2.4f,1.3f,1);body.addOrReplaceChild("vertebrae",vertebrae,PartPose.ZERO);
        body.addOrReplaceChild("back_lats",ivory().addBox(-10.5f,-20,2.5f,6.5f,6,3).addBox(4,-20,2.5f,6.5f,6,3).addBox(-8.5f,-14,2.5f,4.5f,5,2).addBox(4,-14,2.5f,4.5f,5,2),PartPose.ZERO);
        body.addOrReplaceChild("sash",dark().addBox(-6.8f,-2,-4.8f,13.6f,5,9.6f).addBox(-3,2,-6.8f,6,14,1.2f).addBox(1,2,-7.5f,3,11,1.3f),PartPose.ZERO);
        body.addOrReplaceChild("neckline",ivory().addBox(-8.8f,-22,-7.3f,17.6f,.7f,.6f),PartPose.ZERO);
        for(int i=0;i<9;i++){
            float x=(i-4)*1.75f,y=-20.5f+(4-Math.abs(i-4))*1.02f;
            var link=dark().addBox(-1.2f,-.8f,-.16f,2.4f,.3f,.32f).addBox(-1.2f,.5f,-.16f,2.4f,.3f,.32f).addBox(-1.2f,-.5f,-.16f,.3f,1,.32f).addBox(.9f,-.5f,-.16f,.3f,1,.32f);
            body.addOrReplaceChild("chain_link_"+i,link,PartPose.offsetAndRotation(x,y,-7.45f,0,0,i<4?.36f:i>4?-.36f:0));
        }
        var armR=root.addOrReplaceChild("right_arm",ivory().addBox(-4.2f,-2,-4.6f,8.4f,12,9.2f),PartPose.offset(-14,-13,0));
        var armL=root.addOrReplaceChild("left_arm",ivory().addBox(-4.2f,-2,-4.6f,8.4f,12,9.2f),PartPose.offset(14,-13,0));
        for(var arm:new PartDefinition[]{armR,armL}){
            arm.addOrReplaceChild("deltoid",ivory().addBox(-5.3f,-1,-5.5f,10.6f,6,11),PartPose.ZERO);
            arm.addOrReplaceChild("shoulder_caps",ivory().addBox(-4.5f,-2,-5,9,2,10).addBox(-4.8f,1,4.6f,9.6f,7,1.8f),PartPose.ZERO);
            arm.addOrReplaceChild("biceps",ivory().addBox(-4.3f,4,-6,8.6f,6,2),PartPose.ZERO);
            var forearm=arm.addOrReplaceChild("forearm",ivory().addBox(-3.5f,0,-4,7,14,8).addBox(-3.8f,12,-4.3f,7.6f,5.5f,8.6f),PartPose.offset(0,10,0));
            forearm.addOrReplaceChild("raised_ridges",gray().addBox(-4.3f,2,-5,8.6f,1.5f,1.2f).addBox(-4.3f,6,-5,8.6f,1.5f,1.2f).addBox(-4.3f,10,-5,8.6f,1.5f,1.2f),PartPose.ZERO);
        }
        armR.getChild("forearm").addOrReplaceChild("extermination_blade",ivory().addBox(-4,-1,-1,7,17,2).addBox(-3.5f,16,-.9f,6,4,1.8f).addBox(-2.5f,20,-.75f,4,4,1.5f).addBox(-1.5f,24,-.6f,2,3,1.2f),PartPose.offsetAndRotation(-5,0,-1,0,0,.09f));
        var blade=armR.getChild("forearm").getChild("extermination_blade");
        blade.addOrReplaceChild("blade_spine",gray().addBox(-2,-.5f,-1.25f,2,19,.5f).addBox(-1.5f,18,-1,1,5,.4f),PartPose.ZERO);
        blade.addOrReplaceChild("blade_guard",bronze().addBox(-4.7f,-1.8f,-1.5f,8.4f,1.5f,3),PartPose.ZERO);
        var head=root.addOrReplaceChild("head",ivory().addBox(-4,-10,-4,8,8,8).addBox(-4.2f,-2.2f,-4.8f,8.4f,2.8f,8.4f).addBox(-3.3f,.6f,-4.5f,6.6f,1.8f,7.4f),PartPose.offset(0,-17,0));
        head.addOrReplaceChild("blind_brow",ivory().addBox(-4.4f,-8.5f,-5,8.8f,5.8f,1.7f).addBox(-.8f,-10.7f,-4,1.6f,7.7f,8.2f),PartPose.ZERO);
        head.addOrReplaceChild("cheekbones",ivory().addBox(-5,-3.8f,-5,2.1f,2.5f,2).addBox(2.9f,-3.8f,-5,2.1f,2.5f,2).addBox(-3.6f,1,-4.5f,7.2f,1.4f,6),PartPose.ZERO);
        head.addOrReplaceChild("brow_seam",gray().addBox(-.25f,-9.7f,-5.1f,.5f,6,.35f),PartPose.ZERO);
        head.addOrReplaceChild("mouth",dark().addBox(-4.05f,-2.6f,-5.35f,8.1f,2.3f,.6f),PartPose.ZERO);
        var teeth=ivory();for(int i=0;i<8;i++)teeth.addBox(-3.8f+i*.96f,-2.4f,-5.8f,.72f,1.6f,.55f);head.addOrReplaceChild("teeth",teeth,PartPose.ZERO);
        head.addOrReplaceChild("brow_bridge",ivory().addBox(-5.7f,-6.5f,-5.6f,11.4f,1.6f,1.3f),PartPose.ZERO);
        for(int side:new int[]{-1,1})for(int tier=0;tier<2;tier++){
            // Several connected, tapering cuboids sweep outward then curl upward.
            float[] lengths=tier==0?new float[]{5,5,4,3}:new float[]{4.5f,4,3.5f,2};
            float[] angles=tier==0?new float[]{-.16f,-.30f,-.52f,-.85f}:new float[]{.10f,-.06f,-.28f,-.70f};
            float x=side*3.8f,y=tier==0?-7.4f:-3.6f;
            for(int j=0;j<lengths.length;j++){
                float length=lengths[j],angle=angles[j],thick=(tier==0?2.7f:2.2f)-j*.55f;
                var horn=ivory().addBox(side<0?-length:0,-thick/2,-thick*.62f,length,thick,thick*1.24f);
                head.addOrReplaceChild("horn_"+side+"_"+tier+"_"+j,horn,PartPose.offsetAndRotation(x,y,tier==0?-.1f:-1.2f,0,0,side*angle));
                x+=side*(float)Math.cos(angle)*length;y+=(float)Math.sin(angle)*length;
            }
        }
        for(int side:new int[]{-1,1}){
            var leg=root.addOrReplaceChild(side<0?"right_leg":"left_leg",cloth().addBox(-4,0,-4,8,13,8).addBox(-4.2f,2,-4.3f,8.4f,8,8.6f),PartPose.offset(side*4.5f,7,0));
            leg.addOrReplaceChild("folds",cloth().addBox(-4.5f,1,-5.8f,2.3f,11,1.4f).addBox(.6f,1,-5.7f,2,10.5f,1.3f),PartPose.ZERO);
            leg.addOrReplaceChild("ankle",ivory().addBox(-3,12,-3.2f,6,3.5f,6.4f),PartPose.ZERO);
            leg.addOrReplaceChild("ankle_wrap",dark().addBox(-3.2f,12.6f,-3.4f,6.4f,1.8f,6.8f),PartPose.ZERO);
            leg.addOrReplaceChild("foot",ivory().addBox(-4,14.5f,-7.5f,8,2.5f,12),PartPose.ZERO);
        }
        var mount=root.addOrReplaceChild("wheel_mount",CubeListBuilder.create(),PartPose.offsetAndRotation(0,-33,0,-.20f,0,0));var wheel=mount.addOrReplaceChild("wheel",bronze().addBox(-2,-1.8f,-2,4,3.6f,4),PartPose.ZERO);
        wheel.addOrReplaceChild("hub_rings",bronze().addBox(-3,-1.1f,-3,6,2.2f,6).addBox(-1.4f,-2.4f,-1.4f,2.8f,1.2f,2.8f),PartPose.ZERO);
        for(int i=0;i<8;i++){var detailing=wheel.addOrReplaceChild("inlay_"+i,gray().addBox(-3.3f,-.82f,10.5f,6.6f,.25f,.65f),PartPose.rotation(0,i*(float)Math.PI/4,0));detailing.addOrReplaceChild("handle_cap",bronze().addBox(-1.15f,-2.05f,14.5f,2.3f,.6f,2.3f),PartPose.ZERO);}
        for(int i=0;i<8;i++)wheel.addOrReplaceChild("segment_"+i,bronze().addBox(-4.55f,-.7f,10,9.1f,1.4f,2).addBox(-.65f,-.55f,1,1.3f,1.1f,10.5f).addBox(-.8f,-.8f,11,1.6f,1.6f,3.8f).addBox(-1.65f,-1.65f,14,3.3f,3.3f,3.3f),PartPose.rotation(0,i*(float)Math.PI/4,0));
        return LayerDefinition.create(mesh,256,256);
    }
    @Override public ModelPart root(){return root;}
    @Override public void setupAnim(MahoragaEntity entity,float swing,float amount,float age,float yaw,float pitch){
        root.getAllParts().forEach(ModelPart::resetPose);float partial=age-entity.tickCount;
        root.y=(1-entity.emergence(partial))*60;head.yRot=yaw*(float)Math.PI/180*.45f;head.xRot=pitch*(float)Math.PI/180*.35f;
        // Lengthen legs and torso while preserving head width and the planted feet.
        body.y=-1.6f;body.yScale=1.2f;head.y=-28.4f;wheelMount.y=-47.5f;
        right.y=left.y=-23.5f;right.yScale=left.yScale=1.15f;
        rightLeg.y=leftLeg.y=1;rightLeg.yScale=leftLeg.yScale=23f/17f;
        right.zRot=.10f;left.zRot=-.10f;right.xRot=(float)Math.cos(swing*.6662)*.45f*amount;left.xRot=-right.xRot;rightLeg.xRot=(float)Math.cos(swing*.6662)*.8f*amount;leftLeg.xRot=-rightLeg.xRot;
        body.xRot=.035f;wheel.yRot=entity.wheelAngle(partial);wheelMount.y+=(float)Math.sin(age*.07)*.25f;
        float hit=(float)Math.sin(entity.attackProgress(partial)*Math.PI);if(entity.attackType()>0){right.xRot-=hit*1.6f;right.zRot+=hit*.5f;body.yRot=-hit*.15f;}
    }
}
