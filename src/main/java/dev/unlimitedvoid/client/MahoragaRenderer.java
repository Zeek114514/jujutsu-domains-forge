package dev.unlimitedvoid.client;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.MahoragaEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
public final class MahoragaRenderer extends MobRenderer<MahoragaEntity,MahoragaModel> {
    private static final ResourceLocation TEXTURE=new ResourceLocation(UnlimitedVoid.ID,"textures/entity/mahoraga.png");
    public MahoragaRenderer(EntityRendererProvider.Context context){super(context,new MahoragaModel(context.bakeLayer(MahoragaModel.LAYER)),.85f);}
    @Override public ResourceLocation getTextureLocation(MahoragaEntity entity){return TEXTURE;}
    @Override public void render(MahoragaEntity entity,float yaw,float partial,PoseStack poses,MultiBufferSource buffer,int light){super.render(entity,yaw,partial,poses,buffer,light);
        if(entity.emerging()||entity.dismissing()){
            float progress=entity.emergence(partial),alpha=entity.dismissing()?dev.unlimitedvoid.DomainTimeline.smooth(progress*5)*dev.unlimitedvoid.DomainTimeline.smooth((1-progress)*5):1-dev.unlimitedvoid.DomainTimeline.smooth((progress-.75f)/.25f);
            var vertex=buffer.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(new ResourceLocation(UnlimitedVoid.ID,"textures/entity/mahoraga_shadow.png")));var matrix=poses.last().pose();var normal=poses.last().normal();float radius=1.8f;
            float[][] points={{-radius,.022f,-radius,0,0},{-radius,.022f,radius,0,1},{radius,.022f,radius,1,1},{radius,.022f,-radius,1,0}};
            for(var p:points)vertex.vertex(matrix,p[0],p[1],p[2]).color(1,1,1,alpha*.9f).uv(p[3],p[4]).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(light).normal(normal,0,1,0).endVertex();
        }}
}
