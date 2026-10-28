package dev.unlimitedvoid.server;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.JujutsuNpc;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Single-target wind-up, actual reach/visibility check, and a synchronized block-hand animation. */
public final class NpcMeleeService {
    private NpcMeleeService(){}
    public static boolean start(JujutsuNpc npc,LivingEntity target,LimitlessKind kind){
        if(!npc.isGojo()||!npc.isAlive()||(DomainService.isCaptive(npc.getUUID())||DomainService.npcChanting(npc))||npc.isNoAi()||kind==LimitlessKind.PURPLE||kind==LimitlessKind.RED&&!npc.isAwakened()||npc.punchKind()>=0||!NpcTechniqueService.enemy(npc,target)||!npc.hasLineOfSight(target)||npc.distanceToSqr(target)>NpcMeleeRules.REACH*NpcMeleeRules.REACH||NpcTechniqueService.charging(npc)||NpcTechniqueService.detonating(npc))return false;
        npc.beginPunch(kind,target.getUUID());
        if(kind==LimitlessKind.BLUE){var pull=npc.position().subtract(target.position());if(pull.lengthSqr()>.01){target.setDeltaMovement(target.getDeltaMovement().scale(.5).add(pull.normalize().scale(.22*MahoragaService.force(target,MahoragaRules.Kind.BLUE))));target.hurtMarked=true;}}
        particles(npc,kind,npc.getEyePosition().add(npc.getLookAngle().scale(.55)).add(0,-.3,0),6);
        return true;
    }
    public static void impact(JujutsuNpc npc,LivingEntity target,LimitlessKind kind){
        if(!npc.isAlive()||(DomainService.isCaptive(npc.getUUID())||DomainService.npcChanting(npc))||NpcTechniqueService.charging(npc)||NpcTechniqueService.detonating(npc)||!NpcTechniqueService.enemy(npc,target)||!npc.hasLineOfSight(target)||npc.distanceToSqr(target)>2.8*2.8)return;
        npc.swing(InteractionHand.MAIN_HAND,true);
        boolean hurt=target instanceof dev.unlimitedvoid.entity.MahoragaEntity mahoraga?mahoraga.hurtAs(kind==LimitlessKind.RED?MahoragaRules.Kind.RED:MahoragaRules.Kind.BLUE,npc.damageSources().mobAttack(npc),NpcMeleeRules.damage(kind)):target.hurt(npc.damageSources().mobAttack(npc),NpcMeleeRules.damage(kind));if(!hurt)return;
        Vec3 direction=kind==LimitlessKind.RED?target.position().subtract(npc.position()):npc.position().subtract(target.position());
        if(direction.lengthSqr()>.01){target.setDeltaMovement(direction.normalize().scale(kind==LimitlessKind.RED?1.5:.30).add(0,kind==LimitlessKind.RED?.3:.04,0).scale(MahoragaService.force(target,kind==LimitlessKind.RED?MahoragaRules.Kind.RED:MahoragaRules.Kind.BLUE)));target.hurtMarked=true;}
        particles(npc,kind,target.getBoundingBox().getCenter(),12);
        if(npc.level() instanceof ServerLevel server)server.playSound(null,npc.getX(),npc.getY(),npc.getZ(),net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(UnlimitedVoid.ID,kind==LimitlessKind.RED?"red_impact":"blue_impact")),net.minecraft.sounds.SoundSource.HOSTILE,DomainSounds.gain(.22f),1.15f);
    }
    private static void particles(JujutsuNpc npc,LimitlessKind kind,Vec3 p,int amount){if(npc.level() instanceof ServerLevel server)server.sendParticles(new DustParticleOptions(kind==LimitlessKind.RED?new Vector3f(1,.10f,.16f):new Vector3f(.10f,.65f,1),.85f),p.x,p.y,p.z,amount,.10,.13,.10,.02);}
}
