package dev.unlimitedvoid.client;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.TextureUtil;
import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.unlimitedvoid.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.*;
import java.util.*;

/** Analytic world-space spheres with procedural interiors and captured-scene refraction. */
public final class LimitlessRenderer {
    private static ShaderInstance shader;
    private static RenderTarget scene;
    private static int worldDepth;
    private record Frame(Vec3 camera,Vec3 right,Vec3 up,Matrix4f view,Matrix4f projection,float partial,List<LimitlessClient.Effect> effects){}
    private static Frame frame;
    private static long drawNanos;
    static long drawNanos(){return drawNanos;}
    static boolean stateRestored=true;
    private static boolean failed;
    private static int quads;
    private record Ball(Vec3 position,Vec3 axis,float radius,float age,int kind,boolean fusion,float stretch,float opacity){}
    static void setShader(ShaderInstance value){shader=value;failed=false;}
    static int quads(){return quads;}
    static boolean ready(){return shader!=null&&!failed;}
    static void clear(){
        Runnable release=()->{frame=null;if(scene==null&&worldDepth==0)return;var before=FramebufferState.capture();if(scene!=null){scene.destroyBuffers();scene=null;}if(worldDepth!=0){TextureUtil.releaseTextureId(worldDepth);worldDepth=0;}before.restore();};
        if(RenderSystem.isOnRenderThread())release.run();else RenderSystem.recordRenderCall(release::run);
    }
    static void prepare(RenderLevelStageEvent event,Collection<LimitlessClient.Effect> effects){
        quads=0;drawNanos=0;var mc=Minecraft.getInstance();if(effects.isEmpty()||mc.level==null){clear();return;}if(!ready())return;
        var left=event.getCamera().getLeftVector();var top=event.getCamera().getUpVector();
        frame=new Frame(event.getCamera().getPosition(),new Vec3(-left.x,-left.y,-left.z),new Vec3(top.x,top.y,top.z),new Matrix4f(event.getPoseStack().last().pose()),new Matrix4f(event.getProjectionMatrix()),mc.isPaused()?0:event.getPartialTick(),List.copyOf(effects));
        var target=FramebufferState.capture();int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);RenderSystem.activeTexture(GL13.GL_TEXTURE0);int texture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try{
            if(worldDepth==0)worldDepth=TextureUtil.generateTextureId();RenderSystem.bindTexture(worldDepth);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,target.draw());
            GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_DEPTH_COMPONENT32F,target.x(),target.y(),target.width(),target.height(),0);
        }finally{RenderSystem.bindTexture(texture);RenderSystem.activeTexture(active);target.restore();}
    }
    /** Draw after Iris has composited the world, while the hand depth is still available. */
    public static void renderLate(){
        var f=frame;var mc=Minecraft.getInstance();if(f==null||mc.level==null||!ready()||worldDepth==0)return;
        long begin=System.nanoTime();Vec3 camera=f.camera,right=f.right,up=f.up;float partial=f.partial;var effects=f.effects;
        var balls=new ArrayList<Ball>();
        for(var e:effects){
            var s=e.state;if(s.position().distanceToSqr(camera)>256*256)continue;
            float age=mc.level.getGameTime()+partial-s.start(),phaseAge=mc.level.getGameTime()+partial-s.phaseStart();
            Vec3 p=e.position(partial),dir=s.direction(),side=dir.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
            Vec3 vertical=side.cross(dir).normalize();float power=LimitlessKind.smooth(age/s.kind().charge);
            float fade=s.phase()==2?(s.kind()==LimitlessKind.BLUE?1-LimitlessKind.smooth((phaseAge-68)/12):1-LimitlessKind.smooth(phaseAge/16)):1;
            float radius=s.phase()==0?.22f+.67f*power:s.kind()==LimitlessKind.PURPLE?4.5f:s.kind()==LimitlessKind.BLUE?(float)(1.2+LimitlessKind.power(s.charge())*.6):.8f;
            if(s.phase()==1&&mc.player!=null&&s.owner().equals(mc.player.getUUID()))fade*=.12f+.88f*LimitlessKind.smooth((float)(p.distanceTo(camera)/12));
            if(s.phase()==0&&s.kind()==LimitlessKind.PURPLE){
                float merge=LimitlessKind.smooth((age-35)/28);Vec3 axis=side.scale(.85).add(vertical.scale(.52)).normalize();
                Vec3 center=p.add(0,.15,0);float gap=1.65f*(1-merge),ingredients=1-LimitlessKind.smooth((merge-.65f)/.35f);
                balls.add(new Ball(center.add(axis.scale(gap)).add(dir.scale(.12)),axis,.90f+.13f*merge,age,0,true,1+.3f*merge,ingredients));
                balls.add(new Ball(center.subtract(axis.scale(gap)).subtract(dir.scale(.12)),axis,.90f+.13f*merge,-age,1,true,1+.3f*merge,ingredients));
                if(merge>.48f){float formed=LimitlessKind.smooth((merge-.48f)/.52f);balls.add(new Ball(center.subtract(dir.scale(.18)),axis,.22f+1.20f*formed,age,2,false,1,formed));}
            }else{
                if(s.phase()==2&&s.kind()==LimitlessKind.RED)radius+=phaseAge*.23f;
                if(s.phase()!=1||p.distanceTo(camera)>radius*1.05)balls.add(new Ball(p,vertical,radius,age,s.kind().ordinal(),false,1,fade));
                // Tapered travelling afterimages, rather than repeated full-size glowing discs.
                if(s.phase()==1)for(int i=1;i<=4;i++){
                    double behind=i*1.4;if(behind>phaseAge*s.kind().speed)break;
                    Vec3 tail=p.subtract(dir.scale(behind));float size=radius*(1-i*.18f);
                    if(tail.distanceTo(camera)>size*1.3)balls.add(new Ball(tail,vertical,size,age-i,s.kind().ordinal(),false,1,fade*(1-i/5f)*.12f));
                }
            }
        }
        balls.removeIf(b->b.opacity<.008||b.radius<.02);if(balls.isEmpty())return;
        balls.sort(Comparator.comparingDouble((Ball b)->b.position.distanceToSqr(camera)).reversed());
        boolean blend=GL11.glIsEnabled(GL11.GL_BLEND),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC),src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcA=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstA=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        var colorMask=org.lwjgl.BufferUtils.createByteBuffer(4);GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK,colorMask);
        int program=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),equation=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),equationA=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        int texture=RenderSystem.getShaderTexture(0),activeTexture=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);float[] color=RenderSystem.getShaderColor().clone();var previous=RenderSystem.getShader();var target=FramebufferState.capture();
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);int bound0=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);RenderSystem.activeTexture(GL13.GL_TEXTURE1);int bound1=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);RenderSystem.activeTexture(activeTexture);
        try{
            if(target.width()<1||target.height()<1)return;
            float scale=Math.min(1,Math.min(1280f/target.width(),720f/target.height()));int width=Math.max(1,(int)(target.width()*scale)),height=Math.max(1,(int)(target.height()*scale));
            if(scene==null)scene=new TextureTarget(width,height,false,Minecraft.ON_OSX);
            else if(scene.width!=width||scene.height!=height)scene.resize(width,height,Minecraft.ON_OSX);
            target.restore();
            RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL11.GL_LEQUAL);RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.setShaderColor(1,1,1,1);
            RenderSystem.setShader(()->shader);shader.setSampler("Scene",scene.getColorTextureId());shader.setSampler("WorldDepth",worldDepth);
            shader.safeGetUniform("OrbMat").set(f.view);shader.safeGetUniform("OrbProjection").set(f.projection);
            shader.safeGetUniform("Resolution").set((float)target.width(),(float)target.height());shader.safeGetUniform("Viewport").set((float)target.x(),(float)target.y());
            shader.safeGetUniform("Right").set((float)right.x,(float)right.y,(float)right.z);shader.safeGetUniform("Up").set((float)up.x,(float)up.y,(float)up.z);
            for(var b:balls){
                if(quads>=32)break;captureScene(target);RenderSystem.enableBlend();Vec3 center=b.position.subtract(camera);
                shader.safeGetUniform("Center").set((float)center.x,(float)center.y,(float)center.z);shader.safeGetUniform("Axis").set((float)b.axis.x,(float)b.axis.y,(float)b.axis.z);
                shader.safeGetUniform("Radius").set(b.radius);shader.safeGetUniform("Time").set(b.age/20);shader.safeGetUniform("Kind").set((float)b.kind);
                shader.safeGetUniform("Fusion").set(b.fusion?1f:0f);shader.safeGetUniform("Stretch").set(b.stretch);shader.safeGetUniform("Opacity").set(b.opacity);
                Vec3 x=right.scale(b.radius*2.2),y=up.scale(b.radius*2.2);var buffer=Tesselator.getInstance().getBuilder();buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION);
                vertex(buffer,center.subtract(x).subtract(y));vertex(buffer,center.add(x).subtract(y));vertex(buffer,center.add(x).add(y));vertex(buffer,center.subtract(x).add(y));
                shader.apply();
                GL11.glEnable(GL11.GL_BLEND);GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glColorMask(true,true,true,true);
                BufferUploader.draw(buffer.end());
                shader.clear();quads++;
            }
        }catch(RuntimeException error){failed=true;com.mojang.logging.LogUtils.getLogger().error("Limitless sphere rendering failed",error);}
        finally{
            target.restore();RenderSystem.setShaderTexture(0,texture);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);RenderSystem.depthMask(mask);RenderSystem.depthFunc(depthFunc);
            RenderSystem.blendFuncSeparate(src,dst,srcA,dstA);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            RenderSystem.colorMask(colorMask.get(0)!=0,colorMask.get(1)!=0,colorMask.get(2)!=0,colorMask.get(3)!=0);GL11.glColorMask(colorMask.get(0)!=0,colorMask.get(1)!=0,colorMask.get(2)!=0,colorMask.get(3)!=0);
            GL14.glBlendFuncSeparate(src,dst,srcA,dstA);GL20.glBlendEquationSeparate(equation,equationA);if(blend)GL11.glEnable(GL11.GL_BLEND);else GL11.glDisable(GL11.GL_BLEND);GL20.glUseProgram(program);
            RenderSystem.setShader(()->previous);RenderSystem.activeTexture(GL13.GL_TEXTURE0);RenderSystem.bindTexture(bound0);RenderSystem.activeTexture(GL13.GL_TEXTURE1);RenderSystem.bindTexture(bound1);RenderSystem.activeTexture(activeTexture);stateRestored=target.equals(FramebufferState.capture());drawNanos=System.nanoTime()-begin;
        }
    }
    private static void captureScene(FramebufferState target){
        // Capture after each back-to-front sphere so refraction includes the other ingredient.
        int attachment=GL11.glGetInteger(GL20.GL_DRAW_BUFFER0);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,target.draw());int oldReadBuffer=GL11.glGetInteger(GL11.GL_READ_BUFFER);
        try{
            GL11.glReadBuffer(attachment);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,scene.frameBufferId);
            GL30.glBlitFramebuffer(target.x(),target.y(),target.x()+target.width(),target.y()+target.height(),0,0,scene.width,scene.height,GL11.GL_COLOR_BUFFER_BIT,GL11.GL_NEAREST);
        }finally{GL11.glReadBuffer(oldReadBuffer);target.restore();}
    }
    private static void vertex(BufferBuilder buffer,Vec3 p){buffer.vertex(p.x,p.y,p.z).endVertex();}
}
