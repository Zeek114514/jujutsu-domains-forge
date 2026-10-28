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

/** Detailed segmented block models with a 256x256 atlas, retaining vanilla limb animation. */
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
    private static final class DetailCubes extends CubeListBuilder {
        private final float uvScale;
        DetailCubes(float uvScale){this.uvScale=uvScale;}
        @Override public CubeListBuilder addBox(float x,float y,float z,float w,float h,float d){return super.addBox(x,y,z,w,h,d,CubeDeformation.NONE,uvScale,uvScale);}
        @Override public CubeListBuilder addBox(float x,float y,float z,float w,float h,float d,CubeDeformation deformation){return super.addBox(x,y,z,w,h,d,deformation,uvScale,uvScale);}
    }
    private static CubeListBuilder cube(int u,int v){boolean red=u==0&&v==48;return new DetailCubes(red?.5f:.25f).texOffs(red?u*2:u,red?v*2:v);}
    private static void part(PartDefinition parent,String name,int u,int v,float x,float y,float z,float w,float h,float d){
        parent.addOrReplaceChild(name,cube(u,v).addBox(x,y,z,w,h,d),PartPose.ZERO);
    }
    private static void lock(PartDefinition head,String name,float x,float y,float z,float w,float h,float rx,float ry,float rz,int tint){
        // Three overlapping tapered blocks form one angled lock, instead of a row of square pegs.
        var shape=cube(tint,32).addBox(-w*.5f,-h*.42f,-w*.4f,w,h*.48f,w*.8f)
            .addBox(-w*.37f,-h*.76f,-w*.32f,w*.74f,h*.40f,w*.64f)
            .addBox(-w*.20f,-h,-w*.20f,w*.40f,h*.32f,w*.40f);
        head.addOrReplaceChild(name,shape,PartPose.offsetAndRotation(x,y,z,rx,ry,rz));
    }
    static LayerDefinition layer(boolean gojo){
        var mesh=HumanoidModel.createMesh(CubeDeformation.NONE,0);var root=mesh.getRoot();
        var head=root.addOrReplaceChild("head",cube(0,0).addBox(-4,-8,-4,8,8,8),PartPose.ZERO);
        root.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
        var body=root.addOrReplaceChild("body",cube(16,16).addBox(-4,0,-2,8,12,4,new CubeDeformation(.10f)),PartPose.ZERO);
        var ra=root.addOrReplaceChild("right_arm",cube(40,16).addBox(-3,-2,-2,4,12,4),PartPose.offset(-5,2,0));
        var la=root.addOrReplaceChild("left_arm",cube(32,48).addBox(-1,-2,-2,4,12,4),PartPose.offset(5,2,0));
        var rl=root.addOrReplaceChild("right_leg",cube(0,16).addBox(-2,0,-2,4,12,4),PartPose.offset(-1.9f,12,0));
        var ll=root.addOrReplaceChild("left_leg",cube(16,48).addBox(-2,0,-2,4,12,4),PartPose.offset(1.9f,12,0));
        part(head,"hair_crown",0,32,-4.15f,-8.25f,-4.1f,8.3f,1.2f,8.2f);
        int i=0;
        for(int row=0;row<4;row++)for(int col=0;col<5;col++){
            float x=-3.4f+col*1.7f,z=-2.8f+row*1.85f;
            float h=(gojo?2.4f:1.6f)+(float)Math.sin(col*1.8+row*.9)*.6f;
            lock(head,"crown_"+i++,x,-7.7f,z,gojo?1.7f:1.5f,h,-.22f+row*.12f,col*.18f,(col-2)*-.12f,(col+row)%3*8);
        }
        for(int side:new int[]{-1,1}){
            for(int row=0;row<4;row++)lock(head,"side_"+i++,side*3.8f,-5.2f,-2.4f+row*1.7f,1.45f,gojo?2.7f:2.0f,.25f,row*.3f,side*-.38f,row%3*8);
            for(int row=0;row<3;row++)lock(head,"nape_"+i++,side*(1.0f+row*1.2f),-4.6f,3.8f,1.5f,gojo?2.8f:2.2f,-.40f,0,side*.20f,row%3*8);
        }
        for(int col=0;col<(gojo?7:5);col++){
            float x=gojo?-3.6f+col*1.17f:-3.3f+col*1.65f;
            // Gojo's swept bangs descend, Sukuna's short swept-back fringe leaves forehead tattoos exposed.
            lock(head,"bang_"+col,x,gojo?-5.25f+(float)Math.sin(col*1.7)*.55f:-7.0f,-4.2f,gojo?1.2f:1.35f,gojo?2.5f+(col%3)*.22f:1.5f,-.12f,.15f,(col%2==0?1:-1)*.27f,col%3*8);
        }
        // Small block ears remain attached to the rotating head.
        part(head,"ears",56,8,-4.3f,-3.5f,-.8f,.5f,1.8f,1.3f);
        part(head,"ear_left",56,8,3.8f,-3.5f,-.8f,.5f,1.8f,1.3f);
        if(gojo){
            var glasses=cube(32,0);
            for(float cx:new float[]{-2.0f,2.0f}){
                // Stepped octagonal lenses with thin side arms and a separate bridge.
                glasses.addBox(cx-.9f,-4.35f,-4.55f,1.8f,2.1f,.35f)
                    .addBox(cx-1.2f,-4.05f,-4.55f,2.4f,1.5f,.35f);
            }
            glasses.addBox(-.85f,-3.65f,-4.45f,1.7f,.22f,.22f)
                .addBox(-4.12f,-3.65f,-4.25f,.16f,.24f,3.8f).addBox(3.96f,-3.65f,-4.25f,.16f,.24f,3.8f);
            head.addOrReplaceChild("round_sunglasses",glasses,PartPose.offset(0,-.65f,0));
        }
        int collarU=gojo?36:0,collarV=gojo?32:48;
        part(body,"collar_back",collarU,collarV,-3.4f,-1.7f,1.0f,6.8f,3.4f,1.5f);
        part(body,"collar_right",collarU,collarV,-3.5f,-1.5f,-2.1f,1.5f,3.0f,3.5f);
        part(body,"collar_left",collarU,collarV,2,-1.5f,-2.1f,1.5f,3.0f,3.5f);
        if(!gojo){
            part(body,"hood_fold",0,48,-3.7f,.3f,2,7.4f,3,1.7f);
            body.addOrReplaceChild("red_lapel_left",cube(0,48).addBox(-1.4f,0,-.4f,2.8f,3.2f,.8f),PartPose.offsetAndRotation(-1.6f,-.1f,-2.35f,0,0,-.24f));
            body.addOrReplaceChild("red_lapel_right",cube(0,48).addBox(-1.4f,0,-.4f,2.8f,3.2f,.8f),PartPose.offsetAndRotation(1.6f,-.1f,-2.35f,0,0,.24f));
        }
        part(body,"jacket_left",36,32,-3.95f,2.4f,-2.35f,3.65f,9.8f,.38f);
        part(body,"jacket_right",40,32,.20f,2.4f,-2.35f,3.75f,9.8f,.38f);
        part(body,"front_seam",48,32,-.13f,2.2f,-2.42f,.22f,9.7f,.14f);
        part(body,"hem",36,32,-4.2f,11.35f,-2.3f,8.4f,1.0f,4.6f);
        part(body,"gold_button",56,0,2.4f,2.8f,-2.85f,.65f,.65f,.25f);
        part(body,"pocket_edge",48,32,-3.2f,7,-2.62f,2.5f,.18f,.18f);
        if(!gojo)part(body,"hood_button",56,0,-.30f,1.1f,-3,.6f,.65f,.25f);
        int index=0;
        for(var arm:new PartDefinition[]{ra,la}){
            float left=index++==0?-3:-1;
            part(arm,"upper_sleeve",36,32,left-.15f,-2.15f,-2.15f,4.3f,5.5f,4.3f);
            part(arm,"forearm_sleeve",40,32,left-.12f,3.6f,-2.12f,4.24f,4.7f,4.24f);
            part(arm,"cuff",gojo?36:0,gojo?32:48,left-.22f,7.3f,-2.22f,4.44f,1.1f,4.44f);
        }
        for(var leg:new PartDefinition[]{rl,ll}){
            part(leg,"trouser_cuff",36,32,-2.15f,8.6f,-2.15f,4.3f,1,4.3f);
            part(leg,"boot",32,0,-2.05f,9.4f,-3.15f,4.1f,2.4f,5.25f);
            part(leg,"sole",32,0,-2.2f,11.55f,-3.3f,4.4f,.5f,5.5f);
            part(leg,"toe_panel",gojo?36:0,gojo?32:48,-1.8f,10.25f,-3.35f,3.6f,.8f,.3f);
            if(!gojo){
                part(leg,"shoe_tongue",0,48,-1.15f,9.2f,-2.65f,2.3f,1.5f,.4f);
                for(int row=0;row<3;row++)part(leg,"lace"+row,36,32,-.85f,9.45f+row*.32f,-2.91f,1.7f,.11f,.12f);
            }
        }
        return LayerDefinition.create(mesh,256,256);
    }
}
