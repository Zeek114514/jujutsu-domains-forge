package dev.unlimitedvoid.entity;

import dev.unlimitedvoid.DomainEntities;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** Non-damaging, server-owned leash and kick interaction; never overrides a player's leash. */
final class SunDogPrankGoal extends Goal {
    private final InnocentSunDog actor;
    private InnocentSunDog partner;
    private int elapsed;
    private boolean kicking,contact;
    SunDogPrankGoal(InnocentSunDog actor){this.actor=actor;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    private boolean valid(InnocentSunDog other){return other.isAlive()&&!other.isNoAi()&&other.getType()==DomainEntities.SUN_DOG_SERVANT.get()&&(!other.isLeashed()||other.getLeashHolder()==actor)&&actor.hasLineOfSight(other);}
    @Override public boolean canUse(){
        if(!actor.isSunDog()||actor.level().getGameTime()<actor.nextPrankAt)return false;
        partner=actor.level().getEntitiesOfClass(InnocentSunDog.class,actor.getBoundingBox().inflate(3),this::valid).stream()
                .filter(e->actor.distanceToSqr(e)<=9).min(Comparator.comparingDouble(actor::distanceToSqr)).orElse(null);
        if(partner==null)return false;
        partner.setLeashedTo(actor,true);
        actor.nextPrankAt=actor.level().getGameTime()+160;
        if(!InnocentSunDog.shouldPrank(actor.getRandom().nextFloat())){partner=null;return false;}
        return true;
    }
    @Override public boolean canContinueToUse(){return partner!=null&&valid(partner)&&actor.distanceToSqr(partner)<64&&elapsed<120&&(!kicking||actor.kickAge(0)<InnocentSunDog.KICK_DURATION);}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void start(){elapsed=0;kicking=false;contact=false;actor.prankActive=true;}
    @Override public void stop(){actor.getNavigation().stop();actor.prankActive=false;partner=null;actor.nextPrankAt=Math.max(actor.nextPrankAt,actor.level().getGameTime()+40);}
    @Override public void tick(){
        if(partner==null||!valid(partner))return;
        elapsed++;
        Vec3 forward=new Vec3(-Math.sin(Math.toRadians(partner.yBodyRot)),0,Math.cos(Math.toRadians(partner.yBodyRot)));
        Vec3 behind=partner.position().subtract(forward.scale(1.15));
        if(!kicking){
            actor.getLookControl().setLookAt(partner,30,30);
            if(actor.position().distanceToSqr(behind)>.30){
                actor.getNavigation().moveTo(behind.x,behind.y,behind.z,1);
                // A path ending in the same floor cell can already be marked complete;
                // finish the last fraction of a block with collision-aware walking.
                if(actor.getNavigation().isDone())actor.getMoveControl().setWantedPosition(behind.x,behind.y,behind.z,1);
                return;
            }
            if(!actor.onGround()||!partner.onGround())return;
            actor.getNavigation().stop();actor.beginKick();kicking=true;
        }
        float yaw=(float)Math.toDegrees(Math.atan2(partner.getZ()-actor.getZ(),partner.getX()-actor.getX()))-90;
        actor.setYRot(yaw);actor.setYBodyRot(yaw);actor.setYHeadRot(yaw);
        if(!contact&&actor.kickAge(0)>=InnocentSunDog.KICK_CONTACT){
            contact=true;
            Vec3 offset=actor.position().subtract(partner.position());
            if(actor.distanceToSqr(partner)<=4&&Math.abs(offset.y)<1&&offset.dot(forward)<-.15&&actor.hasLineOfSight(partner)){
                partner.knockback(.6,actor.getX()-partner.getX(),actor.getZ()-partner.getZ());partner.hurtMarked=true;partner.recoil();
            }
        }
    }
}
