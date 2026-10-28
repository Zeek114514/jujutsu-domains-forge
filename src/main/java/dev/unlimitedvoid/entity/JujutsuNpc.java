package dev.unlimitedvoid.entity;

import dev.unlimitedvoid.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Persistent display actors. No attack, target, wander or automatic technique goals. */
public final class JujutsuNpc extends PathfinderMob {
    public JujutsuNpc(EntityType<? extends JujutsuNpc> type,Level level){super(type,level);setPersistenceRequired();}
    public boolean isGojo(){return getType()==DomainEntities.STUDENT_GOJO.get();}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new LookAtPlayerGoal(this,Player.class,8,1.0f));
    }
    @Override public void aiStep(){
        super.aiStep();
        if(!level().isClientSide&&!isNoAi()){
            var viewer=level().getNearestPlayer(this,8);
            if(viewer!=null){getLookControl().setLookAt(viewer,30,30);setYRot(getYHeadRot());setYBodyRot(getYHeadRot());}
        }
    }
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(player.getItemInHand(hand).isEmpty()){
            if(!level().isClientSide)player.displayClientMessage(Component.translatable(isGojo()?"message.unlimitedvoid.student_gojo":"message.unlimitedvoid.sukuna_16"),true);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player,hand);
    }
    @Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event){
            var attributes=Mob.createMobAttributes().add(Attributes.MAX_HEALTH,40).add(Attributes.MOVEMENT_SPEED,.2)
                .add(Attributes.FOLLOW_RANGE,16).add(Attributes.KNOCKBACK_RESISTANCE,1).build();
            event.put(DomainEntities.STUDENT_GOJO.get(),attributes);event.put(DomainEntities.SUKUNA_16.get(),attributes);
        }
    }
}
