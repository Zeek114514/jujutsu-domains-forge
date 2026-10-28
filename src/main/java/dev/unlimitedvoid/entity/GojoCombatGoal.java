package dev.unlimitedvoid.entity;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.server.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.*;

/** Only Gojo attacks monsters. True-form Sukuna outranks ordinary monsters for Shinjuku Gojo. */
public final class GojoCombatGoal extends Goal {
    private final JujutsuNpc npc;private long ready;private UUID duelFocus;private int scan;private LivingEntity target;
    public GojoCombatGoal(JujutsuNpc npc){this.npc=npc;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public boolean canUse(){return npc.isGojo()&&(npc.awakeningBurstPending()||NpcCombatRules.lastResort(npc.getHealth(),npc.getMaxHealth())&&!npc.lastResortUsed()||find()!=null);}
    @Override public boolean canContinueToUse(){return npc.isGojo()&&(npc.awakeningBurstPending()||target!=null&&NpcTechniqueService.enemy(npc,target)&&npc.distanceToSqr(target)<48*48||NpcTechniqueService.casting(npc)||NpcCombatRules.lastResort(npc.getHealth(),npc.getMaxHealth())&&!npc.lastResortUsed());}
    private LivingEntity find(){
        var candidates=npc.level().getEntitiesOfClass(LivingEntity.class,npc.getBoundingBox().inflate(32),e->NpcTechniqueService.enemy(npc,e)&&npc.hasLineOfSight(e));
        candidates.sort(Comparator.<LivingEntity>comparingInt(e->npc.isShinjuku()&&NpcTechniqueService.trueSukuna(e)?0:1).thenComparingDouble(npc::distanceToSqr));
        return target=candidates.isEmpty()?null:candidates.get(0);
    }
    @Override public void tick(){
        long now=npc.level().getGameTime();
        if(NpcCombatRules.lastResort(npc.getHealth(),npc.getMaxHealth())&&!npc.isAwakened())npc.awakenFromDanger();
        if(npc.awakeningBurstPending()){
            NpcTechniqueService.cancel(npc);npc.getNavigation().stop();find();
            if(NpcTechniqueService.instantPurple(npc,target)){npc.clearAwakeningBurst();ready=now+LimitlessKind.PURPLE.cooldown;}return;
        }
        if(NpcCombatRules.lastResort(npc.getHealth(),npc.getMaxHealth())&&!npc.lastResortUsed()){
            NpcTechniqueService.cancel(npc);npc.getNavigation().stop();
            if(NpcTechniqueService.start(npc,LimitlessKind.PURPLE,null,true))npc.markLastResort();return;
        }
        if(scan++%10==0||target==null||!NpcTechniqueService.enemy(npc,target))find();npc.setTarget(target);
        if(target==null)return;boolean duel=npc.isShinjuku()&&NpcTechniqueService.trueSukuna(target);
        if(duel&&!target.getUUID().equals(duelFocus)){NpcTechniqueService.cancel(npc);ready=0;duelFocus=target.getUUID();}
        if(!duel)duelFocus=null;
        npc.getLookControl().setLookAt(target,30,30);
        double distance=npc.distanceToSqr(target);
        if(duel?distance>1.8*1.8:distance>24*24||!npc.hasLineOfSight(target)){npc.getNavigation().moveTo(target,duel?1.65:1.15);return;}
        npc.getNavigation().stop();
        if(now<ready||NpcTechniqueService.casting(npc)||!npc.hasLineOfSight(target))return;
        var kind=duel?LimitlessKind.PURPLE:NpcCombatRules.choose(npc.isAwakened(),npc.getRandom().nextInt(100));
        if(duel?NpcTechniqueService.instantPurple(npc,target):NpcTechniqueService.start(npc,kind,target,false))ready=now+kind.cooldown;
    }
    @Override public void stop(){target=null;npc.setTarget(null);npc.getNavigation().stop();}
}
