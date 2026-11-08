package dev.unlimitedvoid.server;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.network.ProgressionNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID)
public final class ProgressionService {
    private static final Map<UUID,Long> REQUESTS=new HashMap<>();
    public static CompoundTag data(ServerPlayer p){
        var root=p.getPersistentData();if(!root.contains("uv_progression",10)){var d=new CompoundTag();d.putFloat("Energy",100);root.put("uv_progression",d);}return root.getCompound("uv_progression");
    }
    public static int xp(ServerPlayer p){return Math.max(0,Math.min(100000,data(p).getInt("Xp")));}
    public static boolean spend(ServerPlayer p,int ability){
        if(!DomainConfig.SURVIVAL_PROGRESSION.get()||p.getAbilities().instabuild)return true;
        var d=data(p);int path=d.getInt("Path"),xp=xp(p);float energy=d.getFloat("Energy");
        if(!ProgressionRules.unlocked(path,xp,ability)){p.displayClientMessage(Component.literal(path==0?"潜行持眼罩/宿傩手指右键，选择术式路线":"路线不符，或需要熟练等级 "+ProgressionRules.required(ability)),true);sync(p,false);return false;}
        if(!Float.isFinite(energy)||energy<ProgressionRules.cost(ability)){p.displayClientMessage(Component.literal("咒力不足，稍作休息恢复"),true);return false;}
        d.putFloat("Energy",Math.max(0,energy-ProgressionRules.cost(ability)));d.putInt("Xp",Math.min(100000,xp+2));
        d.putInt("Mastery_"+ability,Math.min(10000,d.getInt("Mastery_"+ability)+1));sync(p,false);return true;
    }
    @SubscribeEvent public static void choose(PlayerInteractEvent.RightClickItem event){
        if(event.getHand()!=InteractionHand.MAIN_HAND||!(event.getEntity() instanceof ServerPlayer p)||!p.isShiftKeyDown()||!DomainConfig.SURVIVAL_PROGRESSION.get())return;
        var stack=p.getMainHandItem();int path=stack.is(DomainItems.BLINDFOLD.get())?1:stack.is(DomainItems.SUKUNA_FINGER.get())?2:0;if(path==0)return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
        var d=data(p);if(d.getInt("Path")==0){d.putInt("Path",path);d.putFloat("Energy",ProgressionRules.maximum(xp(p)));if(!p.getAbilities().instabuild)stack.shrink(1);p.displayClientMessage(Component.literal(path==1?"已觉醒五条路线：苍已解锁":"已觉醒宿傩路线：灶·开已解锁"),false);}
        else p.displayClientMessage(Component.literal("已有术式路线，继续练习和击败怪物提升熟练度"),true);sync(p,true);
    }
    @SubscribeEvent public static void kill(LivingDeathEvent event){if(event.getEntity() instanceof Enemy&&event.getSource().getEntity() instanceof ServerPlayer p&&DomainConfig.SURVIVAL_PROGRESSION.get()&&!p.getAbilities().instabuild){data(p).putInt("Xp",Math.min(100000,xp(p)+12));sync(p,false);}}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||p.tickCount%20!=0)return;
        var d=data(p);float energy=d.getFloat("Energy");d.putFloat("Energy",Math.min(ProgressionRules.maximum(xp(p)),Float.isFinite(energy)?Math.max(0,energy)+2:0));sync(p,false);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){
        for(String key:new String[]{"uv_progression","uv_arena"})if(event.getOriginal().getPersistentData().contains(key))event.getEntity().getPersistentData().put(key,event.getOriginal().getPersistentData().get(key).copy());
    }
    public static void request(ServerPlayer p,boolean open){long now=p.serverLevel().getGameTime();if(now-REQUESTS.getOrDefault(p.getUUID(),-100L)<10)return;REQUESTS.put(p.getUUID(),now);sync(p,open);}
    public static void sync(ServerPlayer p,boolean open){var d=data(p);float energy=d.getFloat("Energy");ProgressionNetwork.send(p,new ProgressionNetwork.State(DomainConfig.SURVIVAL_PROGRESSION.get(),p.getAbilities().instabuild,Math.max(0,Math.min(2,d.getInt("Path"))),xp(p),Math.max(0,Math.min(ProgressionRules.maximum(xp(p)),Float.isFinite(energy)?energy:0)),DomainService.cooldown(p),open));}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){REQUESTS.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppingEvent event){REQUESTS.clear();}
}
