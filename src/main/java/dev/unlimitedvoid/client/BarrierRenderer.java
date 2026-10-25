package dev.unlimitedvoid.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.unlimitedvoid.network.DomainNetwork;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;

/** World-space, depth-writing shell. Interior visuals are handled separately. */
public final class BarrierRenderer {
    private static final int LONGITUDES = 64;
    private static final int LATITUDES = 32;
    private static final float[] UNIT_MESH = makeMesh();

    private BarrierRenderer() {}

    public static void render(RenderLevelStageEvent event, List<DomainNetwork.DomainState> states) {
        if (states.isEmpty()) return;
        Vec3 camera = event.getCamera().getPosition();
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        float[] color = RenderSystem.getShaderColor().clone();
        ShaderInstance previousShader = RenderSystem.getShader();
        PoseStack poses = event.getPoseStack();

        try {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            // Two-sided triangles also avoid winding sensitivity across render mods.
            RenderSystem.disableCull();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            for (DomainNetwork.DomainState state : states) {
                if (state.kind()!=dev.unlimitedvoid.DomainKind.VOID) continue;
                float fade=dev.unlimitedvoid.DomainTimeline.fade(state.closing());
                float radius=state.radius()*(0.65f+0.35f*fade);
                if (!Float.isFinite(radius) || radius <= 0.025f || radius > 128) continue;
                Vec3 delta = state.center().subtract(camera);
                double distance = delta.length();
                if (distance <= radius + 0.01) continue;

                if(fade<1){RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.depthMask(false);}else{RenderSystem.disableBlend();RenderSystem.depthMask(true);}
                poses.pushPose();
                try {
                    poses.translate(delta.x, delta.y, delta.z);
                    Matrix4f matrix = poses.last().pose();
                    Vec3 towardCamera = delta.scale(-1.0 / distance);
                    BufferBuilder vertices = Tesselator.getInstance().getBuilder();
                    vertices.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
                    for (int i = 0; i < UNIT_MESH.length; i += 3) {
                        float x = UNIT_MESH[i], y = UNIT_MESH[i + 1], z = UNIT_MESH[i + 2];
                        double facing = Math.max(0, x * towardCamera.x + y * towardCamera.y + z * towardCamera.z);
                        // Nearly black body with a quiet charcoal rim: no transparency,
                        // so interior terrain and entities cannot show through the shell.
                        float rim = (float) Math.pow(1 - facing, 4);
                        float grain = (float) (0.5 + 0.5 * Math.sin(x * 17 + y * 11 + z * 23));
                        float crack=(1-state.integrity())*(float)Math.pow(Math.max(0,Math.cos(x*37+y*23-z*19)),24);
                        float shade = 0.003f + 0.025f * rim + 0.002f * grain + crack*0.75f;
                        vertices.vertex(matrix, x * radius, y * radius, z * radius)
                                .color(shade * 0.82f, shade * 0.88f, shade, fade).endVertex();
                    }
                    BufferUploader.drawWithShader(vertices.end());
                } finally {
                    poses.popPose();
                }
            }
        } finally {
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            RenderSystem.depthMask(depthWrite);
            RenderSystem.depthFunc(depthFunction);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            RenderSystem.setShader(() -> previousShader);
        }
    }

    private static float[] makeMesh() {
        float[] mesh = new float[LONGITUDES * (LATITUDES - 1) * 6 * 3];
        int index = 0;
        for (int latitude = 0; latitude < LATITUDES; latitude++) {
            double lower = -Math.PI / 2 + latitude * Math.PI / LATITUDES;
            double upper = -Math.PI / 2 + (latitude + 1) * Math.PI / LATITUDES;
            for (int longitude = 0; longitude < LONGITUDES; longitude++) {
                double left = longitude * Math.PI * 2 / LONGITUDES;
                double right = (longitude + 1) * Math.PI * 2 / LONGITUDES;
                // Skip pole triangles with two coincident vertices.
                if (latitude != 0) {
                    index = vertex(mesh, index, lower, left);
                    index = vertex(mesh, index, lower, right);
                    index = vertex(mesh, index, upper, left);
                }
                if (latitude != LATITUDES - 1) {
                    index = vertex(mesh, index, upper, left);
                    index = vertex(mesh, index, lower, right);
                    index = vertex(mesh, index, upper, right);
                }
            }
        }
        if (index != mesh.length) throw new IllegalStateException("Invalid barrier mesh size");
        return mesh;
    }

    private static int vertex(float[] mesh, int index, double latitude, double longitude) {
        mesh[index++] = (float) (Math.cos(latitude) * Math.cos(longitude));
        mesh[index++] = (float) Math.sin(latitude);
        mesh[index++] = (float) (Math.cos(latitude) * Math.sin(longitude));
        return index;
    }
}
