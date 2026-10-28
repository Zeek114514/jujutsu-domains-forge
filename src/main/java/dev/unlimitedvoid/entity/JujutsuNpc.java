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
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> AWAKENED=net.minecraft.network.syncher.SynchedEntityData.defineId(JujutsuNpc.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(AWAKENED,false);}
    public boolean isAwakened(){return entityData.get(AWAKENED);}
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("Awakened",isAwakened());}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(AWAKENED,tag.contains("Awakened")?tag.getBoolean("Awakened"):isAdvancedType());}
    @Override protected Component getTypeName(){return Component.translatable(isGojo()?(isAwakened()?"entity.unlimitedvoid.shinjuku_gojo":"entity.unlimitedvoid.student_gojo"):(isAwakened()?"entity.unlimitedvoid.true_sukuna":"entity.unlimitedvoid.sukuna_16"));}
    public JujutsuNpc(EntityType<? extends JujutsuNpc> type,Level level){super(type,level);setPersistenceRequired();entityData.set(AWAKENED,isAdvancedType());}
    public boolean isGojo(){return getType()==DomainEntities.STUDENT_GOJO.get()||getType()==DomainEntities.SHINJUKU_GOJO.get();}
    private boolean isAdvancedType(){return getType()==DomainEntities.SHINJUKU_GOJO.get()||getType()==DomainEntities.TRUE_SUKUNA.get();}
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
        var held=player.getItemInHand(hand);
        boolean correct=held.is(isGojo()?DomainItems.BLINDFOLD.get():DomainItems.SUKUNA_FINGER.get());
        if(correct){
            if(!level().isClientSide){
                if(!isAwakened()){
                    entityData.set(AWAKENED,true);
                    if(!player.getAbilities().instabuild)held.shrink(1);
                    player.displayClientMessage(Component.translatable("message.unlimitedvoid.npc_transformed",getName()),true);
                }else player.displayClientMessage(Component.translatable("message.unlimitedvoid.npc_already_transformed"),true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if(held.isEmpty()){
            if(!level().isClientSide)player.displayClientMessage(Component.translatable("message.unlimitedvoid.npc_identity",getName()),true);
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
            event.put(DomainEntities.SHINJUKU_GOJO.get(),attributes);event.put(DomainEntities.TRUE_SUKUNA.get(),attributes);
        }
    }
}
