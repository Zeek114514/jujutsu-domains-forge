package dev.unlimitedvoid.server;

import dev.unlimitedvoid.FugaTimeline;
import dev.unlimitedvoid.UnlimitedVoid;
import dev.unlimitedvoid.network.DomainNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Authoritative cast, bounded damage and observer synchronization; never loads terrain. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID)
public final class FugaService {
    private static final Map<UUID,Cast> CASTS=new HashMap<>();
    private static final Map<UUID,Integer> READY=new HashMap<>();
    private static final Map<UUID,Integer> LAST_RESET=new HashMap<>();
    private FugaService() {}
    public static int cooldown(ServerPlayer p) { return Math.max(0,READY.getOrDefault(p.getUUID(),0)-p.server.getTickCount()); }
    public static boolean casting(UUID id) { return CASTS.containsKey(id); }
    public static void resetCooldown(ServerPlayer p) {
        if(!p.isAlive()||p.isSpectator()||DomainService.isCaptive(p.getUUID()))return;
        int now=p.server.getTickCount();
        if(now-LAST_RESET.getOrDefault(p.getUUID(),-100)<4)return;
        LAST_RESET.put(p.getUUID(),now);READY.remove(p.getUUID());DomainNetwork.fugaStatus(p);
    }
    public static void request(ServerPlayer player) {
        if(!player.isAlive() || player.isSpectator() || DomainService.isCaptive(player.getUUID())) return;
        int now=player.server.getTickCount();
        if(CASTS.containsKey(player.getUUID()) || now<READY.getOrDefault(player.getUUID(),0)) return;
        if(CASTS.size()>=FugaTimeline.MAX_CASTS) return;
        Cast cast=new Cast(player,now);
        CASTS.put(player.getUUID(),cast); READY.put(player.getUUID(),now+FugaTimeline.COOLDOWN);
        DomainNetwork.fugaStatus(player);
        syncObservers(cast);
    }
    private static Vec3 hand(ServerPlayer p) {
        Vec3 direction=p.getLookAngle();
        Vec3 right=direction.cross(new Vec3(0,1,0));
        if(right.lengthSqr()<.001)right=new Vec3(1,0,0);else right=right.normalize();
        return p.getEyePosition().add(direction.scale(1.2)).add(right.scale(.25)).add(0,-.22,0);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        var server=ServerLifecycleHooks.getCurrentServer(); if(server==null)return;
        int now=server.getTickCount();
        Iterator<Cast> iterator=CASTS.values().iterator();
        while(iterator.hasNext()) {
            Cast c=iterator.next();var player=server.getPlayerList().getPlayer(c.owner);
            if(player==null || !player.isAlive() || player.isSpectator() || player.serverLevel()!=c.level || DomainService.isCaptive(c.owner)) {
                for(var listener:c.level.players())if(c.listeners.contains(listener.getUUID()))send(c,listener,2);
                iterator.remove();
                if(player!=null)DomainNetwork.fugaStatus(player);continue;
            }
            int age=now-c.start;
            if(!c.released && age>=FugaTimeline.CHARGE) {
                c.from=hand(player); Vec3 eye=player.getEyePosition(),direction=player.getLookAngle();
                double loadedRange=0;
                // Do not generate or synchronously load chunks merely for a visual projectile.
                for(double d=.5;d<=FugaTimeline.RANGE;d+=.5) {
                    if(!c.level.hasChunkAt(BlockPos.containing(eye.add(direction.scale(d)))))break;
                    loadedRange=d;
                }
                Vec3 end=eye.add(direction.scale(loadedRange));
                c.to=c.level.clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getLocation();
                var entityHit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(c.level,player,eye,c.to,
                    player.getBoundingBox().expandTowards(c.to.subtract(eye)).inflate(1),t->t instanceof net.minecraft.world.entity.LivingEntity living&&DomainService.fugaEligible(living,player));
                if(entityHit!=null)c.to=entityHit.getLocation();
                // A wall immediately in front must not put the visible arrow beyond that wall.
                if(eye.distanceTo(c.to)<1.6 || c.level.clip(new ClipContext(eye,c.from,
                        ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getType()!=net.minecraft.world.phys.HitResult.Type.MISS)c.from=eye;
                c.flight=FugaTimeline.flight(c.from.distanceTo(c.to));c.released=true;
                for(var listener:c.level.players())if(c.listeners.contains(listener.getUUID()))send(c,listener,1);
            }
            if(c.released&&age>=FugaTimeline.CHARGE+c.flight){
                float burst=age-FugaTimeline.CHARGE-c.flight;
                if(!c.impacted){
                    c.impacted=true;
                    if(dev.unlimitedvoid.DomainConfig.FUGA_COMBAT.get())c.dust=DomainService.consumeFugaDust(player,c.to);
                    if(c.dust!=null){for(var listener:c.level.players())if(c.listeners.contains(listener.getUUID()))send(c,listener,3);DomainNetwork.fugaStatus(player);}
                }
                if(((int)burst)%4==0)FugaCombat.pulse(player,c.from,c.to,burst,c.dust,c.struck);
            }
            if(now%10==0)syncObservers(c);
            if(c.released && age>=FugaTimeline.CHARGE+c.flight+FugaTimeline.AFTERMATH){iterator.remove();DomainNetwork.fugaStatus(player);}
        }
        READY.entrySet().removeIf(e->now>=e.getValue());
        LAST_RESET.entrySet().removeIf(e->now-e.getValue()>200);
        if(now%20==0)for(var p:server.getPlayerList().getPlayers())DomainNetwork.fugaStatus(p);
    }
    @SubscribeEvent public static void stop(ServerStoppingEvent event) { CASTS.clear();READY.clear();LAST_RESET.clear(); }
    private static void send(Cast c,ServerPlayer p,int phase){DomainNetwork.fugaTo(p,c.owner,phase,c.from,c.to,c.seed,c.flight,c.startGameTime);}
    private static void syncObservers(Cast c){
        c.listeners.removeIf(id->c.level.getPlayerByUUID(id)==null);
        double range=FugaTimeline.VIEW_RANGE*FugaTimeline.VIEW_RANGE;
        for(var p:c.level.players())if(p.getUUID().equals(c.owner)||p.distanceToSqr(c.from)<range||p.distanceToSqr(c.to)<range){
            if(c.listeners.add(p.getUUID()))send(c,p,c.dust!=null?3:c.released?1:0);
        }
    }
    private static final class Cast {
        final UUID owner;final ServerLevel level;final int start,seed;
        final long startGameTime;final java.util.Set<UUID> listeners=new java.util.HashSet<>(),struck=new java.util.HashSet<>();
        Vec3 from,to;int flight=1;boolean released,impacted;DomainService.FugaDust dust;
        Cast(ServerPlayer p,int tick){owner=p.getUUID();level=p.serverLevel();start=tick;startGameTime=level.getGameTime();seed=p.getRandom().nextInt();from=hand(p);to=from;}
    }
}
