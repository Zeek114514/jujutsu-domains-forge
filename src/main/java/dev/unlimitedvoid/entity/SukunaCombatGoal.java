package dev.unlimitedvoid.entity;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.server.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.*;
public final class SukunaCombatGoal extends Goal {
    private final JujutsuNpc npc;private LivingEntity target;private long ready,rareAt;private int scan;
    public SukunaCombatGoal(JujutsuNpc npc){this.npc=npc;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    private LivingEntity find(){var list=npc.level().getEntitiesOfClass(LivingEntity.class,npc.getBoundingBox().inflate(32),t->NpcTechniqueService.enemy(npc,t));list.sort(Comparator.<LivingEntity>comparingInt(t->t instanceof JujutsuNpc?0:1).thenComparingDouble(npc::distanceToSqr));return target=list.isEmpty()?null:list.get(0);}
    @Override public boolean canUse(){return !npc.isGojo()&&(npc.isMegumi()&&NpcCombatRules.lastResort(npc.getHealth(),npc.getMaxHealth())||find()!=null);}
    @Override public boolean canContinueToUse(){return !npc.isGojo()&&(target!=null&&NpcTechniqueService.enemy(npc,target)&&npc.distanceToSqr(target)<48*48||FugaService.casting(npc.getUUID())||DomainService.npcChanting(npc));}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void start(){rareAt=Math.max(rareAt,npc.level().getGameTime()+NpcBattleRules.INITIAL_DELAY);}
    @Override public void tick(){
        if(npc.isMegumi()&&NpcCombatRules.lastResort(npc.getHealth(),npc.getMaxHealth()))npc.becomeTrueSukuna();
        if(scan++%10==0||target==null||!NpcTechniqueService.enemy(npc,target))find();npc.setTarget(target);
        if(npc.summoningMahoraga()||DomainService.isCaptive(npc.getUUID())||DomainService.npcChanting(npc)||FugaService.casting(npc.getUUID())){npc.getNavigation().stop();return;}
        if(target==null)return;npc.getLookControl().setLookAt(target,30,30);
        if(NpcDomainService.spontaneous(npc))return;
        if(SukunaCombatService.descend(npc,target))return;
        long now=npc.level().getGameTime();
        if(now>=rareAt){rareAt=now+NpcBattleRules.ROLL_INTERVAL;if(NpcBattleRules.fuga(npc.getRandom().nextInt(1000))&&FugaService.request(npc))return;if(npc.isMegumi()&&NpcBattleRules.mahoraga(npc.getRandom().nextInt(1000))&&SukunaCombatService.summon(npc,target))return;}
        if(now<ready||npc.punchKind()>=0)return;double distance=npc.distanceToSqr(target);
        if(distance>NpcMeleeRules.REACH*NpcMeleeRules.REACH){npc.getNavigation().moveTo(target,1.4);if(distance<24*24&&npc.hasLineOfSight(target)&&npc.getRandom().nextInt(100)<35&&SukunaCombatService.slash(npc,target))ready=now+30;return;}
        npc.getNavigation().stop();
        if(npc.getRandom().nextInt(100)<35){if(SukunaCombatService.slash(npc,target))ready=now+24;}
        else if(SukunaCombatService.punch(npc,target))ready=now+16;
    }
    @Override public void stop(){npc.descendingShrine(false);target=null;npc.setTarget(null);npc.getNavigation().stop();}
}
