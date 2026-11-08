package dev.unlimitedvoid.server;

import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.*;
import dev.unlimitedvoid.network.NpcControlNetwork;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID)
public final class NpcControlService {
    private static final Map<UUID,Long> LAST=new HashMap<>(),INFO=new HashMap<>();
    public static boolean supported(Entity e){return e instanceof JujutsuNpc||e instanceof MahoragaEntity||e instanceof InnocentSunDog;}
    public static boolean editor(ServerPlayer p){return p.getAbilities().instabuild||p.hasPermissions(2);}
    public static int mode(Entity e){return Math.max(0,Math.min(2,e.getPersistentData().getInt("uv_order")));}
    public static int team(Entity e){int own=Math.max(0,Math.min(2,e.getPersistentData().getInt("uv_team")));if(own==0&&e instanceof MahoragaEntity m&&m.owner()!=null)return team(m.owner());return own;}
    public static boolean canFight(Entity e){return mode(e)==0;}
    public static Boolean teamEnemy(Entity a,Entity b){int x=team(a),y=team(b);return x>0&&y>0?x!=y:null;}
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract e){
        if(e.getHand()!=InteractionHand.MAIN_HAND||!e.getEntity().isShiftKeyDown()||!supported(e.getTarget()))return;
        e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
        if(e.getEntity() instanceof ServerPlayer p)request(p,e.getTarget().getId(),9);
    }
    public static void request(ServerPlayer p,int id,int op){
        if(op<0||op>10||!p.isAlive())return;long now=p.serverLevel().getGameTime();
        var throttle=op==0?INFO:LAST;if(now-throttle.getOrDefault(p.getUUID(),-100L)<(op==0?10:4))return;throttle.put(p.getUUID(),now);
        if(op==8&&editor(p)){resetArena(p);return;}
        var e=p.serverLevel().getEntity(id);if(!(e instanceof Mob npc)||!supported(npc)||!npc.isAlive()||p.distanceToSqr(npc)>(op==0?24*24:8*8)||!p.hasLineOfSight(npc))return;
        if(op!=0&&!editor(p)){p.displayClientMessage(Component.literal("管理角色需要创造模式或管理员权限"),true);return;}
        if(op>=1&&op<=3){
            stop(npc);npc.setNoAi(false);if(op!=3&&npc.isLeashed())npc.dropLeash(true,false);npc.getPersistentData().putInt("uv_order",op==3?0:op);if(op==1)npc.getPersistentData().putUUID("uv_follow",p.getUUID());
        }else if(op>=4&&op<=6){stop(npc);npc.getPersistentData().putInt("uv_team",op==6?0:op-3);}
        else if(op==7)startArena(p,npc);
        else if(op==8)resetArena(p);
        else if(op==10){
            stop(npc);npc.getPersistentData().putInt("uv_order",0);
            if(npc instanceof JujutsuNpc n){var targets=p.serverLevel().getEntitiesOfClass(LivingEntity.class,n.getBoundingBox().inflate(24),t->NpcTechniqueService.enemy(n,t)&&n.hasLineOfSight(t));targets.sort(Comparator.comparingDouble(n::distanceToSqr));if(!targets.isEmpty())n.setTarget(targets.get(0));}
        }
        send(p,npc,op!=0);
    }
    public static void stop(Mob npc){
        npc.setTarget(null);npc.getNavigation().stop();
        if(npc instanceof JujutsuNpc n){n.cancelPunch();n.tacticUntil=0;NpcTechniqueService.cancel(n);DomainService.dismissNpc(n);FugaService.cancel(n.getUUID());}
    }
    private static void send(ServerPlayer p,Mob n,boolean open){
        String state="待命";
        if(n instanceof JujutsuNpc j)state=(j.staggered()?"硬直":j.getTarget()!=null?"交战":"警戒")+" · 领域"+(j.domainReady()?"就绪":"冷却")+(j.canReverse()?" · 反转术式":"");
        NpcControlNetwork.send(p,new NpcControlNetwork.State(n.getId(),n.getName().getString().substring(0,Math.min(128,n.getName().getString().length())),mode(n),team(n),p.getPersistentData().contains("uv_arena"),open,state));
    }
    public static void startArena(ServerPlayer p,Mob center){
        if(p.getPersistentData().contains("uv_arena")){p.displayClientMessage(Component.literal("请先重置当前对战"),true);return;}
        var actors=p.serverLevel().getEntitiesOfClass(Mob.class,center.getBoundingBox().inflate(32),e->supported(e)&&!(e instanceof InnocentSunDog)&&team(e)>0);
        if(actors.size()>24||actors.stream().noneMatch(e->team(e)==1)||actors.stream().noneMatch(e->team(e)==2)){p.displayClientMessage(Component.literal("32格内需要红蓝双方，总数不超过24个角色"),true);return;}
        if(actors.stream().anyMatch(a->a.getPersistentData().hasUUID("uv_arena_owner"))){p.displayClientMessage(Component.literal("这些角色已在另一场对战中"),true);return;}
        var session=new CompoundTag();session.putString("Dimension",p.level().dimension().location().toString());var tags=new ListTag();
        for(var actor:actors){var tag=new CompoundTag();actor.save(tag);tags.add(tag);}
        session.put("Actors",tags);p.getPersistentData().put("uv_arena",session);
        for(var actor:actors){stop(actor);actor.getPersistentData().putInt("uv_order",0);actor.getPersistentData().putUUID("uv_arena_owner",p.getUUID());actor.setNoAi(false);for(var slot:EquipmentSlot.values())actor.setDropChance(slot,0);}
        p.displayClientMessage(Component.literal("对战开始："+actors.size()+"个角色"),true);
    }
    public static void resetArena(ServerPlayer p){
        var session=p.getPersistentData().getCompound("uv_arena");if(!session.contains("Actors"))return;
        if(!session.getString("Dimension").equals(p.level().dimension().location().toString())){p.displayClientMessage(Component.literal("请回到开始对战的维度重置"),true);return;}
        for(var raw:session.getList("Actors",10)){
            var pos=((CompoundTag)raw).getList("Pos",6);
            if(pos.size()!=3||!p.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(pos.getDouble(0),pos.getDouble(1),pos.getDouble(2)))){p.displayClientMessage(Component.literal("请先靠近对战区域，等区块加载后再重置"),true);return;}
        }
        var originalIds=new java.util.HashSet<java.util.UUID>();for(var raw:session.getList("Actors",10))originalIds.add(((CompoundTag)raw).getUUID("UUID"));
        var helpers=new java.util.ArrayList<MahoragaEntity>();for(var entity:p.serverLevel().getAllEntities())if(entity instanceof MahoragaEntity m&&m.ownerId().isPresent()&&originalIds.contains(m.ownerId().get())&&!originalIds.contains(m.getUUID()))helpers.add(m);for(var m:helpers)m.discard();
        for(var raw:session.getList("Actors",10)){
            var tag=(CompoundTag)raw;if(!tag.hasUUID("UUID"))continue;var existing=p.serverLevel().getEntity(tag.getUUID("UUID"));
            if(existing instanceof Mob m){stop(m);m.discard();}
            var entity=EntityType.loadEntityRecursive(tag.copy(),p.serverLevel(),e->e);
            if(entity!=null&&p.serverLevel().hasChunkAt(entity.blockPosition()))p.serverLevel().addFreshEntity(entity);
        }
        p.getPersistentData().remove("uv_arena");p.displayClientMessage(Component.literal("对战角色已恢复到开始前"),true);NpcControlNetwork.send(p,new NpcControlNetwork.State(-1,"",0,0,false,true,""));
    }
    @SubscribeEvent public static void stopServer(net.minecraftforge.event.server.ServerStoppingEvent e){LAST.clear();INFO.clear();}
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){LAST.remove(e.getEntity().getUUID());INFO.remove(e.getEntity().getUUID());}
}
