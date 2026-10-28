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
        var body=root.addOrReplaceChild("body",ivory().addBox(-10,-21,-5,20,15,10).addBox(-7.5f,-8,-4.5f,15,10,9),PartPose.offset(0,5,0));
        body.addOrReplaceChild("pectorals",ivory().addBox(-10.4f,-20,-7,10,7,3).addBox(.4f,-20,-7,10,7,3),PartPose.ZERO);
        body.addOrReplaceChild("ribs",gray().addBox(-9,-12.5f,-5.7f,4,4,1.4f).addBox(5,-12.5f,-5.7f,4,4,1.4f),PartPose.ZERO);
        var abs=ivory();for(int row=0;row<3;row++){float y=-13+row*3.4f;abs.addBox(-4.8f,y,-6.3f,4.5f,2.8f,1.9f).addBox(.3f,y,-6.3f,4.5f,2.8f,1.9f);}body.addOrReplaceChild("abdominals",abs,PartPose.ZERO);
        body.addOrReplaceChild("trapezius",ivory().addBox(-8,-24,-3.7f,16,5,8).addBox(-3,-21,4,6,15,2.5f),PartPose.ZERO);
        body.addOrReplaceChild("back_lats",ivory().addBox(-11,-20,3,7,12,3.5f).addBox(4,-20,3,7,12,3.5f),PartPose.ZERO);
        body.addOrReplaceChild("sash",dark().addBox(-9,-2,-6,18,5,12).addBox(-3,2,-6.8f,6,14,1.2f).addBox(1,2,-7.5f,3,11,1.3f),PartPose.ZERO);
        var trim=dark().addBox(-8,-20.6f,-7.5f,16,.7f,.7f);for(int i=0;i<5;i++){float x=-7.5f+i*3.5f,y=-21+Math.abs(2-i)*.5f;trim.addBox(x,y,-8,2.3f,2.3f,.7f).addBox(x+.85f,y+2,-7.9f,.7f,2,.6f);}body.addOrReplaceChild("chest_chain",trim,PartPose.ZERO);var holes=ivory();for(int i=0;i<5;i++){float x=-7.5f+i*3.5f,y=-21+Math.abs(2-i)*.5f;holes.addBox(x+.55f,y+.55f,-8.2f,1.2f,1.2f,.5f);}body.addOrReplaceChild("chain_insets",holes,PartPose.ZERO);
        var armR=root.addOrReplaceChild("right_arm",ivory().addBox(-5,-2,-5.5f,10,12,11),PartPose.offset(-14,-13,0));
        var armL=root.addOrReplaceChild("left_arm",ivory().addBox(-5,-2,-5.5f,10,12,11),PartPose.offset(14,-13,0));
        for(var arm:new PartDefinition[]{armR,armL}){
            arm.addOrReplaceChild("deltoid",ivory().addBox(-5.8f,-1,-6.1f,11.6f,6,12.2f),PartPose.ZERO);
            arm.addOrReplaceChild("biceps",ivory().addBox(-4.3f,4,-6,8.6f,6,2),PartPose.ZERO);
            var forearm=arm.addOrReplaceChild("forearm",ivory().addBox(-4,0,-4.5f,8,14,9).addBox(-4.3f,12,-4.8f,8.6f,5.5f,9.6f),PartPose.offset(0,10,0));
            forearm.addOrReplaceChild("raised_ridges",gray().addBox(-4.3f,2,-5,8.6f,1.5f,1.2f).addBox(-4.3f,6,-5,8.6f,1.5f,1.2f).addBox(-4.3f,10,-5,8.6f,1.5f,1.2f),PartPose.ZERO);
        }
        armR.getChild("forearm").addOrReplaceChild("extermination_blade",ivory().addBox(-4,-1,-1,7,17,2).addBox(-3.5f,16,-.9f,6,4,1.8f).addBox(-2.5f,20,-.75f,4,4,1.5f).addBox(-1.5f,24,-.6f,2,3,1.2f),PartPose.offsetAndRotation(-5,0,-1,0,0,.09f));
        var head=root.addOrReplaceChild("head",ivory().addBox(-4,-10,-4,8,8,8).addBox(-4.6f,-2.2f,-4.8f,9.2f,4,8.8f),PartPose.offset(0,-17,0));
        head.addOrReplaceChild("blind_brow",ivory().addBox(-4.4f,-8.5f,-5,8.8f,5.8f,1.7f).addBox(-1.2f,-11,-4,2.4f,8,8.6f),PartPose.ZERO);
        head.addOrReplaceChild("mouth",dark().addBox(-4.05f,-2.6f,-5.35f,8.1f,2.3f,.6f),PartPose.ZERO);
        var teeth=ivory();for(int i=0;i<8;i++)teeth.addBox(-3.8f+i*.96f,-2.4f,-5.8f,.72f,1.6f,.55f);head.addOrReplaceChild("teeth",teeth,PartPose.ZERO);
        for(int side:new int[]{-1,1})for(int tier=0;tier<2;tier++){
            var fin=ivory().addBox(side<0?-9:0,-1,-1.4f,9,2,2.8f).addBox(side<0?-11:9,-.7f,-1,2,1.4f,2);
            head.addOrReplaceChild("fin_"+side+"_"+tier,fin,PartPose.offsetAndRotation(side*3.8f,-6+tier*4,0,0,0,side*(tier==0?-.20f:.13f)));
        }
        for(int side:new int[]{-1,1}){
            var leg=root.addOrReplaceChild(side<0?"right_leg":"left_leg",cloth().addBox(-4.5f,0,-4.5f,9,13,9).addBox(-5,2,-5,10,8,10),PartPose.offset(side*5,7,0));
            leg.addOrReplaceChild("folds",cloth().addBox(-4.5f,1,-5.8f,2.3f,11,1.4f).addBox(.6f,1,-5.7f,2,10.5f,1.3f),PartPose.ZERO);
            leg.addOrReplaceChild("ankle",ivory().addBox(-3,12,-3.2f,6,3.5f,6.4f),PartPose.ZERO);
            leg.addOrReplaceChild("ankle_wrap",dark().addBox(-3.2f,12.6f,-3.4f,6.4f,1.8f,6.8f),PartPose.ZERO);
            leg.addOrReplaceChild("foot",ivory().addBox(-4,14.5f,-7.5f,8,2.5f,12),PartPose.ZERO);
        }
        var mount=root.addOrReplaceChild("wheel_mount",CubeListBuilder.create(),PartPose.offsetAndRotation(0,-33,0,.20f,0,0));var wheel=mount.addOrReplaceChild("wheel",bronze().addBox(-2,-1.8f,-2,4,3.6f,4),PartPose.ZERO);
        for(int i=0;i<8;i++)wheel.addOrReplaceChild("segment_"+i,bronze().addBox(-4.55f,-.7f,10,9.1f,1.4f,2).addBox(-.65f,-.55f,1,1.3f,1.1f,10.5f).addBox(-.8f,-.8f,11,1.6f,1.6f,3.8f).addBox(-1.65f,-1.65f,14,3.3f,3.3f,3.3f),PartPose.rotation(0,i*(float)Math.PI/4,0));
        return LayerDefinition.create(mesh,256,256);
    }
    @Override public ModelPart root(){return root;}
    @Override public void setupAnim(MahoragaEntity entity,float swing,float amount,float age,float yaw,float pitch){
        root.getAllParts().forEach(ModelPart::resetPose);float partial=age-entity.tickCount;
        root.y=(1-entity.emergence(partial))*60;head.yRot=yaw*(float)Math.PI/180*.45f;head.xRot=pitch*(float)Math.PI/180*.35f;
        right.zRot=.10f;left.zRot=-.10f;right.xRot=(float)Math.cos(swing*.6662)*.45f*amount;left.xRot=-right.xRot;rightLeg.xRot=(float)Math.cos(swing*.6662)*.8f*amount;leftLeg.xRot=-rightLeg.xRot;
        body.xRot=.035f;wheel.yRot=entity.wheelAngle(partial);wheelMount.y+=(float)Math.sin(age*.07)*.25f;
        float hit=(float)Math.sin(entity.attackProgress(partial)*Math.PI);if(entity.attackType()>0){right.xRot-=hit*1.6f;right.zRot+=hit*.5f;body.yRot=-hit*.15f;}
    }
}
