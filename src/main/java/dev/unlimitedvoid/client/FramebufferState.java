package dev.unlimitedvoid.client;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Preserve the actual caller target, including Oculus/Iris g-buffer passes. */
record FramebufferState(int draw,int read,int x,int y,int width,int height) {
    static FramebufferState capture(){
        int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        return new FramebufferState(GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),viewport[0],viewport[1],viewport[2],viewport[3]);
    }
    void restore(){
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);
        RenderSystem.viewport(x,y,width,height);
    }
}
