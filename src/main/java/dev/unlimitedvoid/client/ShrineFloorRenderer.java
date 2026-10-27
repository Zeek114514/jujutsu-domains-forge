package dev.unlimitedvoid.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.ShrineEntity;
import dev.unlimitedvoid.network.DomainNetwork.DomainState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import java.util.*;

/** A temporary world-space mirror. Never edits terrain, fluid or collision. */
public final class ShrineFloorRenderer {
    private static ShaderInstance shader;
    private static RenderTarget reflection;
    private static final MultiBufferSource.BufferSource MIRROR_BUFFERS=MultiBufferSource.immediate(new BufferBuilder(262144));
    private static boolean reflecting,failed;
    private static int frames,reflectedCount,shrineCount;
    private static final Map<UUID,Double> SURFACES=new HashMap<>();
    public static void setShader(ShaderInstance value){shader=value;failed=false;}
    public static boolean isReflecting(){return reflecting;}
    public static boolean hasTarget(){return reflection!=null;}
    public static int reflectedCount(){return reflectedCount;}
    public static int shrineCount(){return shrineCount;}
    public static int frames(){return frames;}
    private static void release(){
        if(reflection!=null){
            var target=FramebufferState.capture();boolean bound=target.draw()==reflection.frameBufferId||target.read()==reflection.frameBufferId;
            reflection.destroyBuffers();reflection=null;
            if(bound)Minecraft.getInstance().getMainRenderTarget().bindWrite(true);else target.restore();
        }
        reflectedCount=0;shrineCount=0;
    }
    public static void clear(){Runnable clean=()->{release();SURFACES.clear();};if(RenderSystem.isOnRenderThread())clean.run();else RenderSystem.recordRenderCall(clean::run);}

    public static void render(RenderLevelStageEvent event,List<DomainState> states){
        Minecraft mc=Minecraft.getInstance();Vec3 camera=event.getCamera().getPosition();
        SURFACES.keySet().removeIf(id->states.stream().noneMatch(d->d.owner().equals(id)));
        List<DomainState> visible=states.stream().filter(d->d.kind()==DomainKind.SHRINE && opacity(d,event.getPartialTick())>0.001f
            && camera.distanceToSqr(d.center())<(d.maxRadius()+48)*(d.maxRadius()+48))
            .sorted(Comparator.comparingDouble(d->camera.distanceToSqr(d.center()))).limit(2).toList();
        if(visible.isEmpty() || mc.level==null || shader==null || failed){release();return;}
        boolean blend=GL11.glIsEnabled(GL11.GL_BLEND),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC),front=GL11.glGetInteger(GL11.GL_FRONT_FACE);
        var target=FramebufferState.capture();
        int src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int texture=RenderSystem.getShaderTexture(0);
        float[] color=RenderSystem.getShaderColor().clone();ShaderInstance previous=RenderSystem.getShader();
        try {
            ensureTarget(mc);
            target.restore();
            for(DomainState d:visible){
                double floor=SURFACES.computeIfAbsent(d.owner(),id->surfaceHeight(d))+0.025;
                if(camera.y<=floor+0.01)continue;
                renderReflections(mc,event,d,floor);
                target.restore();
                GL11.glFrontFace(front);
                // Terrain is already drawn; living entities and the shrine are drawn next.
                // Replace ground colour even when it lies above the visual plane, while
                // retaining its original depth so later entities keep correct occlusion.
                boolean inside=camera.distanceToSqr(d.center())<d.radius()*d.radius();
                RenderSystem.enableDepthTest();RenderSystem.depthFunc(inside?GL11.GL_ALWAYS:GL11.GL_LEQUAL);RenderSystem.depthMask(false);
                RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,1);
                shader.setSampler("Reflection",reflection.getColorTextureId());
                shader.safeGetUniform("Resolution").set((float)target.width(),(float)target.height());
                shader.safeGetUniform("Time").set((d.age()+event.getPartialTick())/20f);
                shader.safeGetUniform("Opacity").set(opacity(d,event.getPartialTick()));
                shader.safeGetUniform("Radius").set(d.radius());
                shader.safeGetUniform("CameraLocal").set((float)(camera.x-d.center().x),(float)(camera.y-floor),(float)(camera.z-d.center().z));
                DomainState opponent=states.stream().filter(o->o.kind()==DomainKind.VOID && o.radius()>1 && o.center().distanceTo(d.center())<o.radius()+d.radius()).findFirst().orElse(null);
                Vec3 normal=opponent==null?new Vec3(1,0,0):d.center().subtract(opponent.center()).normalize();
                if(normal.lengthSqr()<0.01)normal=new Vec3(1,0,0);
                Vec3 midpoint=opponent==null?d.center():d.center().add(opponent.center()).scale(0.5);
                shader.safeGetUniform("ClashNormal").set((float)normal.x,(float)normal.y,(float)normal.z);
                shader.safeGetUniform("ClashOffset").set((float)d.center().subtract(midpoint).dot(normal));
                shader.safeGetUniform("Clash").set(opponent==null?0:DomainTimeline.smooth((opponent.age()-197)/50f)*DomainTimeline.fade(opponent.closing()));
                RenderSystem.setShader(()->shader);
                event.getPoseStack().pushPose();
                try {
                    event.getPoseStack().translate(d.center().x-camera.x,floor-camera.y,d.center().z-camera.z);
                    Matrix4f matrix=event.getPoseStack().last().pose();
                    shader.safeGetUniform("FloorMat").set(matrix);
                    shader.safeGetUniform("FloorProjection").set(event.getProjectionMatrix());
                    BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION);
                    float r=d.radius();b.vertex(-r,0,-r).endVertex();b.vertex(-r,0,r).endVertex();b.vertex(r,0,r).endVertex();b.vertex(r,0,-r).endVertex();
                    BufferUploader.drawWithShader(b.end());frames++;
                } finally {event.getPoseStack().popPose();}
            }
        } catch(RuntimeException error){
            failed=true;com.mojang.logging.LogUtils.getLogger().error("Shrine mirror rendering failed",error);
        } finally {
            reflecting=false;target.restore();GL11.glFrontFace(front);
            RenderSystem.setShaderTexture(0,texture);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
            RenderSystem.depthMask(depthMask);RenderSystem.depthFunc(depthFunc);
            RenderSystem.blendFuncSeparate(src,dst,srcAlpha,dstAlpha);
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            RenderSystem.setShader(()->previous);
        }
    }
    public static float opacity(DomainState state,float partial){return DomainTimeline.sky(DomainKind.SHRINE,state.age()+partial)*DomainTimeline.fade(state.closing());}
    private static double surfaceHeight(DomainState state){
        // The server selected the actual support surface, including slab/stair height.
        return state.center().y-1;
    }
    private static void ensureTarget(Minecraft mc){
        int w=Math.min(1280,mc.getMainRenderTarget().width),h=Math.max(1,Math.round(w*(float)mc.getMainRenderTarget().height/mc.getMainRenderTarget().width));
        if(reflection==null || reflection.width!=w || reflection.height!=h){release();reflection=new TextureTarget(w,h,true,Minecraft.ON_OSX);reflection.setClearColor(0,0,0,0);reflection.setFilterMode(GL11.GL_LINEAR);}
    }
    private static void renderReflections(Minecraft mc,RenderLevelStageEvent event,DomainState d,double floor){
        Vec3 camera=event.getCamera().getPosition();float partial=event.getPartialTick();
        // This pass now precedes normal entity drawing; explicitly prepare its camera.
        mc.getEntityRenderDispatcher().prepare(mc.level,event.getCamera(),mc.crosshairPickEntity);
        List<Entity> entities=new ArrayList<>();
        for(Entity e:mc.level.entitiesForRendering())if(e.isAlive() && !e.isSpectator() && (e instanceof LivingEntity || e instanceof ShrineEntity)
            && e.position().distanceToSqr(camera)<64*64 && e.position().distanceToSqr(d.center())<d.radius()*d.radius()
            && e.getBoundingBox().maxY>floor)entities.add(e);
        entities.sort(Comparator.<Entity>comparingInt(e->e instanceof ShrineEntity?0:1).thenComparingDouble(e->e.position().distanceToSqr(camera)));
        if(entities.size()>48)entities=entities.subList(0,48);
        reflectedCount=0;shrineCount=0;
        reflection.clear(Minecraft.ON_OSX);reflection.bindWrite(true);
        RenderSystem.enableDepthTest();RenderSystem.depthMask(true);RenderSystem.setShaderColor(1,1,1,1);
        GL11.glFrontFace(GL11.GL_CW);
        boolean hitBoxes=mc.getEntityRenderDispatcher().shouldRenderHitBoxes();
        mc.getEntityRenderDispatcher().setRenderHitBoxes(false);
        PoseStack poses=event.getPoseStack();poses.pushPose();reflecting=true;
        try {
            poses.translate(0,2*(floor-camera.y),0);poses.scale(1,-1,1);
            for(Entity e:entities){
                double x=Mth.lerp(partial,e.xo,e.getX())-camera.x,y=Mth.lerp(partial,e.yo,e.getY())-camera.y,z=Mth.lerp(partial,e.zo,e.getZ())-camera.z;
                mc.getEntityRenderDispatcher().render(e,x,y,z,Mth.rotLerp(partial,e.yRotO,e.getYRot()),partial,poses,MIRROR_BUFFERS,LightTexture.FULL_BRIGHT);
                reflectedCount++;if(e instanceof ShrineEntity)shrineCount++;
            }
            MIRROR_BUFFERS.endBatch();
        } finally {reflecting=false;poses.popPose();mc.getEntityRenderDispatcher().setRenderHitBoxes(hitBoxes);}
    }
}
