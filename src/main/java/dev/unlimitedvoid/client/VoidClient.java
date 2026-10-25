package dev.unlimitedvoid.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.UnlimitedVoid;
import dev.unlimitedvoid.DomainKind;
import dev.unlimitedvoid.DomainTimeline;
import dev.unlimitedvoid.network.DomainNetwork.DomainState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/** Receives authoritative domain snapshots, renders shell/interior and handles local input. */
@Mod.EventBusSubscriber(modid = UnlimitedVoid.ID, value = Dist.CLIENT)
public final class VoidClient {
    private static final KeyMapping TOGGLE = new KeyMapping("key.unlimitedvoid.expand",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, "category.unlimitedvoid");
    private static final KeyMapping CYCLE = new KeyMapping("key.unlimitedvoid.cycle",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.unlimitedvoid");
    private static final KeyMapping RESET = new KeyMapping("key.unlimitedvoid.reset",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_K,"category.unlimitedvoid");
    private static int burnout;
    private static DomainKind selected = DomainKind.VOID;
    private static ShaderInstance shader;
    private static java.util.List<dev.unlimitedvoid.network.DomainNetwork.DomainState> states = java.util.List.of();
    private static int sinceSnapshot;
    private static int insideTicks;
    private static java.util.UUID insideOwner;
    private static boolean captive;
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;
    private static boolean errorReported;

    static void setShader(ShaderInstance instance) { shader = instance; }

    public static void toggle() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) dev.unlimitedvoid.network.DomainNetwork.requestToggle();
    }

    public static void receive(java.util.List<dev.unlimitedvoid.network.DomainNetwork.DomainState> snapshot, boolean frozen, DomainKind chosen, int cooldown) {
        states = java.util.List.copyOf(snapshot);
        captive = frozen;
        selected = chosen;
        burnout = cooldown;
        sinceSnapshot = 0;
        lastLevel = Minecraft.getInstance().level;
    }

    public static int debugStateCount() { return renderStates().size(); }
    public static boolean debugInside() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && findInside(mc.player.getEyePosition()) != null;
    }

    public static boolean isCameraInside() {
        Minecraft mc = Minecraft.getInstance();
        if(shader==null || mc.level==null || mc.player==null)return false;
        var d=findVoid(mc.gameRenderer.getMainCamera().getPosition());
        if(d==null)return false;
        // A white veil covers the terrain switch; the fade reverses this on teardown.
        return whiteAt(d,mc.gameRenderer.getMainCamera().getPosition(),0)>0.98f && (d.closing()<0 || d.closing()<10);
    }

    public static boolean shouldHideEntity(net.minecraft.world.entity.Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (shader == null || mc.level == null || mc.player == null) return false;
        var domain = findVoid(mc.gameRenderer.getMainCamera().getPosition());
        if (domain == null || !isCameraInside()) return false;
        return entity.getBoundingBox().getCenter().distanceToSqr(domain.center()) > domain.radius() * domain.radius();
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog event) {
        if (isCameraInside()) {
            event.setNearPlaneDistance(0);
            event.setFarPlaneDistance(512);
            event.setCanceled(true);
        }
    }

    public static java.util.List<DomainState> renderStates() {
        if(sinceSnapshot>40)return java.util.List.of();
        java.util.List<DomainState> result=new java.util.ArrayList<>();
        for(var state:states){
            int age=state.age()+sinceSnapshot,closing=state.closing()<0?-1:Math.min(40,state.closing()+sinceSnapshot);
            if(closing>=40)continue;
            result.add(new DomainState(state.owner(),state.center(),state.axis(),state.maxRadius()*DomainTimeline.growth(state.kind(),age),age,
                Math.max(0,state.remaining()-sinceSnapshot),state.kind(),state.maxRadius(),state.integrity(),closing));
        }
        return result;
    }
    private static Vec3 horizontal(DomainState d){Vec3 a=new Vec3(d.axis().x,0,d.axis().z).normalize();return a.lengthSqr()<0.1?new Vec3(0,0,1):a;}
    private static float whiteAt(DomainState d,Vec3 eye,float partial){
        float age=d.age()+partial;
        float front=-1.2f+2.4f*DomainTimeline.smooth((age-DomainTimeline.chant(d.kind()))/45f);
        float projection=(float)eye.subtract(d.center()).dot(horizontal(d))/d.maxRadius();
        return DomainTimeline.smooth((front-projection+0.08f)/0.16f);
    }
    private static float cosmicAt(DomainState d,Vec3 eye,float partial){
        float front=-1.2f+2.4f*DomainTimeline.smooth((d.age()+partial-DomainTimeline.chant(d.kind())-50)/50f);
        float projection=(float)eye.subtract(d.center()).dot(horizontal(d))/d.maxRadius();
        return DomainTimeline.smooth((front-projection+0.08f)/0.16f);
    }
    public static boolean debugClash(){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null)return false;Vec3 eye=mc.player.getEyePosition();
        return domainOfKind(eye,DomainKind.VOID)!=null && domainOfKind(eye,DomainKind.SHRINE)!=null;
    }
    private static DomainState domainOfKind(Vec3 eye,DomainKind kind){
        for(var d:renderStates())if(d.kind()==kind && d.radius()>0.1 && eye.distanceToSqr(d.center())<d.radius()*d.radius())return d;
        return null;
    }

    private static dev.unlimitedvoid.network.DomainNetwork.DomainState findInside(net.minecraft.world.phys.Vec3 position) {
        var sealed=findVoid(position);
        if (sealed!=null) return sealed;
        for (var state : renderStates()) {
            if (state.radius()>0.1 && position.distanceToSqr(state.center()) < state.radius() * state.radius()) return state;
        }
        return null;
    }

    private static dev.unlimitedvoid.network.DomainNetwork.DomainState findVoid(net.minecraft.world.phys.Vec3 position) {
        for (var state:renderStates()) if (state.kind()==DomainKind.VOID && position.distanceToSqr(state.center())<state.radius()*state.radius()) return state;
        return null;
    }

    @SubscribeEvent
    public static void hud(RenderGuiEvent.Post event) {
        Minecraft mc=Minecraft.getInstance();
        if (mc.player==null || mc.options.hideGui) return;
        event.getGuiGraphics().drawString(mc.font,Component.translatable("hud.unlimitedvoid.selected",
                Component.translatable(selected.translation), CYCLE.getTranslatedKeyMessage(),TOGGLE.getTranslatedKeyMessage()),8,8,0xE0DFD9,true);
        event.getGuiGraphics().drawString(mc.font,Component.translatable("hud.unlimitedvoid.burnout",Math.max(0,burnout-sinceSnapshot+19)/20,RESET.getTranslatedKeyMessage()),8,20,0xCCA0A0,true);
        if(debugClash())event.getGuiGraphics().drawString(mc.font,Component.literal("领域对峙 · 重叠区必中抵消"),8,56,0xDFB5DA,true);
        for(var state:renderStates()) if(state.owner().equals(mc.player.getUUID())) {
            Component status=state.kind()==DomainKind.SHRINE
                    ? Component.translatable("hud.unlimitedvoid.open_radius",(int)state.maxRadius())
                    : Component.translatable("hud.unlimitedvoid.shell_integrity",Math.round(state.integrity()*100));
            event.getGuiGraphics().drawString(mc.font,status,8,32,0xC7BCA8,true);
            String phase=state.closing()>=0?"领域收束":state.kind()==DomainKind.SHRINE?
                (state.age()<DomainTimeline.formationStart(state.kind())?"咏唱":state.age()<DomainTimeline.formed(state.kind())?"咏唱 · 神龛升起":"领域展开"):
                state.age()<147?"咏唱":state.age()<197?"白色空间推进":state.age()<247?"宇宙显现":"领域展开";
            event.getGuiGraphics().drawString(mc.font,Component.literal(phase),8,44,0xD5D5E8,true);
            break;
        }
    }

    @SubscribeEvent
    public static void tint(ViewportEvent.ComputeFogColor event) {
        var active=findInside(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
        if (active!=null && active.kind()==DomainKind.SHRINE) {
            float f=DomainTimeline.sky(active.kind(),active.age())*DomainTimeline.fade(active.closing());
            event.setRed(event.getRed()*(1-0.3f*f)+0.16f*f);
            event.setGreen(event.getGreen()*(1-0.6f*f));
            event.setBlue(event.getBlue()*(1-0.55f*f));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.level != lastLevel) {
            states = java.util.List.of(); captive = false; insideOwner = null; insideTicks = 0;
            lastLevel = mc.level;
            ClientDomainAudio.clear();
            ShrineFloorRenderer.clear();
            if (mc.level == null || mc.player == null) return;
        }
        while (TOGGLE.consumeClick()) if (mc.screen == null) toggle();
        while (RESET.consumeClick()) if (mc.screen == null) dev.unlimitedvoid.network.DomainNetwork.requestReset();
        while (CYCLE.consumeClick()) if (mc.screen == null) dev.unlimitedvoid.network.DomainNetwork.requestCycle();
        if (mc.isPaused()) return;
        sinceSnapshot++;
        if (sinceSnapshot > 40) captive = false;
        var active = findInside(mc.player.getEyePosition());
        if (active == null) { insideOwner = null; insideTicks = 0; }
        else if (!active.owner().equals(insideOwner)) {
            insideOwner = active.owner(); insideTicks = 1;

        } else insideTicks++;
        ClientDomainAudio.tick(renderStates());
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent event) {
        if (!captive) return;
        var input = event.getInput();
        input.forwardImpulse = 0; input.leftImpulse = 0;
        input.jumping = false; input.shiftKeyDown = false;
        input.up = false; input.down = false; input.left = false; input.right = false;
    }

    @SubscribeEvent
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (captive) { event.setCanceled(true); event.setSwingHand(false); }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS){
            if(!isCameraInside())ShrineFloorRenderer.render(event,renderStates());
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            BarrierRenderer.render(event, renderStates());
            ShrineRenderer.render(event,renderStates());
            drawSky(event,true);
            return;
        }
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY){
            drawSky(event,false);
            // Cancelling chunk layers also suppresses Forge's cutout-stage event.
            // In a Void clash the terrain is hidden, so draw the mirror here instead.
            if(isCameraInside())ShrineFloorRenderer.render(event,renderStates());
        }
    }
    private static void drawSky(RenderLevelStageEvent event,boolean overlay){
        Minecraft mc=Minecraft.getInstance();if(shader==null)return;
        var active=findInside(event.getCamera().getPosition());if(active==null)return;
        float partial=mc.isPaused()?0:event.getPartialTick();
        float ticks=active.age()+partial;
        float fadeIn=smooth(Math.min(1,(insideTicks+partial)/10f));
        float fadeOut=DomainTimeline.fade(active.closing()<0?-1:Math.min(40,active.closing()));
        var voidDomain=domainOfKind(event.getCamera().getPosition(),DomainKind.VOID);
        var shrineDomain=domainOfKind(event.getCamera().getPosition(),DomainKind.SHRINE);
        boolean clash=voidDomain!=null && shrineDomain!=null;
        if(clash)fadeOut=Math.max(fadeOut,DomainTimeline.fade(shrineDomain.closing()));
        if(overlay && active.kind()!=DomainKind.VOID)return;
        var axis = active.axis();
        // Draw the cosmic sky before entities. Client mixins suppress terrain only
        // inside the sphere, so enemies remain visible against the domain interior.
        boolean blend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        boolean depth = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        boolean cull = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        boolean depthMask = org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);
        ShaderInstance previous = RenderSystem.getShader();
        try {
            Vector3f right = new Vector3f(event.getCamera().getLeftVector()).negate();
            Vector3f up = new Vector3f(event.getCamera().getUpVector());
            Vector3f forward = new Vector3f(event.getCamera().getLookVector());
            // Read the effective projection, including sprint and dynamic FOV.
            float tanHalfFov = 1f / event.getProjectionMatrix().m11();
            shader.safeGetUniform("Resolution").set((float) mc.getWindow().getWidth(), (float) mc.getWindow().getHeight());
            shader.safeGetUniform("Time").set(ticks / 20f);
            shader.safeGetUniform("DomainStyle").set(active.kind()==DomainKind.SHRINE?1f:0f);
            shader.safeGetUniform("DomainAge").set(ticks);
            shader.safeGetUniform("ShrineSky").set(DomainTimeline.sky(DomainKind.SHRINE,ticks));
            shader.safeGetUniform("VoidOpacity").set(voidDomain==null?0:DomainTimeline.fade(voidDomain.closing()));
            shader.safeGetUniform("ShrineOpacity").set(shrineDomain==null?0:DomainTimeline.fade(shrineDomain.closing()));
            shader.safeGetUniform("ClashAmount").set(clash?DomainTimeline.sky(DomainKind.SHRINE,shrineDomain.age()+partial):0f);
            Vec3 normal=clash?shrineDomain.center().subtract(voidDomain.center()).normalize():new Vec3(1,0,0);
            if(normal.lengthSqr()<0.1)normal=new Vec3(1,0,0);
            shader.safeGetUniform("ClashNormal").set((float)normal.x,(float)normal.y,(float)normal.z);
            Vec3 offset=event.getCamera().getPosition().subtract(active.center());
            shader.safeGetUniform("CameraOffset").set((float)offset.x,(float)offset.y,(float)offset.z);
            shader.safeGetUniform("DomainRadius").set(active.maxRadius());
            shader.safeGetUniform("Closing").set((float)active.closing());
            shader.safeGetUniform("Overlay").set(overlay?1f:0f);
            float veil=active.kind()==DomainKind.VOID?whiteAt(active,event.getCamera().getPosition(),partial)*(1-cosmicAt(active,event.getCamera().getPosition(),partial)):0;
            if(active.closing()>=0)veil=Math.max(veil,DomainTimeline.smooth(active.closing()/10f)*(1-DomainTimeline.smooth((active.closing()-10)/30f)));
            shader.safeGetUniform("Veil").set(veil);
            shader.safeGetUniform("Opacity").set(fadeIn * fadeOut);
            shader.safeGetUniform("TanHalfFov").set(tanHalfFov);
            shader.safeGetUniform("CameraRight").set(right.x, right.y, right.z);
            shader.safeGetUniform("CameraUp").set(up.x, up.y, up.z);
            shader.safeGetUniform("CameraForward").set(forward.x, forward.y, forward.z);
            shader.safeGetUniform("VoidAxis").set((float)axis.x, (float)axis.y, (float)axis.z);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(() -> shader);
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            buffer.vertex(-1, -1, 0).endVertex();
            buffer.vertex(1, -1, 0).endVertex();
            buffer.vertex(1, 1, 0).endVertex();
            buffer.vertex(-1, 1, 0).endVertex();
            BufferUploader.drawWithShader(buffer.end());
        } catch (RuntimeException exception) {
            shader = null;
            if (!errorReported) {
                errorReported = true;
                LogUtils.getLogger().error("Unlimited Void rendering failed; effect stopped", exception);
                mc.player.displayClientMessage(Component.translatable("message.unlimitedvoid.shader_missing"), false);
            }
        } finally {
            RenderSystem.depthMask(depthMask);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            RenderSystem.setShader(() -> previous);
        }
    }

    private static float smooth(float value) { return value * value * (3 - 2 * value); }

    @Mod.EventBusSubscriber(modid = UnlimitedVoid.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) { event.register(TOGGLE); event.register(CYCLE); event.register(RESET); }

        @SubscribeEvent
        public static void shaders(RegisterShadersEvent event) throws java.io.IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation(UnlimitedVoid.ID,"shrine_floor"),DefaultVertexFormat.POSITION),ShrineFloorRenderer::setShader);
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation(UnlimitedVoid.ID, "unlimited_void"), DefaultVertexFormat.POSITION),
                    VoidClient::setShader);
        }
    }
}
