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

/** Persistent Jujutsu actors. Gojo variants use server-owned combat goals. */
public final class JujutsuNpc extends PathfinderMob {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> AWAKENED=net.minecraft.network.syncher.SynchedEntityData.defineId(JujutsuNpc.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> ASCENDED=net.minecraft.network.syncher.SynchedEntityData.defineId(JujutsuNpc.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(AWAKENED,false);entityData.define(ASCENDED,false);}
    private boolean awakeningBurstPending;
    private long reversalReadyAt,nextReversalAt;
    public boolean canReverse(){return !isGojo()||isAwakened();}
    public void applyFormVitals(boolean refill){float before=getHealth(),maximum=getMaxHealth();getAttribute(Attributes.MAX_HEALTH).setBaseValue(NpcVitals.maximum(isGojo(),isAscended(),isShinjuku(),isAwakened()));setHealth(refill?getMaxHealth():NpcVitals.preserved(before,maximum,getMaxHealth()));}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){boolean hurt=super.hurt(source,amount);if(hurt&&amount>0){reversalReadyAt=level().getGameTime()+60;nextReversalAt=Math.max(nextReversalAt,reversalReadyAt);}return hurt;}
    private void reverseTick(){
        long now=level().getGameTime();if(!canReverse()||getHealth()>=getMaxHealth()||now<reversalReadyAt||now<nextReversalAt||isGojo()&&NpcCombatRules.lastResort(getHealth(),getMaxHealth())&&(!lastResortUsed||dev.unlimitedvoid.server.NpcTechniqueService.detonating(this)))return;
        heal(getMaxHealth()*.005f);nextReversalAt=now+40;
        if(level() instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1f,.72f,.80f),.7f),getX(),getY()+1.1,getZ(),6,.3,.45,.3,.01);
    }
    public boolean awakeningBurstPending(){return awakeningBurstPending;}
    public void clearAwakeningBurst(){awakeningBurstPending=false;}
    public boolean isAscended(){return isGojo()&&entityData.get(ASCENDED);}
    public boolean isShinjuku(){return isGojo()&&entityData.get(AWAKENED);}
    public boolean awakenFromDanger(){if(!isGojo()||isAwakened()||!isAlive())return false;entityData.set(ASCENDED,true);applyFormVitals(true);awakeningBurstPending=true;return true;}
    private boolean lastResortUsed;
    public boolean lastResortUsed(){return lastResortUsed;}
    public void markLastResort(){lastResortUsed=true;}
    public boolean isAwakened(){return entityData.get(AWAKENED)||isAscended();}
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("Awakened",entityData.get(AWAKENED));tag.putBoolean("Ascended",isAscended());tag.putBoolean("AwakeningBurstPending",awakeningBurstPending);tag.putBoolean("LastResortUsed",lastResortUsed);tag.putInt("NpcVitalsRevision",1);tag.putInt("ReverseDelay",(int)Math.max(0,Math.min(60,reversalReadyAt-level().getGameTime())));tag.putInt("ReversePulseDelay",(int)Math.max(0,Math.min(40,nextReversalAt-level().getGameTime())));}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);lastResortUsed=tag.getBoolean("LastResortUsed");awakeningBurstPending=tag.getBoolean("AwakeningBurstPending");entityData.set(ASCENDED,isGojo()&&(tag.contains("Ascended")?tag.getBoolean("Ascended"):getType()==DomainEntities.AWAKENED_GOJO.get()));entityData.set(AWAKENED,tag.contains("Awakened")?tag.getBoolean("Awakened"):isAdvancedType());
        if(tag.getInt("NpcVitalsRevision")<1){
            float oldMax=40;for(var value:tag.getList("Attributes",10)){var attribute=(net.minecraft.nbt.CompoundTag)value;if(attribute.getString("Name").endsWith("generic.max_health"))oldMax=(float)attribute.getDouble("Base");}
            float oldHealth=tag.contains("Health")?tag.getFloat("Health"):oldMax;applyFormVitals(false);setHealth(NpcVitals.preserved(oldHealth,oldMax,getMaxHealth()));
        }else applyFormVitals(false);
        reversalReadyAt=level().getGameTime()+Math.max(0,Math.min(60,tag.getInt("ReverseDelay")));nextReversalAt=level().getGameTime()+Math.max(0,Math.min(40,tag.getInt("ReversePulseDelay")));
    }
    @Override protected Component getTypeName(){return Component.translatable(isGojo()?(isShinjuku()?"entity.unlimitedvoid.shinjuku_gojo":isAscended()?"entity.unlimitedvoid.awakened_gojo":"entity.unlimitedvoid.student_gojo"):(isAwakened()?"entity.unlimitedvoid.true_sukuna":"entity.unlimitedvoid.sukuna_16"));}
    public JujutsuNpc(EntityType<? extends JujutsuNpc> type,Level level){super(type,level);setPersistenceRequired();entityData.set(AWAKENED,isAdvancedType());entityData.set(ASCENDED,getType()==DomainEntities.AWAKENED_GOJO.get());applyFormVitals(true);}
    public boolean isGojo(){return getType()==DomainEntities.STUDENT_GOJO.get()||getType()==DomainEntities.SHINJUKU_GOJO.get()||getType()==DomainEntities.AWAKENED_GOJO.get();}
    private boolean isAdvancedType(){return getType()==DomainEntities.SHINJUKU_GOJO.get()||getType()==DomainEntities.TRUE_SUKUNA.get();}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new GojoCombatGoal(this));
        goalSelector.addGoal(2,new LookAtPlayerGoal(this,Player.class,8,1.0f));
    }
    @Override public void aiStep(){
        super.aiStep();
        if(!level().isClientSide&&!isNoAi()){
            var viewer=getTarget()!=null?getTarget():level().getNearestPlayer(this,8);
            if(viewer!=null){getLookControl().setLookAt(viewer,30,30);setYRot(getYHeadRot());setYBodyRot(getYHeadRot());}
            if(isAlive())reverseTick();
        }
    }
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        var held=player.getItemInHand(hand);
        boolean correct=held.is(isGojo()?DomainItems.BLINDFOLD.get():DomainItems.SUKUNA_FINGER.get());
        if(correct){
            if(!level().isClientSide){
                if(isGojo()?!isShinjuku():!isAwakened()){
                    entityData.set(AWAKENED,true);entityData.set(ASCENDED,false);applyFormVitals(false);
                    if(!player.getAbilities().instabuild)held.shrink(1);
                    player.displayClientMessage(Component.translatable("message.unlimitedvoid.npc_transformed",getName()),true);
                }else player.displayClientMessage(Component.translatable("message.unlimitedvoid.npc_already_transformed"),true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if(held.isEmpty()){
            if(!level().isClientSide)player.displayClientMessage(Component.translatable("message.unlimitedvoid.npc_identity_health",getName(),(int)Math.ceil(getHealth()),(int)Math.ceil(getMaxHealth())),true);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player,hand);
    }
    @Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private static AttributeSupplier attributes(double hp){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,hp).add(Attributes.MOVEMENT_SPEED,.2).add(Attributes.FOLLOW_RANGE,16).add(Attributes.KNOCKBACK_RESISTANCE,1).build();}
        @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event){
            event.put(DomainEntities.STUDENT_GOJO.get(),attributes(NpcVitals.STUDENT));event.put(DomainEntities.SUKUNA_16.get(),attributes(NpcVitals.SUKUNA));
            event.put(DomainEntities.AWAKENED_GOJO.get(),attributes(NpcVitals.AWAKENED));event.put(DomainEntities.SHINJUKU_GOJO.get(),attributes(NpcVitals.SHINJUKU));event.put(DomainEntities.TRUE_SUKUNA.get(),attributes(NpcVitals.TRUE_SUKUNA));
        }
    }
}
