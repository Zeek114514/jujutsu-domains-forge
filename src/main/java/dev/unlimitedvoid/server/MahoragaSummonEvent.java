package dev.unlimitedvoid.server;
import dev.unlimitedvoid.entity.JujutsuNpc;
import net.minecraft.world.entity.LivingEntity;
/** Extension point only: a future Mahoraga implementation must create its actual entity. */
public final class MahoragaSummonEvent extends net.minecraftforge.eventbus.api.Event {
    private final JujutsuNpc caster;private final LivingEntity target;private boolean summoned;
    public MahoragaSummonEvent(JujutsuNpc caster,LivingEntity target){this.caster=caster;this.target=target;}
    public JujutsuNpc caster(){return caster;}public LivingEntity target(){return target;}
    public void markSummoned(){summoned=true;}public boolean summoned(){return summoned;}
}
