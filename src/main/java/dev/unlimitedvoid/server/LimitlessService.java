package dev.unlimitedvoid.server;

import dev.unlimitedvoid.*;
import dev.unlimitedvoid.network.LimitlessNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.*;

/** Bounded, server-owned casts. Requests never supply coordinates, power or victims. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID)
public final class LimitlessService {
    private static final Map<UUID,Cast> CASTS=new LinkedHashMap<>();
    private static final Map<UUID,int[]> READY=new HashMap<>();
    public record State(UUID id,UUID owner,LimitlessKind kind,Vec3 position,Vec3 direction,int phase,int charge,long start,long phaseStart){}
    public static boolean casting(UUID owner){return CASTS.values().stream().anyMatch(c->c.owner.equals(owner)&&c.phase==0);}
    public static int cooldown(ServerPlayer p,LimitlessKind kind){var ready=READY.get(p.getUUID());return ready==null?0:Math.max(0,ready[kind.ordinal()]-p.server.getTickCount());}
    public static void reset(ServerPlayer p){READY.remove(p.getUUID());sync(p);}
    private static boolean chanting(ServerPlayer p){return DomainService.snapshot(p).stream().anyMatch(d->d.owner().equals(p.getUUID())&&d.closing()<0&&d.age()<DomainTimeline.chant(d.kind()));}
    public static void request(ServerPlayer p,LimitlessKind kind){
        if(!p.isAlive()||p.isSpectator()||DomainService.isCaptive(p.getUUID())||chanting(p)||FugaService.casting(p.getUUID())||casting(p.getUUID())||cooldown(p,kind)>0||CASTS.size()>=8)return;
        var c=new Cast(p,kind);CASTS.put(c.id,c);
        READY.computeIfAbsent(p.getUUID(),id->new int[3])[kind.ordinal()]=p.server.getTickCount()+kind.cooldown;
        syncAll(p.server);
    }
    public static void releaseBlue(ServerPlayer p){for(var c:CASTS.values())if(c.owner.equals(p.getUUID())&&c.kind==LimitlessKind.BLUE)c.held=false;}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null)return;
        var iterator=CASTS.values().iterator();
        while(iterator.hasNext()){
            var c=iterator.next();var p=server.getPlayerList().getPlayer(c.owner);
            if(p==null||!p.isAlive()||p.isSpectator()||p.serverLevel()!=c.level||DomainService.isCaptive(c.owner)||c.phase==0&&chanting(p)){iterator.remove();continue;}
            long now=c.level.getGameTime();int age=(int)(now-c.start);
            if(c.phase==0){
                c.direction=p.getLookAngle();c.position=p.getEyePosition().add(c.direction.scale(3));
                int required=c.kind==LimitlessKind.BLUE&&c.held?40:c.kind.charge;
                if(age>=required){
                    c.charge=c.kind==LimitlessKind.BLUE?Math.min(40,age):c.kind.charge;c.phase=1;c.phaseStart=now;
                    // Start from the eye, then advance using loaded-only block and entity sweeps.
                    c.position=p.getEyePosition();c.origin=c.position;
                }
            }else if(c.phase==1){
                Vec3 from=c.position,to=from.add(c.direction.scale(Math.min(c.kind.speed,c.kind.range-c.travel)));
                boolean loaded=true;for(double t=0;t<=1;t+=.25)if(!c.level.hasChunkAt(BlockPos.containing(from.lerp(to,t)))){loaded=false;break;}
                if(!loaded){activate(c,now);continue;}
                var block=c.level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
                to=block.getLocation();boolean stop=block.getType()!=HitResult.Type.MISS;
                if(c.kind==LimitlessKind.BLUE||c.kind==LimitlessKind.RED){
                    var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(c.level,p,from,to,new AABB(from,to).inflate(.5),t->t instanceof LivingEntity l&&DomainService.fugaEligible(l,p));
                    if(hit!=null){to=hit.getLocation();stop=true;}
                }else{
                    sweep(c,p,from,to);
                    if(DomainConfig.LIMITLESS_TERRAIN.get()){
                        breakBlocks(c,p,to);
                        if(stop&&c.level.getBlockState(block.getBlockPos()).getCollisionShape(c.level,block.getBlockPos()).isEmpty())stop=false;
                    }
                }
                c.travel+=from.distanceTo(to);c.position=to;
                if(stop||c.travel>=c.kind.range-.001)activate(c,now);
            }else{
                int active=(int)(now-c.phaseStart);
                if(c.kind==LimitlessKind.BLUE){if(active<80)attract(c,p,active);else iterator.remove();}
                else{if(!c.impacted){c.impacted=true;if(c.kind==LimitlessKind.RED)repel(c,p);}if(active>16)iterator.remove();}
            }
        }
        int now=server.getTickCount();READY.values().removeIf(a->Arrays.stream(a).allMatch(t->t<=now));
        if(now%2==0)syncAll(server);
    }
    private static void activate(Cast c,long now){c.phase=2;c.phaseStart=now;if(c.kind==LimitlessKind.BLUE)c.position=c.position.add(0,1.2,0);}
    private static DamageSource source(Cast c,ServerPlayer p){var key=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation(UnlimitedVoid.ID,c.kind.name().toLowerCase(Locale.ROOT)));return new DamageSource(c.level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key),p,p);}
    private static boolean eligible(Cast c,ServerPlayer p,LivingEntity t){return DomainService.fugaEligible(t,p)&&!DomainService.fugaSheltered(c.level,c.origin,t.getBoundingBox().getCenter());}
    private static List<LivingEntity> targets(Cast c,ServerPlayer p,double radius){return c.level.getEntitiesOfClass(LivingEntity.class,new AABB(c.position,c.position).inflate(radius),t->eligible(c,p,t));}
    private static void attract(Cast c,ServerPlayer p,int age){
        double radius=c.kind.radius(c.charge);int scanned=0;
        for(var t:targets(c,p,radius)){
            if(scanned++>=128)break;Vec3 delta=c.position.subtract(t.getBoundingBox().getCenter());double distance=delta.length();if(distance>radius)continue;
            if(delta.lengthSqr()>.01){Vec3 velocity=t.getDeltaMovement().scale(.65).add(delta.normalize().scale(.12+.28*(1-distance/radius)));t.setDeltaMovement(velocity);t.hurtMarked=true;}
            if(age%10==0)t.hurt(source(c,p),(float)(c.kind.damage+2*LimitlessKind.power(c.charge)));
        }
    }
    private static void repel(Cast c,ServerPlayer p){int scanned=0;
        for(var t:targets(c,p,c.kind.radius)){
            if(scanned++>=128)break;Vec3 delta=t.getBoundingBox().getCenter().subtract(c.position);double distance=delta.length();if(distance>c.kind.radius)continue;
            t.hurt(source(c,p),(float)(c.kind.damage*(1-.45*distance/c.kind.radius)));
            Vec3 away=distance<.05?c.direction:delta.normalize();away=away.add(c.direction.scale(.8)).normalize();t.setDeltaMovement(away.scale(2.4).add(0,.65,0));t.hurtMarked=true;
        }
    }
    private static void sweep(Cast c,ServerPlayer p,Vec3 from,Vec3 to){
        int scanned=0;for(var t:c.level.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(c.kind.radius),t->eligible(c,p,t)&&!c.struck.contains(t.getUUID()))){
            if(scanned++>=128||c.struck.size()>=512)break;
            Vec3 point=t.getBoundingBox().getCenter(),line=to.subtract(from);double q=line.lengthSqr()<.001?0:Math.max(0,Math.min(1,point.subtract(from).dot(line)/line.lengthSqr()));
            if(point.distanceToSqr(from.add(line.scale(q)))<=c.kind.radius*c.kind.radius){c.struck.add(t.getUUID());t.hurt(source(c,p),(float)c.kind.damage);}
        }
    }
    private static void breakBlocks(Cast c,ServerPlayer p,Vec3 center){
        int checked=0,broken=0;var base=BlockPos.containing(center);
        var candidates=new ArrayList<BlockPos>();for(var pos:BlockPos.betweenClosed(base.offset(-2,-2,-2),base.offset(2,2,2)))candidates.add(pos.immutable());
        candidates.sort(Comparator.comparingDouble(pos->pos.distToCenterSqr(center)));
        for(var pos:candidates){
            if(checked++>=125||broken>=16)break;if(!c.level.hasChunkAt(pos))continue;var state=c.level.getBlockState(pos);
            if(state.isAir()||!state.getFluidState().isEmpty()||state.hasBlockEntity()||state.getDestroySpeed(c.level,pos)<0)continue;
            if(!p.mayBuild()||!c.level.mayInteract(p,pos))continue;
            // A spell is independent of the held item (creative swords cannot mine).
            if(net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.BlockEvent.BreakEvent(c.level,pos,state,p)))continue;
            if(c.level.destroyBlock(pos,false,p))broken++;
        }
    }
    public static List<State> snapshot(ServerPlayer p){return CASTS.values().stream().filter(c->c.level==p.serverLevel()&&c.position.distanceToSqr(p.position())<256*256).map(c->new State(c.id,c.owner,c.kind,c.position,c.direction,c.phase,c.charge,c.start,c.phaseStart)).toList();}
    private static void sync(ServerPlayer p){LimitlessNetwork.sync(p,snapshot(p),cooldown(p,LimitlessKind.BLUE),cooldown(p,LimitlessKind.RED),cooldown(p,LimitlessKind.PURPLE));}
    private static void syncAll(net.minecraft.server.MinecraftServer server){for(var p:server.getPlayerList().getPlayers())sync(p);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){UUID id=e.getEntity().getUUID();READY.remove(id);CASTS.values().removeIf(c->c.owner.equals(id));}
    @SubscribeEvent public static void stop(ServerStoppingEvent e){CASTS.clear();READY.clear();}
    private static final class Cast {
        final UUID id=UUID.randomUUID(),owner;final ServerLevel level;final LimitlessKind kind;final long start;final Set<UUID> struck=new HashSet<>();
        long phaseStart;Vec3 position,direction,origin;int phase,charge;double travel;boolean held=true,impacted;
        Cast(ServerPlayer p,LimitlessKind kind){owner=p.getUUID();level=p.serverLevel();this.kind=kind;start=phaseStart=level.getGameTime();position=origin=p.getEyePosition();direction=p.getLookAngle();charge=kind.charge;}
    }
}
