package dev.unlimitedvoid.server;

import dev.unlimitedvoid.UnlimitedVoid;
import dev.unlimitedvoid.DomainKind;
import dev.unlimitedvoid.DomainConfig;
import dev.unlimitedvoid.DomainTimeline;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import org.joml.Vector3f;
import dev.unlimitedvoid.network.DomainNetwork;
import dev.unlimitedvoid.network.DomainNetwork.DomainState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import dev.unlimitedvoid.DomainSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/** All gameplay runs on the logical server; no world blocks or persistent AI flags are changed. */
@Mod.EventBusSubscriber(modid = UnlimitedVoid.ID)
public final class DomainService {
    public static final float RADIUS = 16;
    public static final int DURATION = 400;
    public static final int COOLDOWN = 800;
    public static final UUID STASIS_MOVEMENT = UUID.fromString("8db038a5-09e6-4b1a-8be6-51d89f39974f");
    private static final int MAX_DOMAINS = 8;
    private static final int MAX_CAPTIVES = 256;
    private static final Map<UUID, Float> BARRIER_HP = new HashMap<>();
    private static final Map<UUID, Integer> LAST_RESET = new HashMap<>();
    private static final Map<UUID, DomainKind> SELECTED = new HashMap<>();
    private static final Map<UUID, Integer> LAST_CYCLE = new HashMap<>();
    private static final Map<UUID, Domain> DOMAINS = new LinkedHashMap<>();
    private static final Map<UUID, Captive> CAPTIVES = new HashMap<>();
    private static final Map<UUID, Integer> READY_AT = new HashMap<>();
    private static final Map<UUID, Integer> LAST_REQUEST = new HashMap<>();

    private DomainService() {}

    public static int cooldown(ServerPlayer player) {
        return Math.max(0,READY_AT.getOrDefault(player.getUUID(),0)-player.getServer().getTickCount());
    }
    public static void resetBurnout(ServerPlayer player) {
        if(!player.isAlive() || player.isSpectator() || isCaptive(player.getUUID())) return;
        int now=player.getServer().getTickCount();
        Integer prior=LAST_RESET.put(player.getUUID(),now);
        if(prior!=null && now-prior<4) return;
        READY_AT.remove(player.getUUID());
        LimitlessService.reset(player);
        player.displayClientMessage(Component.translatable("message.unlimitedvoid.reset"),true);
        DomainNetwork.sync(player);
    }
    public static float barrierIntegrity(UUID owner) { return BARRIER_HP.getOrDefault(owner,100f)/100f; }
    private static void dismiss(UUID owner) {
        Domain domain=DOMAINS.get(owner);
        if(domain==null || domain.closingAt>=0)return;
        domain.closingAt=domain.level.getServer().getTickCount();
        releaseOwned(owner);
        domain.level.playSound(null,BlockPos.containing(domain.center),DomainSounds.COLLAPSE.get(),SoundSource.PLAYERS,0.65f,1);
    }
    private static void remove(UUID owner) {
        DOMAINS.remove(owner);BARRIER_HP.remove(owner);releaseOwned(owner);ClosedBarrier.remove(owner);ShrineStructures.remove(owner);
    }
    /** Operator-owned temporary domains for in-game demonstrations; no fake players or saved mobs. */
    public static int generate(ServerPlayer operator,DomainKind kind,Vec3 center,Vec3 axis) {
        if(DOMAINS.size()>=MAX_DOMAINS || !operator.serverLevel().hasChunkAt(BlockPos.containing(center)))return 0;
        if(kind==DomainKind.SHRINE){
            var grounded=ShrinePlacement.center(operator.serverLevel(),center);
            if(grounded.isEmpty())return 0;
            center=grounded.get();
        }
        UUID id=UUID.randomUUID();int now=operator.getServer().getTickCount();
        DOMAINS.put(id,new Domain(id,operator.getUUID(),true,operator.serverLevel(),center,axis,now,kind,kind==DomainKind.SHRINE?DomainConfig.SHRINE_RADIUS.get():16));
        DomainNetwork.announceExpansion(operator.serverLevel(),id,kind,center,kind==DomainKind.SHRINE?DomainConfig.SHRINE_RADIUS.get():16);
        syncAll(operator.getServer());return 1;
    }
    public static int generateClash(ServerPlayer operator) {
        if(DOMAINS.size()>MAX_DOMAINS-2)return 0;
        Vec3 forward=new Vec3(operator.getLookAngle().x,0,operator.getLookAngle().z).normalize();
        if(forward.lengthSqr()<0.1)forward=new Vec3(0,0,1);
        Vec3 side=new Vec3(forward.z,0,-forward.x),middle=operator.position().add(0,1,0);
        int result=generate(operator,DomainKind.VOID,middle.subtract(side.scale(6)),forward);
        result+=generate(operator,DomainKind.SHRINE,middle.add(side.scale(6)),forward);
        return result;
    }
    public static int clearGenerated(ServerPlayer operator) {
        int count=0;for(Domain d:List.copyOf(DOMAINS.values()))if(d.generated && d.level==operator.serverLevel() && d.casterId.equals(operator.getUUID())){dismiss(d.owner);count++;}
        syncAll(operator.getServer());return count;
    }
    private static boolean contested(Domain domain,Vec3 point,int now) {
        for(Domain other:DOMAINS.values())if(other!=domain && other.level==domain.level && other.kind!=domain.kind && (other.active(now)||(other.closingAt<0&&(domain.npc||other.npc)))
            && point.distanceToSqr(other.center)<Math.pow((domain.npc||other.npc)?other.maxRadius:other.radius(now),2))return true;
        return false;
    }
    public static boolean terrainCutAllowed(ServerLevel level,Vec3 point) {
        int now=level.getServer().getTickCount();
        for(Domain d:DOMAINS.values())if(d.level==level && d.kind==DomainKind.VOID && d.active(now)
            && point.distanceToSqr(d.center)<d.radius(now)*d.radius(now))return false;
        return true;
    }
    /** External shell hit; the range test uses the sphere surface, not its center. */
    public static boolean strikeBarrier(ServerLevel level,UUID owner,Vec3 origin,double range,double power) {
        Domain barrier=DOMAINS.get(owner);
        if(barrier==null || barrier.kind!=DomainKind.VOID || barrier.level!=level || !barrier.active(level.getServer().getTickCount())) return false;
        double distance=barrier.center.distanceTo(origin);
        if(distance<=barrier.maxRadius || distance-barrier.maxRadius>range) return false;
        float hp=BARRIER_HP.getOrDefault(owner,100f);
        hp-=12.5f*(float)Math.max(0.25,Math.min(3,power/100));
        BARRIER_HP.put(owner,Math.max(0,hp));
        ServerPlayer caster=level.getServer().getPlayerList().getPlayer(owner);
        if(caster!=null && hp<=87.5f && hp>75) caster.displayClientMessage(Component.translatable("message.unlimitedvoid.shell_hit"),true);
        if(hp<=0) {
            dismiss(owner);
            level.playSound(null,BlockPos.containing(barrier.center),DomainSounds.SHELL_BREAK.get(),SoundSource.PLAYERS,0.9f,1.0f);
            level.sendParticles(ParticleTypes.POOF,barrier.center.x,barrier.center.y,barrier.center.z,50,8,5,8,0.2);
            if(caster!=null) caster.displayClientMessage(Component.translatable("message.unlimitedvoid.shell_broken"),true);
        }
        return true;
    }
    private static boolean strikeOtherBarriers(MinecraftServer server,int now) {
        boolean changed=false;
        for(Domain shrine:List.copyOf(DOMAINS.values())) {
            if(shrine.kind!=DomainKind.SHRINE || !shrine.active(now) || (now-shrine.startedAt)%10!=0) continue;
            LivingEntity attacker=caster(shrine);
            if(attacker==null) continue;
            for(Domain barrier:List.copyOf(DOMAINS.values())) {
                if(barrier.kind!=DomainKind.VOID || barrier.owner.equals(shrine.owner)) continue;
                changed|=strikeBarrier(shrine.level,barrier.owner,shrine.center,shrine.radius(now),ShrineRules.power(attacker));
            }
        }
        return changed;
    }
    private static boolean sheltered(Entity target,Domain shrine) {
        for(Domain barrier:DOMAINS.values()) {
            if(barrier.kind==DomainKind.VOID && barrier.active(barrier.level.getServer().getTickCount()) && barrier.level==shrine.level
                && barrier.center.distanceTo(shrine.center)>barrier.maxRadius
                && target.getBoundingBox().getCenter().distanceToSqr(barrier.center)<barrier.maxRadius*barrier.maxRadius) return true;
        }
        return false;
    }

    public static DomainKind selection(ServerPlayer player) { return SELECTED.getOrDefault(player.getUUID(), DomainKind.VOID); }
    public static void cycle(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || isCaptive(player.getUUID())) return;
        int now=player.getServer().getTickCount();
        Integer prior=LAST_CYCLE.put(player.getUUID(),now);
        if (prior!=null && now-prior<4) return;
        if (DOMAINS.containsKey(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.unlimitedvoid.switch_active"),true); return;
        }
        DomainKind next=selection(player)==DomainKind.VOID ? DomainKind.SHRINE : DomainKind.VOID;
        SELECTED.put(player.getUUID(),next);
        player.displayClientMessage(Component.translatable("message.unlimitedvoid.selected",Component.translatable(next.translation)),true);
        DomainNetwork.sync(player);
    }

    /** Empty activation packets cannot provide positions, damage, radius, or duration. */
    public static void toggle(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || !player.isAlive() || player.isSpectator()) return;
        int now = server.getTickCount();
        UUID id = player.getUUID();
        Integer previous = LAST_REQUEST.put(id, now);
        if (previous != null && now - previous < 4) return;
        if (isCaptive(id)) {
            player.displayClientMessage(Component.translatable("message.unlimitedvoid.overload"), true);
            return;
        }
        if (DOMAINS.containsKey(id)) {
            dismiss(id);
            syncAll(server);
            return;
        }
        int wait = READY_AT.getOrDefault(id, 0) - now;
        if (wait > 0) {
            player.displayClientMessage(Component.translatable("message.unlimitedvoid.cooldown", (wait + 19) / 20), true);
            return;
        }
        if (DOMAINS.size() >= MAX_DOMAINS) {
            player.displayClientMessage(Component.translatable("message.unlimitedvoid.limit"), true);
            return;
        }
        Vec3 axis = player.getLookAngle().add(0, 0.12, 0).normalize();
        DomainKind kind=selection(player);
        Vec3 center=player.position().add(0,1,0);
        if(kind==DomainKind.SHRINE){
            var grounded=ShrinePlacement.center(player.serverLevel(),center);
            if(grounded.isEmpty()){
                player.displayClientMessage(Component.translatable("message.unlimitedvoid.no_ground"),true);
                return;
            }
            center=grounded.get();
        }
        DOMAINS.put(id, new Domain(id, id, false, player.serverLevel(), center, axis, now, kind, kind==DomainKind.SHRINE ? DomainConfig.SHRINE_RADIUS.get() : 16));
        READY_AT.put(id, now + COOLDOWN);
        DomainNetwork.announceExpansion(player,kind,center,kind==DomainKind.SHRINE?DomainConfig.SHRINE_RADIUS.get():16);
        NpcDomainService.respond(player.serverLevel(),kind,center,kind==DomainKind.SHRINE?DomainConfig.SHRINE_RADIUS.get():16);
        player.displayClientMessage(Component.translatable("message.unlimitedvoid.expand_kind", Component.translatable(kind.translation)), true);
        syncAll(server);
    }

    private static LivingEntity caster(Domain d){
        if(d.npc){var e=d.level.getEntity(d.casterId);return e instanceof dev.unlimitedvoid.entity.JujutsuNpc n&&n.isAlive()&&!n.isNoAi()?n:null;}
        return d.level.getServer().getPlayerList().getPlayer(d.casterId);
    }
    public static boolean npcFamilyDomain(ServerLevel level,DomainKind kind){return DOMAINS.values().stream().anyMatch(d->d.npc&&d.level==level&&d.kind==kind&&d.closingAt<0);}
    public static boolean npcChanting(dev.unlimitedvoid.entity.JujutsuNpc npc){var d=DOMAINS.get(npc.getUUID());return d!=null&&d.closingAt<0&&npc.level().getServer().getTickCount()-d.startedAt<=DomainTimeline.casterRelease(d.kind);}
    public static void dismissNpc(dev.unlimitedvoid.entity.JujutsuNpc npc){var d=DOMAINS.get(npc.getUUID());if(d!=null&&d.npc)dismiss(d.owner);}
    /** Reserve the family slot before broadcasting; recursive counter-expansions see this reservation. */
    public static boolean expandNpc(dev.unlimitedvoid.entity.JujutsuNpc npc,boolean counter){
        if(!(npc.level() instanceof ServerLevel level)||!npc.isAlive()||npc.isNoAi()||isCaptive(npc.getUUID())||(!counter&&!npc.domainReady())||DOMAINS.containsKey(npc.getUUID())||DOMAINS.size()>=MAX_DOMAINS)return false;
        DomainKind kind=npc.isGojo()?DomainKind.VOID:DomainKind.SHRINE;
        if(npcFamilyDomain(level,kind))return false;
        Vec3 center=npc.position().add(0,1,0),axis=npc.getLookAngle().add(0,.12,0).normalize();
        if(kind==DomainKind.SHRINE){var ground=ShrinePlacement.center(level,center);if(ground.isEmpty())return false;center=ground.get();}
        int now=level.getServer().getTickCount();float radius=kind==DomainKind.SHRINE?DomainConfig.SHRINE_RADIUS.get():16;
        DOMAINS.put(npc.getUUID(),new Domain(npc.getUUID(),npc.getUUID(),false,level,center,axis,now,kind,radius));npc.domainUsed();npc.cancelPunch();NpcTechniqueService.cancel(npc);FugaService.cancel(npc.getUUID());npc.getNavigation().stop();npc.setDeltaMovement(Vec3.ZERO);
        DomainNetwork.announceExpansion(level,npc.getUUID(),kind,center,radius);
        dev.unlimitedvoid.server.NpcDomainService.respond(level,kind,center,radius);
        syncAll(level.getServer());return true;
    }

    public static boolean isCaptive(UUID id) { return CAPTIVES.containsKey(id); }
    public static int activeCount() { return DOMAINS.size(); }
    public static int captiveCount() { return CAPTIVES.size(); }

    /** The packet contains only nearby domains in this player's current dimension. */
    public static List<DomainState> snapshot(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return List.of();
        int now = server.getTickCount();
        return DOMAINS.values().stream()
                .filter(d -> d.level == player.serverLevel() && d.center.distanceToSqr(player.position()) < (d.maxRadius+160) * (d.maxRadius+160))
                .map(d -> new DomainState(d.owner, d.center, d.axis, d.radius(now),
                        Math.max(0, now - d.startedAt), Math.max(0, d.total() - (now - d.startedAt)), d.kind, d.maxRadius, barrierIntegrity(d.owner),d.closingAt<0?-1:now-d.closingAt))
                .toList();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        int now = server.getTickCount();
        boolean removed=false;
        for(Domain d:List.copyOf(DOMAINS.values())) {
            LivingEntity caster=caster(d);
            int age=now-d.startedAt;
            if(d.closingAt<0 && (caster==null || !caster.isAlive() || caster.isSpectator() || caster.level()!=d.level || age>=d.total())) {dismiss(d.owner);removed=true;}
            if(d.closingAt>=0) {
                if(now-d.closingAt>=DomainTimeline.FADE){remove(d.owner);removed=true;}
                else if(d.kind==DomainKind.SHRINE)ShrineStructures.animate(d.owner,age,now-d.closingAt);
                continue;
            }
            if(d.kind==DomainKind.VOID && !d.sealAdded && age>=DomainTimeline.chant(d.kind)+45){
                ClosedBarrier.add(d.owner,d.level,d.center);BARRIER_HP.put(d.owner,100f);d.sealAdded=true;
            }
            if(d.kind==DomainKind.SHRINE && age>=DomainTimeline.formationStart(d.kind)) {
                if(ShrineStructures.get(d.owner)==null)ShrineStructures.spawn(d.owner,d.level,d.center,d.axis);
                ShrineStructures.animate(d.owner,age,-1);
                if(!d.generated && caster!=null && age<=DomainTimeline.casterRelease(d.kind)){
                    double y=ShrineGeometryY(d.center,age);
                    // An airborne caster is only lifted after the rising roof reaches them.
                    // Never snap a high-flying caster down to the ground at the start.
                    if(d.lifting || y+0.12>=caster.getY()){
                        d.lifting=true;
                        caster.teleportTo(d.center.x,y,d.center.z);
                        caster.setDeltaMovement(Vec3.ZERO);caster.fallDistance=0;
                    }
                }
            }
        }
        removed|=strikeOtherBarriers(server,now);
        BARRIER_HP.keySet().retainAll(DOMAINS.keySet());
        ShrineStructures.retain(DOMAINS.keySet());
        ClosedBarrier.retain(DOMAINS.keySet());
        // Release stale references before an entity can be captured by another active domain.
        for (UUID id : List.copyOf(CAPTIVES.keySet())) {
            Captive captive = CAPTIVES.get(id);
            Domain domain = DOMAINS.get(captive.owner);
            if (captive.entity instanceof dev.unlimitedvoid.entity.MahoragaEntity m&&m.adaptation(dev.unlimitedvoid.MahoragaRules.Kind.VOID)>=4 || domain == null || !domain.active(now) || contested(domain,captive.entity.getBoundingBox().getCenter(),now) || captive.entity.isRemoved() || !captive.entity.isAlive()
                    || captive.entity.level() != domain.level) release(id);
        }
        for (Domain domain : DOMAINS.values()) {
            LivingEntity caster = caster(domain);
            if (caster == null || !domain.active(now)) continue;
            if (domain.kind==DomainKind.SHRINE) { slash(domain,caster,now); continue; }
            double radius = domain.radius(now);
            if (radius < 1.5) continue;
            double reach = radius + 0.6;
            AABB box = new AABB(domain.center.subtract(reach, reach, reach), domain.center.add(reach, reach, reach));
            for (LivingEntity target : domain.level.getEntitiesOfClass(LivingEntity.class, box,
                    candidate -> eligible(candidate, caster))) {
                Vec3 middle = target.getBoundingBox().getCenter();
                if (middle.distanceToSqr(domain.center) > reach * reach || contested(domain,middle,now)) continue;
                if(target instanceof dev.unlimitedvoid.entity.MahoragaEntity m&&m.adaptation(dev.unlimitedvoid.MahoragaRules.Kind.VOID)>=4)continue;
                UUID id = target.getUUID();
                if (CAPTIVES.containsKey(id) || CAPTIVES.size() >= MAX_CAPTIVES) continue;
                Vec3 anchor = safeEntry(domain, target, radius);
                if (anchor == null) continue;
                target.stopRiding();
                CAPTIVES.put(id, new Captive(domain.owner, target, anchor, now));
                applyStasis(target);
                if (target instanceof ServerPlayer victim) {
                    victim.displayClientMessage(Component.translatable("message.unlimitedvoid.overload"), true);
                }
            }
        }
        // Existing prisoners are processed independently of the discovery sphere. A large
        // knockback or command displacement cannot release one by moving it past the query.
        for (UUID id : List.copyOf(CAPTIVES.keySet())) {
            Captive captive = CAPTIVES.get(id);
            Domain domain = DOMAINS.get(captive.owner);
            LivingEntity caster = domain == null ? null : caster(domain);
            if (caster == null || !eligible(captive.entity, caster) || captive.entity.level() != domain.level) {
                release(id);
                continue;
            }
            pin(captive);
            if (now - captive.capturedAt >= 20 && (now - captive.capturedAt) % 20 == 0) {
                captive.entity.hurt(captive.entity.damageSources().indirectMagic(caster, caster), 2.0f);
                // Damage knockback may have run synchronously inside hurt().
                if (captive.entity.isAlive()) pin(captive);
                else release(id);
            }
        }
        ClosedBarrier.tick();
        if (removed || now % 5 == 0) syncAll(server);
        if (now % 1200 == 0) {
            READY_AT.entrySet().removeIf(entry -> entry.getValue() <= now);
            LAST_REQUEST.entrySet().removeIf(entry -> now - entry.getValue() > COOLDOWN);
        }
    }

    private static void slash(Domain domain, LivingEntity caster, int now) {
        int age=now-domain.startedAt;
        if(!domain.active(now)) return;
        double radius=domain.radius(now);
        if(caster instanceof ServerPlayer player)ShrineRules.cutTerrain(domain.level,player,domain.center,radius,age,point->terrainCutAllowed(domain.level,point));
        if(age%10!=0) return;
        // A visual dust field accumulates even with permanent terrain destruction disabled.
        domain.dust=Math.min(dev.unlimitedvoid.FugaCombatRules.DUST_REQUIRED,domain.dust+10);
        AABB box=new AABB(domain.center.subtract(radius,radius,radius),domain.center.add(radius,radius,radius));
        int hit=0,audibleHits=0;
        for(Entity target:domain.level.getEntities((Entity)null,box,t->t!=caster && t.isAlive())) {
            if(target.getBoundingBox().getCenter().distanceToSqr(domain.center)>radius*radius || sheltered(target,domain) || contested(domain,target.getBoundingBox().getCenter(),now) || hit>=MAX_CAPTIVES) continue;
            if(target instanceof LivingEntity living && !(target instanceof ArmorStand)) {
                if(!eligible(living,caster)) continue;
                boolean cleave=ShrineRules.energy(target)>0;
                living.hurt(ShrineRules.source(caster,cleave),cleave ? ShrineRules.cleaveDamage(living,ShrineRules.power(caster)) : 6);
            } else if(caster instanceof ServerPlayer player&&ShrineRules.nonlivingTarget(target)) ShrineRules.dismantle(target,player);
            else continue;
            Vec3 middle=target.getBoundingBox().getCenter();
            if(audibleHits++<2) domain.level.playSound(null,BlockPos.containing(middle),DomainSounds.SLASH_HIT.get(),SoundSource.PLAYERS,0.45f,0.94f+domain.level.random.nextFloat()*0.12f);
            if(hit++<32) {
                domain.level.sendParticles(ParticleTypes.SWEEP_ATTACK,middle.x,middle.y,middle.z,1,0.2,0.4,0.2,0);
                domain.level.sendParticles(new DustParticleOptions(new Vector3f(0.65f,0.015f,0.035f),0.8f),middle.x,middle.y,middle.z,6,0.4,0.5,0.4,0.02);
            }
        }

    }

    private static boolean eligible(LivingEntity target, LivingEntity caster) {
        if (!target.isAlive() || target.isRemoved() || target == caster || target instanceof ArmorStand) return false;
        if (target instanceof ServerPlayer player && (player.isCreative() || player.isSpectator())) return false;
        if(caster instanceof dev.unlimitedvoid.entity.JujutsuNpc npc)return NpcTechniqueService.enemy(npc,target);
        if (caster.isAlliedTo(target)) return false;
        return !(target instanceof TamableAnimal tame && caster.getUUID().equals(tame.getOwnerUUID()));
    }

    public record FugaDust(UUID owner,Vec3 center,double radius){}
    public static boolean fugaEligible(LivingEntity target,LivingEntity caster){return eligible(target,caster);}
    public static int fugaDust(UUID caster){
        return DOMAINS.values().stream().filter(d->d.kind==DomainKind.SHRINE&&d.casterId.equals(caster)&&d.active(d.level.getServer().getTickCount()))
            .mapToInt(d->d.dust).max().orElse(0);
    }
    public static FugaDust consumeFugaDust(LivingEntity caster,Vec3 impact){
        int now=caster.level().getServer().getTickCount();
        for(Domain d:DOMAINS.values())if(d.kind==DomainKind.SHRINE&&d.casterId.equals(caster.getUUID())&&d.level==caster.level()
            &&d.active(now)&&d.dust>=dev.unlimitedvoid.FugaCombatRules.DUST_REQUIRED&&impact.distanceToSqr(d.center)<=d.maxRadius*d.maxRadius&&!contested(d,impact,now)){
            d.dust=0;return new FugaDust(d.owner,d.center,d.maxRadius);
        }
        return null;
    }
    public static boolean fugaContested(UUID owner,Vec3 point){
        Domain d=DOMAINS.get(owner);return d!=null&&contested(d,point,d.level.getServer().getTickCount());
    }
    public static boolean fugaSheltered(ServerLevel level,Vec3 origin,Vec3 point){
        int now=level.getServer().getTickCount();
        for(Domain d:DOMAINS.values())if(d.level==level&&d.kind==DomainKind.VOID&&d.active(now)
            &&origin.distanceToSqr(d.center)>d.maxRadius*d.maxRadius&&point.distanceToSqr(d.center)<d.maxRadius*d.maxRadius)return true;
        return false;
    }

    /** Pull at most the small boundary gap; never teleport through a solid obstacle or load a new chunk. */
    private static Vec3 safeEntry(Domain domain, LivingEntity target, double radius) {
        Vec3 delta = target.getBoundingBox().getCenter().subtract(domain.center);
        double insideRadius = Math.max(0.2, radius - Math.max(target.getBbWidth() * 0.5, target.getBbHeight() * 0.5) - 0.7);
        Vec3 offset = delta.length() > insideRadius
                ? delta.normalize().scale(insideRadius).subtract(delta) : Vec3.ZERO;
        if (offset.lengthSqr() > 0.0001 && !domain.level.noCollision(target, target.getBoundingBox().move(offset))) {
            // A radial correction can lower a grounded mob a few millimetres into its floor.
            // Preserve its height when a horizontal pull still puts the body safely inside.
            Vec3 horizontal = new Vec3(offset.x, 0, offset.z);
            if (delta.add(horizontal).length() <= radius - 0.25
                    && domain.level.noCollision(target, target.getBoundingBox().move(horizontal))) offset = horizontal;
            else return null;
        }
        Vec3 destination = target.position().add(offset);
        if (!domain.level.hasChunkAt(BlockPos.containing(destination))) return null;
        return destination;
    }

    private static void applyStasis(LivingEntity entity) {
        for (Attribute attribute : List.of(Attributes.MOVEMENT_SPEED, Attributes.FLYING_SPEED)) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null && instance.getModifier(STASIS_MOVEMENT) == null) {
                instance.addTransientModifier(new AttributeModifier(STASIS_MOVEMENT, "Unlimited Void stasis", -1,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
    }

    private static void pin(Captive captive) {
        LivingEntity entity = captive.entity;
        entity.stopUsingItem();
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0;
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
        if (entity instanceof Creeper creeper) creeper.setSwellDir(-1);
        if (entity.position().distanceToSqr(captive.anchor) > 0.000001) {
            // Entity#teleportTo dispatches to the ServerPlayer override for position reconciliation.
            entity.teleportTo(captive.anchor.x, captive.anchor.y, captive.anchor.z);
        }
        entity.hurtMarked = true;
    }

    private static void release(UUID id) {
        Captive captive = CAPTIVES.remove(id);
        if (captive == null) return;
        for (Attribute attribute : List.of(Attributes.MOVEMENT_SPEED, Attributes.FLYING_SPEED)) {
            AttributeInstance instance = captive.entity.getAttribute(attribute);
            if (instance != null) instance.removeModifier(STASIS_MOVEMENT);
        }
        captive.entity.setDeltaMovement(Vec3.ZERO);
        captive.entity.hurtMarked = true;
    }

    private static void releaseOwned(UUID owner) {
        for (UUID id : List.copyOf(CAPTIVES.keySet())) if (CAPTIVES.get(id).owner.equals(owner)) release(id);
    }

    private static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) DomainNetwork.sync(player);
    }

    @SubscribeEvent
    public static void attack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (!event.getEntity().level().isClientSide && attacker != null && isCaptive(attacker.getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void playerAttack(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide && isCaptive(event.getEntity().getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent event) {
        if (!event.getEntity().level().isClientSide && event.isCancelable() && isCaptive(event.getEntity().getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void useItem(LivingEntityUseItemEvent.Start event) {
        if (!event.getEntity().level().isClientSide && isCaptive(event.getEntity().getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (!event.getPlayer().level().isClientSide && isCaptive(event.getPlayer().getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void placeBlock(BlockEvent.EntityPlaceEvent event) {
        Entity actor = event.getEntity();
        if (actor != null && !actor.level().isClientSide && isCaptive(actor.getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void teleport(EntityTeleportEvent event) {
        if (!event.getEntity().level().isClientSide && event.isCancelable()
                && (isCaptive(event.getEntity().getUUID()) || (event.getEntity() instanceof LivingEntity living && ClosedBarrier.blocksTeleport(living,event.getTarget())))) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void travel(EntityTravelToDimensionEvent event) {
        if (!event.getEntity().level().isClientSide && ClosedBarrier.contains(event.getEntity().getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void mount(EntityMountEvent event) {
        if (!event.getEntityMounting().level().isClientSide && isCaptive(event.getEntityMounting().getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void explosion(ExplosionEvent.Start event) {
        if (event.getLevel().isClientSide) return;
        Entity direct = event.getExplosion().getDirectSourceEntity();
        LivingEntity indirect = event.getExplosion().getIndirectSourceEntity();
        if ((direct != null && isCaptive(direct.getUUID()))
                || (indirect != null && isCaptive(indirect.getUUID()))) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for(Domain d:List.copyOf(DOMAINS.values()))if(d.casterId.equals(player.getUUID()))dismiss(d.owner);
        releaseOwned(player.getUUID());
        release(player.getUUID());
        if (player.getServer() != null) syncAll(player.getServer());
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        DOMAINS.values().removeIf(domain -> domain.level == level);
        ShrineStructures.retain(DOMAINS.keySet());
        ClosedBarrier.retain(DOMAINS.keySet());
        for (UUID id : List.copyOf(CAPTIVES.keySet())) {
            if (CAPTIVES.get(id).entity.level() == level) release(id);
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppingEvent event) {
        for (UUID id : List.copyOf(CAPTIVES.keySet())) release(id);
        DOMAINS.clear();
        ClosedBarrier.clear();
        ShrineStructures.clear();
        BARRIER_HP.clear();
        LAST_RESET.clear();
        SELECTED.clear();
        LAST_CYCLE.clear();
        READY_AT.clear();
        LAST_REQUEST.clear();
    }

    private static double ShrineGeometryY(Vec3 center,int age) {return center.y-1+10.27*DomainTimeline.rise(age)+0.08;}
    private static final class Domain {
        final UUID owner,casterId;final boolean generated,npc;final ServerLevel level;final Vec3 center,axis;final int startedAt;final DomainKind kind;final float maxRadius;
        int closingAt=-1,dust;boolean sealAdded,lifting;
        Domain(UUID owner,UUID casterId,boolean generated,ServerLevel level,Vec3 center,Vec3 axis,int startedAt,DomainKind kind,float maxRadius){
            this.owner=owner;this.casterId=casterId;this.generated=generated;this.npc=level.getEntity(casterId) instanceof dev.unlimitedvoid.entity.JujutsuNpc;this.level=level;this.center=center;this.axis=axis;this.startedAt=startedAt;this.kind=kind;this.maxRadius=maxRadius;
        }
        int total(){return DomainTimeline.formed(kind)+DURATION;}
        boolean active(int now){return closingAt<0 && now-startedAt>=DomainTimeline.formed(kind);}
        float radius(int now){return maxRadius*DomainTimeline.growth(kind,now-startedAt);}
    }

    private record Captive(UUID owner, LivingEntity entity, Vec3 anchor, int capturedAt) {}
}
