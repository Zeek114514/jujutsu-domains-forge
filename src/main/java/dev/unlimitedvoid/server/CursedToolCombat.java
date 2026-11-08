package dev.unlimitedvoid.server;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.JujutsuNpc;
import dev.unlimitedvoid.item.CursedToolItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID)
public final class CursedToolCombat {
    public static final ResourceKey<DamageType> SOUL_CUT=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation(UnlimitedVoid.ID,"soul_cut"));
    private static final String NULLIFIED="uv_tool_nullify_until";
    public static boolean nullified(LivingEntity entity){return entity.getPersistentData().getLong(NULLIFIED)>entity.level().getGameTime();}
    public static boolean spearContact(DamageSource source,LivingEntity target){
        return source.is(DamageTypes.PLAYER_ATTACK)&&source.getEntity() instanceof Player attacker&&source.getDirectEntity()==attacker&&attacker.distanceToSqr(target)<=4.5*4.5&&attacker.getMainHandItem().getItem() instanceof CursedToolItem tool&&tool.kind==CursedToolItem.Kind.SPEAR;
    }
    public static DamageSource meleeSource(Player player,DamageSource original){
        if(player.getMainHandItem().getItem() instanceof CursedToolItem tool&&tool.kind==CursedToolItem.Kind.SOUL&&player.hasEffect(CursedToolEffects.SOUL_SIGHT.get()))return new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SOUL_CUT),player);
        return original;
    }
    private static boolean ready(LivingEntity attacker){return !(attacker instanceof Player p)||p.getAttackStrengthScale(.5f)>=.9f;}
    public static float physicalPower(LivingEntity attacker){
        var strength=attacker.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST);
        return 1+.5f*(strength==null?0:Math.min(4,strength.getAmplifier()+1));
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event){
        var source=event.getSource();
        if(event.getEntity().level().isClientSide||!source.is(DamageTypes.PLAYER_ATTACK)||!(source.getEntity() instanceof Player attacker)||source.getDirectEntity()!=attacker||!ready(attacker))return;
        ItemStack stack=attacker.getMainHandItem();
        if(stack.getItem() instanceof CursedToolItem tool&&tool.kind==CursedToolItem.Kind.CLOUD)event.setAmount(event.getAmount()*physicalPower(attacker));
    }
    public static void landed(CursedToolItem tool,ItemStack stack,LivingEntity attacker,LivingEntity target){
        long now=target.level().getGameTime();boolean finisher=false;
        if(tool.kind==CursedToolItem.Kind.SPEAR){
            target.getPersistentData().putLong(NULLIFIED,now+40);
            FugaService.cancel(target.getUUID());LimitlessService.nullify(target.getUUID());DomainService.nullify(target.getUUID());
            if(target instanceof JujutsuNpc npc){NpcTechniqueService.cancel(npc);npc.cancelPunch();}
        }else if(tool.kind==CursedToolItem.Kind.CLOUD&&ready(attacker)){
            finisher=true;
            if(finisher){
                target.knockback(.4*physicalPower(attacker),attacker.getX()-target.getX(),attacker.getZ()-target.getZ());
                if(target instanceof JujutsuNpc npc){npc.stagger(true);var away=target.position().subtract(attacker.position()).multiply(1,0,1).normalize();target.setDeltaMovement(target.getDeltaMovement().add(away.scale(.4)).add(0,.12,0));target.hurtMarked=true;}
            }
        }
        if(target.level() instanceof ServerLevel level){
            var p=target.getBoundingBox().getCenter();level.sendParticles(tool.kind==CursedToolItem.Kind.SOUL?ParticleTypes.SOUL:ParticleTypes.CRIT,p.x,p.y,p.z,finisher?14:6,.18,.3,.18,.04);
            if(finisher)level.sendParticles(ParticleTypes.SWEEP_ATTACK,p.x,p.y,p.z,1,0,0,0,0);
            level.playSound(null,p.x,p.y,p.z,DomainSounds.SLASH_HIT.get(),SoundSource.PLAYERS,DomainSounds.gain(finisher?.65f:.28f),tool.kind==CursedToolItem.Kind.CLOUD?.7f:1.2f);
        }
    }
}
