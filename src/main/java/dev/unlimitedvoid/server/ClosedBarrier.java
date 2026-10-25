package dev.unlimitedvoid.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Physical confinement, independent of enemy paralysis and damage immunity. */
public final class ClosedBarrier {
    private record Seal(UUID owner, ServerLevel level, Vec3 center) {}
    private static final Map<UUID, Seal> SEALS = new LinkedHashMap<>();
    private static final Map<UUID, Member> MEMBERS = new HashMap<>();
    private static final class Member {
        final UUID seal; final LivingEntity entity;
        Vec3 safe;
        Member(UUID seal, LivingEntity entity) { this.seal=seal; this.entity=entity; this.safe=entity.position(); }
    }
    public static void add(UUID owner, ServerLevel level, Vec3 center) {
        SEALS.put(owner, new Seal(owner, level, center));
        enroll(SEALS.get(owner));
    }
    public static void remove(UUID owner) {
        SEALS.remove(owner);
        MEMBERS.values().removeIf(m -> m.seal.equals(owner));
    }
    public static void retain(Set<UUID> active) {
        for (UUID owner : List.copyOf(SEALS.keySet())) if (!active.contains(owner)) remove(owner);
    }
    public static boolean contains(UUID id) { return MEMBERS.containsKey(id); }
    public static boolean blocksTeleport(LivingEntity entity, Vec3 target) {
        Member member=MEMBERS.get(entity.getUUID());
        if (member==null || !entity.isAlive() || entity.isSpectator()) return false;
        Seal seal=SEALS.get(member.seal);
        if (seal==null) return false;
        double limit=limit(entity);
        Vec3 destinationMiddle=target.add(0,entity.getBbHeight()/2.0,0);
        return destinationMiddle.distanceToSqr(seal.center)>limit*limit;
    }
    private static double limit(LivingEntity entity) {
        return 16-Math.max(entity.getBbWidth()/2.0,entity.getBbHeight()/2.0)-0.2;
    }
    private static void enroll(Seal seal) {
        AABB box=new AABB(seal.center.subtract(16,16,16),seal.center.add(16,16,16));
        for (LivingEntity entity:seal.level.getEntitiesOfClass(LivingEntity.class,box)) {
            if (entity.isAlive() && !entity.isSpectator() && entity.getBoundingBox().getCenter().distanceToSqr(seal.center)<=256) {
                MEMBERS.putIfAbsent(entity.getUUID(),new Member(seal.owner,entity));
            }
        }
    }
    public static void tick() {
        for (Seal seal:SEALS.values()) enroll(seal);
        for (UUID id:List.copyOf(MEMBERS.keySet())) {
            Member member=MEMBERS.get(id);
            LivingEntity entity=member.entity;
            Seal seal=SEALS.get(member.seal);
            if (seal==null || !entity.isAlive() || entity.isRemoved() || entity.isSpectator() || entity.level()!=seal.level) {
                MEMBERS.remove(id); continue;
            }
            Vec3 delta=entity.getBoundingBox().getCenter().subtract(seal.center);
            double limit=limit(entity);
            if (delta.lengthSqr()<=limit*limit) { member.safe=entity.position(); continue; }
            // Use a collision-tested projection onto the inner face. If terrain blocks
            // that correction, return to the last known position inside the seal.
            Vec3 correction=delta.normalize().scale(limit).subtract(delta);
            Vec3 position=entity.position().add(correction);
            if (!seal.level.noCollision(entity,entity.getBoundingBox().move(correction))) {
                Vec3 horizontal=new Vec3(correction.x,0,correction.z);
                if (delta.add(horizontal).lengthSqr()<=limit*limit+0.01 && seal.level.noCollision(entity,entity.getBoundingBox().move(horizontal)))
                    position=entity.position().add(horizontal);
                else position=member.safe;
            }
            entity.teleportTo(position.x,position.y,position.z);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.fallDistance=0;
            entity.hurtMarked=true;
            member.safe=position;
        }
    }
    public static void clear() { SEALS.clear(); MEMBERS.clear(); }
}
