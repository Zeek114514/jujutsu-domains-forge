package dev.unlimitedvoid.server;

import dev.unlimitedvoid.FugaTimeline;
import dev.unlimitedvoid.UnlimitedVoid;
import dev.unlimitedvoid.network.DomainNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

/** Preview only: never hurts entities, ignites blocks or calls Level.explode. */
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
        DomainNetwork.fuga(cast.level,cast.owner,0,hand(player),player.getEyePosition().add(player.getLookAngle().scale(3)),cast.seed,1);
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
                DomainNetwork.fuga(c.level,c.owner,2,c.from,c.to,c.seed,1);iterator.remove();
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
                // A wall immediately in front must not put the visible arrow beyond that wall.
                if(eye.distanceTo(c.to)<1.6 || c.level.clip(new ClipContext(eye,c.from,
                        ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getType()!=net.minecraft.world.phys.HitResult.Type.MISS)c.from=eye;
                c.flight=FugaTimeline.flight(c.from.distanceTo(c.to));c.released=true;
                DomainNetwork.fuga(c.level,c.owner,1,c.from,c.to,c.seed,c.flight);
                c.level.playSound(null,BlockPos.containing(c.from),SoundEvents.FIRECHARGE_USE,SoundSource.PLAYERS,.8f,.65f);
            }
            if(c.released && !c.impacted && age>=FugaTimeline.CHARGE+c.flight) {
                c.impacted=true;
                c.level.playSound(null,BlockPos.containing(c.to),SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,24f,.5f);
            }
            if(c.released && age>=FugaTimeline.CHARGE+c.flight+FugaTimeline.AFTERMATH){iterator.remove();DomainNetwork.fugaStatus(player);}
        }
        READY.entrySet().removeIf(e->now>=e.getValue());
        LAST_RESET.entrySet().removeIf(e->now-e.getValue()>200);
        if(now%20==0)for(var p:server.getPlayerList().getPlayers())DomainNetwork.fugaStatus(p);
    }
    @SubscribeEvent public static void stop(ServerStoppingEvent event) { CASTS.clear();READY.clear();LAST_RESET.clear(); }
    private static final class Cast {
        final UUID owner;final ServerLevel level;final int start,seed;
        Vec3 from,to;int flight=1;boolean released,impacted;
        Cast(ServerPlayer p,int tick){owner=p.getUUID();level=p.serverLevel();start=tick;seed=p.getRandom().nextInt();from=hand(p);to=from;}
    }
}
