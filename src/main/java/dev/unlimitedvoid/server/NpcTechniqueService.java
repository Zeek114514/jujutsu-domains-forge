package dev.unlimitedvoid.server;

import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.JujutsuNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** NPC casts share the player's visual protocol, timings and global eight-cast budget. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID)
public final class NpcTechniqueService {
    private static final Map<UUID,Cast> CASTS=new LinkedHashMap<>();
    public static int activeCount(){return CASTS.size();}
    public static boolean detonating(JujutsuNpc npc){return CASTS.values().stream().anyMatch(c->c.owner==npc&&c.phase>=PurpleDetonation.GROWING);}
    public static boolean charging(JujutsuNpc npc){return CASTS.values().stream().anyMatch(c->c.owner==npc&&c.phase==0);}
    public static boolean casting(JujutsuNpc npc){return CASTS.values().stream().anyMatch(c->c.owner==npc);}
    public static boolean enemy(JujutsuNpc npc,LivingEntity target){return target!=npc&&target.isAlive()&&!npc.isAlliedTo(target)&&(target instanceof Enemy||target instanceof dev.unlimitedvoid.entity.MahoragaEntity m&&npc.isGojo()&&!m.dismissing()||target instanceof JujutsuNpc other&&npc.isGojo()!=other.isGojo());}
    public static boolean trueSukuna(LivingEntity target){return target instanceof JujutsuNpc n&&n.isTrueSukuna();}
    public static void cancel(JujutsuNpc npc){CASTS.values().removeIf(c->c.owner==npc&&c.phase!=PurpleDetonation.BLAST);}
    public static boolean start(JujutsuNpc npc,LimitlessKind kind,LivingEntity target,boolean emergency){return start(npc,kind,target,emergency,false);}
    public static boolean instantPurple(JujutsuNpc npc,LivingEntity target){return start(npc,LimitlessKind.PURPLE,target,false,true);}
    private static boolean start(JujutsuNpc npc,LimitlessKind kind,LivingEntity target,boolean emergency,boolean instant){
        if(!npc.isAlive()||!npc.isGojo()||DomainService.isCaptive(npc.getUUID())||casting(npc)||CASTS.size()+LimitlessService.activeCount()>=8)return false;
        if(!emergency&&!instant&&(target==null||!enemy(npc,target)))return false;
        var c=new Cast(npc,kind,target);if(instant){c.phase=1;c.position=c.origin=npc.getEyePosition();}if(emergency){c.phase=PurpleDetonation.GROWING;c.position=npc.getEyePosition();}
        CASTS.put(c.id,c);return true;
    }
    public static List<LimitlessService.State> snapshot(ServerPlayer viewer){return CASTS.values().stream().filter(c->c.level==viewer.serverLevel()&&c.position.distanceToSqr(viewer.position())<256*256).map(c->new LimitlessService.State(c.id,c.owner.getUUID(),c.kind,c.position,c.direction,c.phase,c.charge,c.start,c.phaseStart,c.phase>=PurpleDetonation.GROWING?c.radius:0)).toList();}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.START)return;
        for(var c:List.copyOf(CASTS.values())){
            boolean live=c.owner.isAlive()&&!c.owner.isRemoved()&&c.level.getEntity(c.owner.getUUID())==c.owner;
            boolean deathBlast=c.phase==PurpleDetonation.BLAST&&c.owner.isDeadOrDying()&&(c.owner.getRemovalReason()==null||c.owner.getRemovalReason()==net.minecraft.world.entity.Entity.RemovalReason.KILLED);
            if((!live&&!deathBlast)||!c.level.hasChunkAt(BlockPos.containing(c.position))||DomainService.isCaptive(c.owner.getUUID())){CASTS.remove(c.id);continue;}
            long now=c.level.getGameTime();int age=(int)(now-c.phaseStart);
            if(c.phase==0){
                if(c.target==null||!enemy(c.owner,c.target)||c.target.level()!=c.level){CASTS.remove(c.id);continue;}
                c.direction=c.target.getBoundingBox().getCenter().subtract(c.owner.getEyePosition()).normalize();
                if(c.direction.lengthSqr()<.9)c.direction=c.owner.getLookAngle();
                c.position=c.owner.getEyePosition().add(c.direction.scale(1.1));
                if(now-c.start>=c.kind.charge){c.phase=1;c.phaseStart=now;c.position=c.origin=c.owner.getEyePosition();}
            }else if(c.phase==1){
                var from=c.position;var to=from.add(c.direction.scale(Math.min(c.kind.speed,c.kind.range-c.travel)));
                if(!c.level.hasChunkAt(BlockPos.containing(to))){CASTS.remove(c.id);continue;}
                var block=c.level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.owner));to=block.getLocation();boolean stop=block.getType()!=HitResult.Type.MISS;
                if(c.kind==LimitlessKind.PURPLE)purple(c,from,to);
                else{
                    var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(c.level,c.owner,from,to,new AABB(from,to).inflate(.5),e->e instanceof LivingEntity t&&enemy(c.owner,t));
                    if(hit!=null){to=hit.getLocation();stop=true;}
                }
                c.travel+=from.distanceTo(to);c.position=to;
                if(stop||c.travel>=c.kind.range-.001){c.phase=2;c.phaseStart=now;if(c.kind==LimitlessKind.BLUE)c.position=c.position.add(0,.6,0);}
            }else if(c.phase==2){
                if(c.kind==LimitlessKind.BLUE){field(c,age);if(age>=80)CASTS.remove(c.id);}
                else{if(!c.impacted){c.impacted=true;if(c.kind==LimitlessKind.RED)red(c);}if(age>=16)CASTS.remove(c.id);}
            }else if(c.phase==PurpleDetonation.GROWING){if(age>=PurpleDetonation.GROW_TICKS){c.phase=PurpleDetonation.BLAST;c.phaseStart=now;}}
            else if(c.phase==PurpleDetonation.BLAST){if(age<PurpleDetonation.HIT_TICKS)blast(c,age);if(age>=PurpleDetonation.BLAST_TICKS)CASTS.remove(c.id);}
        }
    }
    private static boolean exposed(Cast c,LivingEntity target){return !DomainService.fugaSheltered(c.level,c.origin,target.getBoundingBox().getCenter())&&c.level.clip(new ClipContext(c.position,target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.owner)).getType()==HitResult.Type.MISS;}
    private static List<LivingEntity> targets(Cast c,double radius){return c.level.getEntitiesOfClass(LivingEntity.class,new AABB(c.position,c.position).inflate(radius),t->enemy(c.owner,t));}
    private static DamageSource source(Cast c){var key=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation(UnlimitedVoid.ID,c.kind.name().toLowerCase(Locale.ROOT)));return new DamageSource(c.level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key),c.owner,c.owner);}
    private static void field(Cast c,int age){int scanned=0;for(var t:targets(c,8)){
        if(scanned++>=128)break;var delta=c.position.subtract(t.getBoundingBox().getCenter());double distance=delta.length();if(distance>8||!exposed(c,t))continue;
        if(distance>.05){t.setDeltaMovement(t.getDeltaMovement().scale(.65).add(delta.normalize().scale((.12+.28*(1-distance/8))*MahoragaService.force(t,dev.unlimitedvoid.MahoragaRules.Kind.BLUE))));t.hurtMarked=true;}
        if(age%10==0)t.hurt(source(c),4);
    }}
    private static void red(Cast c){int scanned=0;for(var t:targets(c,6)){
        if(scanned++>=128)break;double d=t.getBoundingBox().getCenter().distanceTo(c.position);if(d>6||!exposed(c,t))continue;
        t.hurt(source(c),(float)(72*(1-.45*d/6)));t.setDeltaMovement(t.position().subtract(c.position).normalize().scale(2).add(0,.5,0).scale(MahoragaService.force(t,dev.unlimitedvoid.MahoragaRules.Kind.RED)));t.hurtMarked=true;
    }}
    private static void purple(Cast c,Vec3 from,Vec3 to){int scanned=0;for(var t:c.level.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(4.5),e->enemy(c.owner,e)&&!c.struck.contains(e.getUUID()))){
        if(scanned++>=128||c.struck.size()>=512)break;
        if(PurpleDetonation.closest(from,to,t.getBoundingBox().getCenter()).distanceToSqr(t.getBoundingBox().getCenter())<=4.5*4.5&&exposed(c,t)){c.struck.add(t.getUUID());t.hurt(source(c),160);}
    }}
    private static void blast(Cast c,int age){double front=PurpleDetonation.front(age,c.radius);int scanned=0;
        var list=c.level.getEntitiesOfClass(LivingEntity.class,new AABB(c.position,c.position).inflate(front),t->(t==c.owner||enemy(c.owner,t))&&!c.struck.contains(t.getUUID()));
        int offset=list.isEmpty()?0:Math.floorMod(age*128,list.size());
        for(int i=0;i<list.size()&&scanned++<128&&c.struck.size()<512;i++){
            var target=list.get((offset+i)%list.size());double distance=target.getBoundingBox().getCenter().distanceTo(c.position);
            if(distance>front||!exposed(c,target))continue;c.struck.add(target.getUUID());float damage=PurpleDetonation.damage(distance,c.radius,c.damage);
            if(target==c.owner)damage*=c.backlash;if(damage>0)target.hurt(source(c),damage);
        }
    }
    @SubscribeEvent public static void stop(ServerStoppingEvent e){CASTS.clear();}
    private static final class Cast {
        final UUID id=UUID.randomUUID();final JujutsuNpc owner;final ServerLevel level;final LimitlessKind kind;final LivingEntity target;final long start;final int charge;final Set<UUID> struck=new HashSet<>();
        final float radius;final double damage,backlash;long phaseStart;Vec3 position,origin,direction;int phase;double travel;boolean impacted;
        Cast(JujutsuNpc owner,LimitlessKind kind,LivingEntity target){this.owner=owner;this.kind=kind;this.target=target;level=(ServerLevel)owner.level();start=phaseStart=level.getGameTime();charge=kind.charge;position=origin=owner.getEyePosition();direction=target==null?owner.getLookAngle():target.getBoundingBox().getCenter().subtract(position).normalize();if(direction.lengthSqr()<.9)direction=new Vec3(0,0,1);radius=DomainConfig.PURPLE_BLAST_RADIUS.get().floatValue();damage=DomainConfig.PURPLE_BLAST_DAMAGE.get();backlash=DomainConfig.PURPLE_BACKLASH.get();}
    }
}
