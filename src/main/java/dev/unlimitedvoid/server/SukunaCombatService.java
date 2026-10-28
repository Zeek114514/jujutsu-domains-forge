package dev.unlimitedvoid.server;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.JujutsuNpc;
import dev.unlimitedvoid.network.DomainNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;
/** NPC attacks protect players and allies and never modify terrain. Ordinary attacks respect Infinity. */
public final class SukunaCombatService {
    private static boolean allowed(JujutsuNpc n,LivingEntity t){return !n.isGojo()&&!n.summoningMahoraga()&&n.isAlive()&&!n.isNoAi()&&!DomainService.isCaptive(n.getUUID())&&!DomainService.npcChanting(n)&&!FugaService.casting(n.getUUID())&&NpcTechniqueService.enemy(n,t)&&n.hasLineOfSight(t);}
    public static boolean punch(JujutsuNpc n,LivingEntity t){if(!allowed(n,t)||n.punchKind()>=0||n.distanceToSqr(t)>NpcMeleeRules.REACH*NpcMeleeRules.REACH)return false;n.beginStrike(t.getUUID());return true;}
    public static void impact(JujutsuNpc n,LivingEntity t){if(!allowed(n,t)||n.distanceToSqr(t)>2.8*2.8)return;n.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);if(t.hurt(n.damageSources().mobAttack(n),18)){t.knockback(.35,n.getX()-t.getX(),n.getZ()-t.getZ());((ServerLevel)n.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,t.getX(),t.getY()+1,t.getZ(),5,.2,.3,.2,.05);}}
    public static boolean slash(JujutsuNpc n,LivingEntity t){
        if(!allowed(n,t)||n.distanceToSqr(t)>24*24)return false;boolean cleave=n.distanceToSqr(t)<4*4;
        Vec3 center=t.getBoundingBox().getCenter(),side=n.getLookAngle().cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.1)side=new Vec3(1,0,0);
        Vec3 extent=side.scale(cleave?2:3).add(0,cleave?1.7:1,0);
        DomainNetwork.npcSlash((ServerLevel)n.level(),n.getUUID(),center.subtract(extent),center.add(extent),cleave);
        n.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
        n.level().playSound(null,n.blockPosition(),DomainSounds.SLASH_WIND.get(),SoundSource.HOSTILE,.55f,1);
        if(t.hurt(n.damageSources().mobAttack(n),cleave?32:24))n.level().playSound(null,t.blockPosition(),DomainSounds.SLASH_HIT.get(),SoundSource.HOSTILE,.5f,1);
        return true;
    }
    public static boolean summon(JujutsuNpc n,LivingEntity t){
        if(!n.isMegumi()||n.getPersistentData().hasUUID("uv_mahoraga_uuid")||!allowed(n,t)||!n.summonReady())return false;
        n.summonAttempted();var event=new MahoragaSummonEvent(n,t);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);return event.summoned();
    }
    private SukunaCombatService(){}
}
