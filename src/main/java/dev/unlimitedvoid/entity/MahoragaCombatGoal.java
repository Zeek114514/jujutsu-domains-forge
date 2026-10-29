package dev.unlimitedvoid.entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.*;
public final class MahoragaCombatGoal extends Goal {
    private final MahoragaEntity actor;private LivingEntity target;private int scan;
    public MahoragaCombatGoal(MahoragaEntity actor){this.actor=actor;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    private LivingEntity find(){var list=actor.level().getEntitiesOfClass(LivingEntity.class,actor.getBoundingBox().inflate(48),t->actor.enemy(t)&&actor.hasLineOfSight(t));list.sort(Comparator.<LivingEntity>comparingInt(t->t instanceof JujutsuNpc?0:1).thenComparingDouble(actor::distanceToSqr));return target=list.isEmpty()?null:list.get(0);}
    @Override public boolean canUse(){return actor.canAct()&&find()!=null;}
    @Override public boolean canContinueToUse(){return actor.canAct()&&target!=null&&actor.enemy(target)&&actor.distanceToSqr(target)<64*64;}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){if(scan++%10==0||target==null||!actor.enemy(target))find();actor.setTarget(target);if(target==null)return;actor.getLookControl().setLookAt(target,30,30);if(actor.attackType()>0){actor.getNavigation().stop();return;}if(actor.startAttack(target))return;if(actor.distanceToSqr(target)>3.3*3.3)actor.getNavigation().moveTo(target,1.25);else actor.getNavigation().stop();}
    @Override public void stop(){target=null;actor.setTarget(null);actor.getNavigation().stop();}
}
