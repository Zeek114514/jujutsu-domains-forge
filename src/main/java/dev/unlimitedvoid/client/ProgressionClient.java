package dev.unlimitedvoid.client;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.network.ProgressionNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class ProgressionClient {
    private static final KeyMapping KEY=new KeyMapping("key.unlimitedvoid.progression",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_P,"category.unlimitedvoid");
    private static ProgressionNetwork.State state;private static Object world;
    public static void receive(ProgressionNetwork.State next){state=next;if(next.open())Minecraft.getInstance().setScreen(new ProgressScreen());}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.level!=world){world=mc.level;state=null;}while(KEY.consumeClick())if(mc.player!=null&&mc.screen==null)ProgressionNetwork.request(true);}
    public static void hud(GuiGraphics g,int height){var mc=Minecraft.getInstance();if(state==null||mc.player==null||mc.options.hideGui||mc.screen!=null||!state.enabled()||state.creative())return;int max=ProgressionRules.maximum(state.xp());g.fill(7,height-57,188,height-21,0xb0181b25);g.drawString(mc.font,"咒力 "+(int)state.energy()+"/"+max+" · Lv"+ProgressionRules.level(state.xp()),12,height-53,0xff91d9f2);g.fill(12,height-41,182,height-36,0xff303947);g.fill(12,height-41,12+(int)(170*state.energy()/max),height-36,0xff65b8e4);g.drawString(mc.font,(state.path()==0?"未觉醒":state.path()==1?"五条路线":"宿傩路线")+" · [P] 成长"+(state.burnout()>0?" · 熔断":""),12,height-33,0xffd1d5dc);}
    public static final class ProgressScreen extends Screen {
        ProgressScreen(){super(Component.literal("术式成长"));}
        @Override protected void init(){addRenderableWidget(Button.builder(Component.literal("火焰特效："+ClientQuality.label()),b->{ClientQuality.cycle();b.setMessage(Component.literal("火焰特效："+ClientQuality.label()));}).bounds(width/2-137,height/2+86,160,20).build());addRenderableWidget(Button.builder(Component.literal("返回"),b->onClose()).bounds(width/2+35,height/2+86,90,20).build());}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);g.fill(width/2-155,height/2-110,width/2+155,height/2+114,0xee18202c);int row=height/2-98;g.drawCenteredString(font,title,width/2,row,0xffffff);row+=20;
            if(state!=null){String path=state.path()==0?"未觉醒":state.path()==1?"五条路线":"宿傩路线";g.drawCenteredString(font,path+" · Lv"+ProgressionRules.level(state.xp())+" · 熟练度 "+state.xp(),width/2,row,0xff91d9f2);row+=18;
                String[] names={"苍","赫","虚式·茈","灶·开","无量空处","伏魔御厨子"};for(int i=0;i<6;i++){boolean unlocked=state.creative()||!state.enabled()||ProgressionRules.unlocked(state.path(),state.xp(),i);g.drawString(font,names[i]+"  "+(unlocked?"已解锁":"Lv"+ProgressionRules.required(i))+"  咒力 "+ProgressionRules.cost(i),width/2-130,row,unlocked?0xffd5f1d6:0xff9299a6);row+=16;}
            }g.drawCenteredString(font,"潜行持眼罩/手指右键，选择初始路线",width/2,height/2+48,0xffc0c6cf);g.drawCenteredString(font,"练习术式、击败怪物提升熟练度",width/2,height/2+63,0xffc0c6cf);super.render(g,x,y,partial);}
    }
    @Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration{
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event){event.register(KEY);}
        @SubscribeEvent public static void overlay(RegisterGuiOverlaysEvent event){event.registerAboveAll("progression",(gui,g,partial,width,height)->hud(g,height));}
    }
}
