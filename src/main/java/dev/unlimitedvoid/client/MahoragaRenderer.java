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
    @Override public void render(MahoragaEntity entity,float yaw,float partial,PoseStack poses,MultiBufferSource buffer,int light){super.render(entity,yaw,partial,poses,buffer,light);}
}
