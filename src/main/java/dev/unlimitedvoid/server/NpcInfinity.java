package dev.unlimitedvoid.server;

import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.JujutsuNpc;
import net.minecraft.world.damagesource.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Defensive Infinity only: no combat target, reflection damage or world-wide projectile scans. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID)
public final class NpcInfinity {
    private static boolean active(JujutsuNpc npc){return npc.isAlive()&&npc.isGojo()&&DomainConfig.GOJO_INFINITY.get();}
    @SubscribeEvent public static void attack(LivingAttackEvent event){
        if(!(event.getEntity() instanceof JujutsuNpc npc)||npc.level().isClientSide||!active(npc))return;
        var source=event.getSource();
        if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        if(source.is(DamageTypeTags.IS_PROJECTILE)||source.is(DamageTypes.PLAYER_ATTACK)||source.is(DamageTypes.MOB_ATTACK)||source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)){
            event.setCanceled(true);spark(npc,npc.getBoundingBox().getCenter());
        }
    }
    @SubscribeEvent public static void impact(ProjectileImpactEvent event){
        if(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit&&hit.getEntity() instanceof JujutsuNpc npc&&!npc.level().isClientSide&&active(npc)){
            event.setCanceled(true);event.getProjectile().setDeltaMovement(Vec3.ZERO);spark(npc,hit.getLocation());
        }
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){
        if(!(event.getEntity() instanceof JujutsuNpc npc)||npc.level().isClientSide||!active(npc))return;
        var shield=npc.getBoundingBox().inflate(.65);int checked=0;
        for(var projectile:npc.level().getEntitiesOfClass(Projectile.class,npc.getBoundingBox().inflate(4))){
            if(checked++>=64)break;
            var velocity=projectile.getDeltaMovement();if(velocity.lengthSqr()<.000001)continue;
            var start=projectile.position();var hit=shield.clip(start,start.add(velocity.scale(4)));
            if(shield.contains(start)){
                // Leave gravity flags and ownership untouched so removing Gojo releases remaining arrows.
                projectile.setDeltaMovement(Vec3.ZERO);
            }else if(hit.isPresent()){
                double allowed=Math.max(0,start.distanceTo(hit.get())-.08)*.22;
                if(velocity.length()>allowed)projectile.setDeltaMovement(velocity.normalize().scale(allowed));
            }
            if(npc.tickCount%10==0&&hit.isPresent())spark(npc,start);
        }
    }
    private static void spark(JujutsuNpc npc,Vec3 point){
        if(npc.level() instanceof ServerLevel server)server.sendParticles(ParticleTypes.END_ROD,point.x,point.y,point.z,2,.08,.08,.08,.005);
    }
}
