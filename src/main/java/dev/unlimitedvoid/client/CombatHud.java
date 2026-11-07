package dev.unlimitedvoid.client;

import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class CombatHud {
    private CombatHud(){}
    private static long lastPoll;private static Object world;
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(mc.level!=world){world=mc.level;lastPoll=-100;NpcManagementScreen.lastState=null;}
        if(mc.level==null||mc.player==null||mc.screen!=null||mc.level.getGameTime()-lastPoll<20)return;
        var target=focused();if(target!=null){lastPoll=mc.level.getGameTime();dev.unlimitedvoid.network.NpcControlNetwork.request(target.getId(),0);}
    }
    @Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration{@SubscribeEvent public static void register(RegisterGuiOverlaysEvent event){event.registerAboveAll("combat_status",(gui,graphics,partial,width,height)->render(graphics,width,height));}}
    public static LivingEntity focused(){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return null;
        Vec3 from=mc.player.getEyePosition(),to=from.add(mc.player.getLookAngle().scale(24));
        double limit=mc.player.pick(24,0,false).getLocation().distanceToSqr(from);LivingEntity nearest=null;
        for(var e:mc.level.getEntities(mc.player,mc.player.getBoundingBox().expandTowards(mc.player.getLookAngle().scale(24)).inflate(1),e->e instanceof JujutsuNpc||e instanceof MahoragaEntity||e instanceof InnocentSunDog)){
            var hit=e.getBoundingBox().inflate(.15).clip(from,to);
            if(hit.isPresent()&&from.distanceToSqr(hit.get())<limit){limit=from.distanceToSqr(hit.get());nearest=(LivingEntity)e;}
        }return nearest;
    }
    private static void render(GuiGraphics g,int width,int height){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.options.hideGui||mc.screen!=null)return;
        LivingEntity e=focused();if(e==null)return;
        int x=width/2-112,y=16;g.fill(x-5,y-4,x+229,y+64,0xc0181b25);
        g.drawString(mc.font,e.getDisplayName(),x,y,0xffffff);y+=12;
        g.fill(x,y,x+224,y+7,0xff383e48);g.fill(x,y,x+(int)(224*Math.max(0,Math.min(1,e.getHealth()/e.getMaxHealth()))),y+7,0xffd95865);
        g.drawString(mc.font,(int)Math.ceil(e.getHealth())+" / "+(int)e.getMaxHealth(),x,y+9,0xffffff);y+=21;
        String state="闲逛";
        if(e instanceof JujutsuNpc n){
            state=n.staggered()?"受击硬直":n.punchKind()>=0?"连招 "+(n.comboStep()+1)+" / 3":switch(n.tactic()){case 1->"侧移避招";case 2->"撤步恢复";case 3->"破绽追击";default->n.getTarget()!=null?"交战中":"警戒";};
            if(n.tactic()==0&&n.punchKind()<0&&!n.staggered()&&NpcManagementScreen.lastState!=null&&NpcManagementScreen.lastState.entity()==n.getId()&&mc.level.getGameTime()-NpcManagementScreen.lastAt<60)state=NpcManagementScreen.lastState.status();
            var pose=LimitlessClient.pose(n.getUUID());if(pose!=null)state="术式蓄力 · "+pose.kind().name();
        }
        for(var d:VoidClient.renderStates())if(d.owner().equals(e.getUUID()))state=Component.translatable(d.kind().translation).getString()+" · "+(d.closing()>=0?"解除中":d.age()<DomainTimeline.chant(d.kind())?"展开中":Math.max(0,d.remaining()/20)+"秒");
        if(e instanceof MahoragaEntity m){
            var text=new StringBuilder("适应 ");for(var kind:MahoragaRules.Kind.values())if(m.adaptation(kind)>0)text.append(Component.translatable("adaptation.unlimitedvoid."+kind.name().toLowerCase(java.util.Locale.ROOT)).getString()).append(m.adaptation(kind)).append("/4 ");
            state=text.length()>3?text.toString():"尚未适应";
        }
        g.drawString(mc.font,mc.font.plainSubstrByWidth(state,224),x,y,0xffb8dbea);
        g.drawString(mc.font,"潜行 + 右键：管理角色",x,y+12,0xffa6acb8);
    }
}
