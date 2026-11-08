package dev.unlimitedvoid.mixin;
import dev.unlimitedvoid.server.CursedToolCombat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Keep vanilla cooldown, criticals and durability; change only the primary strike's damage type. */
@Mixin(Player.class)
public abstract class CursedToolDamageMixin {
    @Group(name="unlimitedvoid$soulSource",min=1,max=1)
    @ModifyArg(method={"attack","m_5706_"},at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),index=0,require=0,remap=false)
    private DamageSource unlimitedvoid$soulNamed(DamageSource source){return CursedToolCombat.meleeSource((Player)(Object)this,source);}
    @Group(name="unlimitedvoid$soulSource",min=1,max=1)
    @ModifyArg(method={"attack","m_5706_"},at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),index=0,require=0,remap=false)
    private DamageSource unlimitedvoid$soulProduction(DamageSource source){return CursedToolCombat.meleeSource((Player)(Object)this,source);}
}
