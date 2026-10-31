package dev.unlimitedvoid.server;

import dev.unlimitedvoid.entity.JujutsuNpc;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Called only after a successful hurt: an invulnerable or missed target never flinches. */
public final class NpcHitFeedback {
    private NpcHitFeedback(){}
    public static void impact(JujutsuNpc source,LivingEntity target,boolean finisher,int kind){
        if(target instanceof JujutsuNpc npc){
            npc.stagger(finisher);
            // These NPCs have full vanilla knockback resistance; small authored impulses
            // make landed hand-to-hand hits readable without changing their attributes.
            Vec3 away=target.position().subtract(source.position()).multiply(1,0,1);
            if(away.lengthSqr()>.001&&kind==2){
                Vec3 impulse=away.normalize().scale(finisher?.38:.10);
                target.setDeltaMovement(target.getDeltaMovement().scale(.4).add(impulse).add(0,finisher?.12:.02,0));target.hurtMarked=true;
            }
        }
        if(source.level() instanceof ServerLevel level){
            Vec3 p=target.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.CRIT,p.x,p.y,p.z,finisher?9:4,.15,.22,.15,.05);
            if(finisher)level.sendParticles(ParticleTypes.SWEEP_ATTACK,p.x,p.y,p.z,1,0,0,0,0);
        }
    }
}
