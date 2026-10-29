package dev.unlimitedvoid.server;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.JujutsuNpc;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Final-damage interception permits a first awakening even when one hit would cross zero health. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID)
public final class NpcAwakening {
    @SubscribeEvent public static void damage(LivingDamageEvent event){
        if(event.getEntity() instanceof JujutsuNpc npc&&!npc.level().isClientSide&&npc.isGojo()&&!npc.isAwakened()&&event.getAmount()>0&&!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)&&NpcCombatRules.lastResort(Math.max(.001f,npc.getHealth()-event.getAmount()),npc.getMaxHealth())){
            if(npc.awakenFromDanger())event.setCanceled(true);
        }
        if(event.getEntity() instanceof JujutsuNpc npc&&!npc.level().isClientSide&&npc.isMegumi()&&event.getAmount()>0&&!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)&&NpcCombatRules.lastResort(Math.max(.001f,npc.getHealth()-event.getAmount()),npc.getMaxHealth())){if(npc.becomeTrueSukuna())event.setCanceled(true);}
    }
}
