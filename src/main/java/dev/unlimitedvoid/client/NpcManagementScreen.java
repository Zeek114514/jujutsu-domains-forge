package dev.unlimitedvoid.client;

import dev.unlimitedvoid.network.NpcControlNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class NpcManagementScreen extends Screen {
    public static NpcControlNetwork.State lastState;
    public static long lastAt;
    private NpcControlNetwork.State state;
    public NpcManagementScreen(NpcControlNetwork.State state){super(Component.literal("角色管理"));this.state=state;}
    public static void receive(NpcControlNetwork.State state){
        var mc=Minecraft.getInstance();if(mc.level==null)return;lastState=state;lastAt=mc.level.getGameTime();
        if(state.open()&&state.entity()<0){if(mc.screen instanceof NpcManagementScreen)mc.setScreen(null);return;}
        if(state.open()){if(mc.screen instanceof NpcManagementScreen screen&&screen.state.entity()==state.entity())screen.state=state;else mc.setScreen(new NpcManagementScreen(state));}
    }
    @Override protected void init(){
        String[] labels={"跟随我","原地待命","自由行动","红方","蓝方","默认阵营","开始对战","重置对战","","攻击最近敌人"};
        int row=0;for(int op=1;op<=10;op++){if(op==9)continue;int action=op;
            addRenderableWidget(Button.builder(Component.literal(labels[op-1]),b->NpcControlNetwork.request(state.entity(),action)).bounds(width/2-132+(row%3)*90,height/2-35+(row/3)*26,86,20).build());row++;
        }
        addRenderableWidget(Button.builder(Component.literal("完成"),b->onClose()).bounds(width/2-45,height/2+65,90,20).build());
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        renderBackground(g);g.fill(width/2-150,height/2-105,width/2+150,height/2+94,0xee18202c);
        g.drawCenteredString(font,state.name(),width/2,height/2-93,0xffffff);
        String[] modes={"自由行动","跟随","待命"},teams={"默认","红方","蓝方"};
        g.drawCenteredString(font,"行为："+modes[Math.max(0,Math.min(2,state.mode()))]+"  阵营："+teams[Math.max(0,Math.min(2,state.team()))],width/2,height/2-76,0xff9ed9e6);
        g.drawCenteredString(font,state.status(),width/2,height/2-61,0xffc0c8d0);
        g.drawCenteredString(font,state.arena()?"对战中：重置可恢复开始前的状态":"对战：先给附近角色分配红蓝双方",width/2,height/2+46,0xffa6acb8);
        super.render(g,mouseX,mouseY,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
}
