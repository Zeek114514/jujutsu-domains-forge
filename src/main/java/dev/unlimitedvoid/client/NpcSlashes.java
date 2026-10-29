package dev.unlimitedvoid.client;
import dev.unlimitedvoid.UnlimitedVoid;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.*;
import java.util.*;
/** Short world-space cuts share the approved Shrine blade silhouette; no permanent decals. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class NpcSlashes {
    private record Cut(Vec3 from,Vec3 to,boolean cleave,long start){}
    private static final ArrayDeque<Cut> CUTS=new ArrayDeque<>();private static net.minecraft.client.multiplayer.ClientLevel level;static int rendered;
    public static void receive(Vec3 from,Vec3 to,boolean cleave,long start){var current=Minecraft.getInstance().level;if(current!=level){CUTS.clear();level=current;}if(current==null)return;while(CUTS.size()>=64)CUTS.removeFirst();CUTS.addLast(new Cut(from,to,cleave,start));}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;var mc=Minecraft.getInstance();if(mc.level!=level){CUTS.clear();level=mc.level;}rendered=0;if(level==null)return;
        CUTS.removeIf(c->level.getGameTime()-c.start>12);if(CUTS.isEmpty())return;
        boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),blend=GL11.glIsEnabled(GL11.GL_BLEND),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcA=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstA=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);var shader=RenderSystem.getShader();float[] color=RenderSystem.getShaderColor().clone();
        try{RenderSystem.enableDepthTest();RenderSystem.depthMask(false);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShader(GameRenderer::getPositionColorShader);
            var buffer=Tesselator.getInstance().getBuilder();buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);var matrix=event.getPoseStack().last().pose();Vec3 camera=event.getCamera().getPosition();
            for(var cut:CUTS){float age=level.getGameTime()+event.getPartialTick()-cut.start;if(age<0||age>12)continue;float alpha=Math.min(1,age/1.5f)*(1-dev.unlimitedvoid.DomainTimeline.smooth((age-3)/9));Vec3 center=cut.from.lerp(cut.to,.5).subtract(camera),direction=cut.to.subtract(cut.from).normalize();if(direction.lengthSqr()<.9)continue;double length=cut.from.distanceTo(cut.to);
                ShrineRenderer.blade(buffer,matrix,center,direction,Vec3.ZERO,length,.055,alpha,false);rendered++;
                if(cut.cleave){ShrineRenderer.blade(buffer,matrix,center,new Vec3(-direction.x,direction.y,direction.z).normalize(),Vec3.ZERO,length*.9,.05,alpha,false);ShrineRenderer.blade(buffer,matrix,center.add(0,.2,0),new Vec3(direction.x,-.15,direction.z).normalize(),Vec3.ZERO,length*1.1,.07,alpha,true);rendered+=2;}
            }BufferUploader.drawWithShader(buffer.end());
        }finally{RenderSystem.setShader(()->shader);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);RenderSystem.depthMask(mask);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();RenderSystem.blendFuncSeparate(src,dst,srcA,dstA);}
    }
}
