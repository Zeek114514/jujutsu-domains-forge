package dev.unlimitedvoid.server;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.JujutsuNpc;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;
import java.util.*;
/** One domain per NPC family per dimension; the existing domain engine owns all effects. */
public final class NpcDomainService {
    private static final Map<JujutsuNpc,Long> NEXT=new WeakHashMap<>();
    public static boolean spontaneous(JujutsuNpc npc){
        if(npc.isNoAi()||!npc.isAlive())return false;
        long now=npc.level().getGameTime(),next=NEXT.computeIfAbsent(npc,n->now+NpcBattleRules.INITIAL_DELAY);
        if(now<next)return false;NEXT.put(npc,now+NpcBattleRules.ROLL_INTERVAL);
        return npc.domainReady()&&NpcBattleRules.domain(npc.getRandom().nextInt(1000))&&DomainService.expandNpc(npc,false);
    }
    public static void respond(ServerLevel level,DomainKind attempted,Vec3 center,float radius){
        DomainKind response=attempted==DomainKind.VOID?DomainKind.SHRINE:DomainKind.VOID;
        if(DomainService.npcFamilyDomain(level,response))return;
        double range=Math.max(64,radius+32);
        var candidates=level.getEntitiesOfClass(JujutsuNpc.class,new AABB(center,center).inflate(range),n->n.isAlive()&&!n.isNoAi()&&!DomainService.isCaptive(n.getUUID())&&n.isGojo()==(response==DomainKind.VOID)&&n.distanceToSqr(center)<=range*range);
        candidates.sort(Comparator.<JujutsuNpc>comparingDouble(n->n.distanceToSqr(center)).thenComparing(n->n.getUUID().toString()));
        for(var npc:candidates)if(DomainService.expandNpc(npc,true))break;
    }
    private NpcDomainService(){}
}
