package dev.unlimitedvoid.server;

import dev.unlimitedvoid.NpcAttackMotion;
import dev.unlimitedvoid.entity.JujutsuNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Short, visible decisions with ordinary navigation, sight checks and bounded cooldowns. */
public final class NpcTactics {
    private NpcTactics(){}
    public static boolean tick(JujutsuNpc n,LivingEntity target){
        long now=n.level().getGameTime();
        if(n.tacticUntil>now){n.getLookControl().setLookAt(target,30,30);return true;}
        if(n.isNoAi()||n.punchKind()>=0||n.staggered()||NpcTechniqueService.casting(n)||FugaService.casting(n.getUUID())||now<n.nextTacticAt||!n.onGround()||!n.hasLineOfSight(target))return false;
        double distance=n.distanceToSqr(target);
        boolean windup=target instanceof JujutsuNpc t&&t.punchKind()>=0&&t.punchAge(0)<NpcAttackMotion.contact(t.comboStep(),t.punchKind());
        boolean charge=target instanceof JujutsuNpc t&&NpcTechniqueService.charging(t);
        Vec3 away=n.position().subtract(target.position()).multiply(1,0,1).normalize();
        if(away.lengthSqr()<.5)return false;
        if((windup||charge)&&distance<36&&n.getRandom().nextFloat()<(n.isGojo()?.65f:.45f)){
            Vec3 side=new Vec3(-away.z,0,away.x).scale(n.getRandom().nextBoolean()?1:-1);
            if(move(n,n.position().add(side.scale(n.isGojo()?2.3:1.8)).add(away.scale(.6)),1,now,9,1.55))return true;
        }
        if(n.canReverse()&&n.getHealth()/n.getMaxHealth()<.30f&&n.getHealth()/n.getMaxHealth()>.06f&&distance<16){
            if(move(n,n.position().add(away.scale(n.isGojo()?3.5:2.7)),2,now,14,1.3))return true;
        }
        if(target instanceof JujutsuNpc t&&t.staggered()&&distance>5&&distance<64){
            n.getNavigation().moveTo(target,1.65);n.showTactic(3,8);n.tacticUntil=now+8;n.nextTacticAt=now+45;return true;
        }
        n.nextTacticAt=now+10;
        return false;
    }
    private static boolean move(JujutsuNpc n,Vec3 destination,int action,long now,int ticks,double speed){
        BlockPos feet=BlockPos.containing(destination);
        if(!n.level().hasChunkAt(feet)||!n.level().getBlockState(feet.below()).isFaceSturdy(n.level(),feet.below(),Direction.UP)||!n.level().getFluidState(feet).isEmpty()||!n.level().noCollision(n,n.getBoundingBox().move(destination.subtract(n.position()))))return false;
        if(!n.getNavigation().moveTo(destination.x,destination.y,destination.z,speed))return false;
        n.tacticUntil=now+ticks;n.nextTacticAt=now+(n.isGojo()?45:60);n.showTactic(action,ticks);return true;
    }
}
