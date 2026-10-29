package dev.unlimitedvoid.entity;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.server.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.*;

/** Only Gojo attacks monsters. True-form Sukuna outranks ordinary monsters for Shinjuku Gojo. */
public final class GojoCombatGoal extends Goal {
    private final JujutsuNpc npc;private long ready,punchReady;private boolean engaged;private UUID duelFocus,duelSpent;private int scan;private LivingEntity target;
    public GojoCombatGoal(JujutsuNpc npc){this.npc=npc;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public void start(){if(!engaged){ready=npc.level().getGameTime()+NpcMeleeRules.REMOTE_INTERVAL;engaged=true;}}
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
            DomainService.dismissNpc(npc);npc.cancelPunch();NpcTechniqueService.cancel(npc);npc.getNavigation().stop();find();
            if(NpcTechniqueService.instantPurple(npc,target)){npc.clearAwakeningBurst();ready=now+LimitlessKind.PURPLE.cooldown;}return;
        }
        if(NpcCombatRules.lastResort(npc.getHealth(),npc.getMaxHealth())&&!npc.lastResortUsed()){
            DomainService.dismissNpc(npc);npc.cancelPunch();NpcTechniqueService.cancel(npc);npc.getNavigation().stop();
            if(NpcTechniqueService.start(npc,LimitlessKind.PURPLE,null,true))npc.markLastResort();return;
        }
        if(scan++%10==0||target==null||!NpcTechniqueService.enemy(npc,target))find();npc.setTarget(target);
        if(target==null)return;if(DomainService.isCaptive(npc.getUUID())||DomainService.npcChanting(npc)){npc.getNavigation().stop();return;}if(NpcDomainService.spontaneous(npc))return;boolean duel=npc.isShinjuku()&&NpcTechniqueService.trueSukuna(target);
        if(duel&&!target.getUUID().equals(duelFocus)){npc.cancelPunch();NpcTechniqueService.cancel(npc);ready=0;duelFocus=target.getUUID();}
        if(!duel)duelFocus=null;boolean opening=duel&&!target.getUUID().equals(duelSpent);
        npc.getLookControl().setLookAt(target,30,30);
        double distance=npc.distanceToSqr(target);boolean visible=npc.hasLineOfSight(target);
        if(NpcTechniqueService.detonating(npc)) {npc.getNavigation().stop();return;}
        if(opening){
            if(distance>1.8*1.8||!visible){npc.getNavigation().moveTo(target,1.65);return;}
            if(now>=ready&&!NpcTechniqueService.casting(npc)&&NpcTechniqueService.instantPurple(npc,target)){duelSpent=target.getUUID();ready=now+LimitlessKind.PURPLE.cooldown;punchReady=now+NpcMeleeRules.INTERVAL;return;}
        }
        if(distance>NpcMeleeRules.REACH*NpcMeleeRules.REACH||!visible){
            npc.getNavigation().moveTo(target,1.35);
            if(!opening&&distance>36&&distance<24*24&&visible&&now>=ready&&now>=punchReady&&!NpcTechniqueService.casting(npc))tryRemote(now);
            return;
        }
        npc.getNavigation().stop();
        if(now<punchReady||NpcTechniqueService.charging(npc)||npc.punchKind()>=0)return;
        if(!opening&&now>=ready&&!NpcTechniqueService.casting(npc)&&tryRemote(now))return;
        var kind=NpcMeleeRules.punch(npc.isAwakened(),npc.getRandom().nextInt(100));
        if(NpcMeleeService.start(npc,target,kind))punchReady=now+NpcMeleeRules.INTERVAL;
    }
    private boolean tryRemote(long now){
        punchReady=now+NpcMeleeRules.INTERVAL;
        if(NpcMeleeRules.preferPunch(npc.getRandom().nextInt(100)))return false;
        var kind=NpcCombatRules.choose(npc.isAwakened(),npc.getRandom().nextInt(100));
        if(NpcTechniqueService.start(npc,kind,target,false)){ready=now+Math.max(NpcMeleeRules.REMOTE_INTERVAL,kind.cooldown);return true;}return false;
    }

    @Override public void stop(){target=null;npc.setTarget(null);npc.getNavigation().stop();}
}
