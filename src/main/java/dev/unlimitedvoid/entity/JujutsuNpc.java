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
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(AWAKENED,false);entityData.define(ASCENDED,false);entityData.define(PUNCH_KIND,-1);entityData.define(PUNCH_START,0L);entityData.define(MEGUMI,false);entityData.define(MAHORAGA_POSE,-100L);}
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> PUNCH_KIND=net.minecraft.network.syncher.SynchedEntityData.defineId(JujutsuNpc.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Long> PUNCH_START=net.minecraft.network.syncher.SynchedEntityData.defineId(JujutsuNpc.class,net.minecraft.network.syncher.EntityDataSerializers.LONG);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> MEGUMI=net.minecraft.network.syncher.SynchedEntityData.defineId(JujutsuNpc.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Long> MAHORAGA_POSE=net.minecraft.network.syncher.SynchedEntityData.defineId(JujutsuNpc.class,net.minecraft.network.syncher.EntityDataSerializers.LONG);
    public boolean summoningMahoraga(){long age=level().getGameTime()-entityData.get(MAHORAGA_POSE);return age>=0&&age<60;}
    public void cancelMahoragaPose(){entityData.set(MAHORAGA_POSE,-100L);}
    public void beginMahoragaSummon(java.util.UUID id){getPersistentData().putUUID("uv_mahoraga_uuid",id);entityData.set(MAHORAGA_POSE,level().getGameTime());cancelPunch();getNavigation().stop();}
    private boolean descendingShrine;
    public void descendingShrine(boolean value){descendingShrine=value;}
    private long domainReadyAt;private int summonDelay;private long nextSummonAt;
    public boolean isMegumi(){return !isGojo()&&entityData.get(MEGUMI)&&!entityData.get(AWAKENED);}
    public boolean isTrueSukuna(){return !isGojo()&&entityData.get(AWAKENED);}
    public boolean becomeTrueSukuna(){if(!isMegumi()||!isAlive()||!rollDangerTransformation())return false;cancelPunch();entityData.set(MEGUMI,false);entityData.set(AWAKENED,true);applyFormVitals(true);return true;}
    public boolean domainReady(){return level().getGameTime()>=domainReadyAt;}
    public void domainUsed(){domainReadyAt=level().getGameTime()+800;}
    public boolean summonReady(){return level().getGameTime()>=nextSummonAt;}
    public void summonAttempted(){nextSummonAt=level().getGameTime()+1200;}
    public void beginStrike(java.util.UUID target){beginPunch(LimitlessKind.BLUE,target);entityData.set(PUNCH_KIND,2);}
    private java.util.UUID punchTarget;private boolean punchLanded;
    public int punchKind(){long age=level().getGameTime()-entityData.get(PUNCH_START);return age>=0&&age<NpcMeleeRules.ANIMATION?entityData.get(PUNCH_KIND):-1;}
    public float punchAge(float partial){return level().getGameTime()-entityData.get(PUNCH_START)+partial;}
    public void beginPunch(LimitlessKind kind,java.util.UUID target){punchTarget=target;punchLanded=false;entityData.set(PUNCH_START,level().getGameTime());entityData.set(PUNCH_KIND,kind==LimitlessKind.RED?1:0);}
    public void cancelPunch(){punchTarget=null;entityData.set(PUNCH_KIND,-1);}
    private void punchTick(){
        long age=level().getGameTime()-entityData.get(PUNCH_START);
        if(age>=NpcMeleeRules.ANIMATION){punchTarget=null;entityData.set(PUNCH_KIND,-1);return;}
        if(!punchLanded&&punchTarget!=null&&age>=NpcMeleeRules.WINDUP){
            punchLanded=true;var target=((net.minecraft.server.level.ServerLevel)level()).getEntity(punchTarget);
            if(target instanceof LivingEntity living){if(isGojo())dev.unlimitedvoid.server.NpcMeleeService.impact(this,living,entityData.get(PUNCH_KIND)==1?LimitlessKind.RED:LimitlessKind.BLUE);else dev.unlimitedvoid.server.SukunaCombatService.impact(this,living);}
        }
    }
    private boolean dangerTransformationRolled;
    public boolean dangerTransformationRolled(){return dangerTransformationRolled;}
    private boolean rollDangerTransformation(){if(level().isClientSide||dangerTransformationRolled)return false;dangerTransformationRolled=true;return getRandom().nextDouble()<DomainConfig.NPC_DANGER_TRANSFORM_CHANCE.get();}
    private boolean awakeningBurstPending;
    private long reversalReadyAt,nextReversalAt;
    public NpcPowerRules.Form form(){return isGojo()?(isShinjuku()?NpcPowerRules.Form.SHINJUKU:isAscended()?NpcPowerRules.Form.AWAKENED:NpcPowerRules.Form.STUDENT):(isTrueSukuna()?NpcPowerRules.Form.TRUE:isMegumi()?NpcPowerRules.Form.MEGUMI:NpcPowerRules.Form.YUJI);}
    public float attackScale(){return NpcPowerRules.attack(form());}
    public float damageTakenScale(){return NpcPowerRules.taken(form());}
    public boolean canReverse(){return !isGojo()||isAwakened();}
    public void applyFormVitals(boolean refill){float before=getHealth(),maximum=getMaxHealth();getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(NpcPowerRules.speed(form()));getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(32);getAttribute(Attributes.MAX_HEALTH).setBaseValue(isMegumi()?NpcVitals.MEGUMI:NpcVitals.maximum(isGojo(),isAscended(),isShinjuku(),isAwakened()));setHealth(refill?getMaxHealth():NpcVitals.preserved(before,maximum,getMaxHealth()));}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){boolean hurt=super.hurt(source,source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)?amount:amount*damageTakenScale());if(hurt&&amount>0){reversalReadyAt=level().getGameTime()+60;nextReversalAt=Math.max(nextReversalAt,reversalReadyAt);}return hurt;}
    private void reverseTick(){
        long now=level().getGameTime();if(!canReverse()||getHealth()>=getMaxHealth()||now<reversalReadyAt||now<nextReversalAt||isGojo()&&NpcCombatRules.lastResort(getHealth(),getMaxHealth())&&(!lastResortUsed||dev.unlimitedvoid.server.NpcTechniqueService.detonating(this)))return;
        heal(getMaxHealth()*.005f);nextReversalAt=now+40;
        if(level() instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1f,.72f,.80f),.7f),getX(),getY()+1.1,getZ(),6,.3,.45,.3,.01);
    }
    public boolean awakeningBurstPending(){return awakeningBurstPending;}
    public void clearAwakeningBurst(){awakeningBurstPending=false;}
    public boolean isAscended(){return isGojo()&&entityData.get(ASCENDED);}
    public boolean isShinjuku(){return isGojo()&&entityData.get(AWAKENED);}
    public boolean awakenFromDanger(){if(!isGojo()||isAwakened()||!isAlive()||!rollDangerTransformation())return false;entityData.set(ASCENDED,true);applyFormVitals(true);awakeningBurstPending=true;return true;}
    private boolean lastResortUsed;
    public boolean lastResortUsed(){return lastResortUsed;}
    public void markLastResort(){lastResortUsed=true;}
    public boolean isAwakened(){return entityData.get(AWAKENED)||isAscended()||isMegumi();}
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("DangerTransformationRolled",dangerTransformationRolled);tag.putBoolean("Megumi",isMegumi());tag.putInt("NpcDomainDelay",(int)Math.max(0,Math.min(800,domainReadyAt-level().getGameTime())));tag.putInt("MahoragaDelay",(int)Math.max(0,Math.min(1200,nextSummonAt-level().getGameTime())));tag.putBoolean("Awakened",entityData.get(AWAKENED));tag.putBoolean("Ascended",isAscended());tag.putBoolean("AwakeningBurstPending",awakeningBurstPending);tag.putBoolean("LastResortUsed",lastResortUsed);tag.putInt("NpcVitalsRevision",1);tag.putInt("ReverseDelay",(int)Math.max(0,Math.min(60,reversalReadyAt-level().getGameTime())));tag.putInt("ReversePulseDelay",(int)Math.max(0,Math.min(40,nextReversalAt-level().getGameTime())));}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);dangerTransformationRolled=tag.getBoolean("DangerTransformationRolled");entityData.set(MEGUMI,!isGojo()&&(tag.contains("Megumi")?tag.getBoolean("Megumi"):getType()==DomainEntities.MEGUMI_SUKUNA.get()));domainReadyAt=level().getGameTime()+Math.max(0,Math.min(800,tag.getInt("NpcDomainDelay")));nextSummonAt=level().getGameTime()+Math.max(0,Math.min(1200,tag.getInt("MahoragaDelay")));lastResortUsed=tag.getBoolean("LastResortUsed");awakeningBurstPending=tag.getBoolean("AwakeningBurstPending");entityData.set(ASCENDED,isGojo()&&(tag.contains("Ascended")?tag.getBoolean("Ascended"):getType()==DomainEntities.AWAKENED_GOJO.get()));entityData.set(AWAKENED,tag.contains("Awakened")?tag.getBoolean("Awakened"):isAdvancedType());
        if(tag.getInt("NpcVitalsRevision")<1){
            float oldMax=40;for(var value:tag.getList("Attributes",10)){var attribute=(net.minecraft.nbt.CompoundTag)value;if(attribute.getString("Name").endsWith("generic.max_health"))oldMax=(float)attribute.getDouble("Base");}
            float oldHealth=tag.contains("Health")?tag.getFloat("Health"):oldMax;applyFormVitals(false);setHealth(NpcVitals.preserved(oldHealth,oldMax,getMaxHealth()));
        }else applyFormVitals(false);
        reversalReadyAt=level().getGameTime()+Math.max(0,Math.min(60,tag.getInt("ReverseDelay")));nextReversalAt=level().getGameTime()+Math.max(0,Math.min(40,tag.getInt("ReversePulseDelay")));
    }
    @Override protected Component getTypeName(){return Component.translatable(isGojo()?(isShinjuku()?"entity.unlimitedvoid.shinjuku_gojo":isAscended()?"entity.unlimitedvoid.awakened_gojo":"entity.unlimitedvoid.student_gojo"):(isTrueSukuna()?"entity.unlimitedvoid.true_sukuna":isMegumi()?"entity.unlimitedvoid.megumi_sukuna":"entity.unlimitedvoid.sukuna_16"));}
    public JujutsuNpc(EntityType<? extends JujutsuNpc> type,Level level){super(type,level);setPersistenceRequired();entityData.set(AWAKENED,isAdvancedType());entityData.set(ASCENDED,getType()==DomainEntities.AWAKENED_GOJO.get());entityData.set(MEGUMI,getType()==DomainEntities.MEGUMI_SUKUNA.get());applyFormVitals(true);}
    public boolean isGojo(){return getType()==DomainEntities.STUDENT_GOJO.get()||getType()==DomainEntities.SHINJUKU_GOJO.get()||getType()==DomainEntities.AWAKENED_GOJO.get();}
    private boolean isAdvancedType(){return getType()==DomainEntities.SHINJUKU_GOJO.get()||getType()==DomainEntities.TRUE_SUKUNA.get();}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new GojoCombatGoal(this));
        goalSelector.addGoal(1,new SukunaCombatGoal(this));
        goalSelector.addGoal(2,new LookAtPlayerGoal(this,Player.class,8,1.0f));
    }
    @Override public void aiStep(){
        super.aiStep();
        if(!level().isClientSide&&!isNoAi()){
            var viewer=getTarget()!=null?getTarget():level().getNearestPlayer(this,8);
            if(viewer!=null){getLookControl().setLookAt(viewer,30,30);if(getNavigation().isDone()&&!descendingShrine){setYRot(getYHeadRot());setYBodyRot(getYHeadRot());}}
            if(isAlive()){punchTick();reverseTick();}
        }
    }
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        var held=player.getItemInHand(hand);
        boolean correct=held.is(isGojo()?DomainItems.BLINDFOLD.get():DomainItems.SUKUNA_FINGER.get());
        if(correct){
            if(!level().isClientSide){
                if(isGojo()?!isShinjuku():!isAwakened()){
                    if(isGojo()){entityData.set(AWAKENED,true);entityData.set(ASCENDED,false);}else entityData.set(MEGUMI,true);applyFormVitals(false);
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
        private static AttributeSupplier attributes(double hp,boolean gojo){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,hp).add(Attributes.MOVEMENT_SPEED,gojo?.34:.32).add(Attributes.FOLLOW_RANGE,32).add(Attributes.KNOCKBACK_RESISTANCE,1).build();}
        @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event){
            event.put(DomainEntities.MAHORAGA.get(),MahoragaEntity.attributes());
            event.put(DomainEntities.STUDENT_GOJO.get(),attributes(NpcVitals.STUDENT,true));event.put(DomainEntities.SUKUNA_16.get(),attributes(NpcVitals.SUKUNA,false));
            event.put(DomainEntities.AWAKENED_GOJO.get(),attributes(NpcVitals.AWAKENED,true));event.put(DomainEntities.SHINJUKU_GOJO.get(),attributes(NpcVitals.SHINJUKU,true));event.put(DomainEntities.TRUE_SUKUNA.get(),attributes(NpcVitals.TRUE_SUKUNA,false));event.put(DomainEntities.MEGUMI_SUKUNA.get(),attributes(NpcVitals.MEGUMI,false));
        }
    }
}
