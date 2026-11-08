package dev.unlimitedvoid.entity;

import dev.unlimitedvoid.server.NpcControlService;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public final class NpcOrderGoal extends Goal {
    private final PathfinderMob npc;
    public NpcOrderGoal(PathfinderMob npc){this.npc=npc;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.TARGET));}
    @Override public boolean canUse(){return NpcControlService.mode(npc)!=0;}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){
        npc.setTarget(null);
        if(NpcControlService.mode(npc)==1&&npc.getPersistentData().hasUUID("uv_follow")){
            var player=npc.level().getPlayerByUUID(npc.getPersistentData().getUUID("uv_follow"));
            if(player!=null&&player.isAlive()&&npc.distanceToSqr(player)<64*64){
                npc.getLookControl().setLookAt(player,30,30);
                if(npc.distanceToSqr(player)>6.25){npc.getNavigation().moveTo(player,1.15);return;}
            }
        }npc.getNavigation().stop();
    }
}
