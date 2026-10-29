package dev.unlimitedvoid.entity;
import dev.unlimitedvoid.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.tags.DamageTypeTags;
import dev.unlimitedvoid.MahoragaRules.Kind;
import dev.unlimitedvoid.server.*;
import java.util.*;
/** Dedicated giant cuboid actor; dimensions include the body, while the wheel floats overhead. */
public final class MahoragaEntity extends PathfinderMob {
    private static final EntityDataAccessor<Long> GROWTH_COUNT=SynchedEntityData.defineId(MahoragaEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> WHEEL=SynchedEntityData.defineId(MahoragaEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> TURN=SynchedEntityData.defineId(MahoragaEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> EMERGE=SynchedEntityData.defineId(MahoragaEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> ADAPT=SynchedEntityData.defineId(MahoragaEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK=SynchedEntityData.defineId(MahoragaEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ATTACK_START=SynchedEntityData.defineId(MahoragaEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DISMISS=SynchedEntityData.defineId(MahoragaEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(MahoragaEntity.class,EntityDataSerializers.OPTIONAL_UUID);
    private final int[] exposures=new int[8];private final long[] first=new long[8],last=new long[8],advanced=new long[8];
    private Kind forcedKind;private UUID attackTarget;private boolean attackLanded;private long recovery,spaceReady,leapReady,lastDamage;private int missingOwner;private long lastGlobalTurn=-100;
    private float growth=1,previousGrowth=1,growthTarget=1;private int growthTicks;
    public float growth(float partial){return previousGrowth+(growth-previousGrowth)*Math.max(0,Math.min(1,partial));}
    public long adaptationCount(){return entityData.get(GROWTH_COUNT);}
    private void growthTick(){previousGrowth=growth;float target=MahoragaRules.growth(adaptationCount());if(target!=growthTarget){growthTarget=target;growthTicks=20;}if(growthTicks>0){growth+=(growthTarget-growth)/growthTicks--;refreshDimensions();}}
    @Override public EntityDimensions getDimensions(Pose pose){return getType().getDimensions().scale(Math.max(1,growth));}
    public int adaptation(Kind kind){return MahoragaRules.stage(entityData.get(ADAPT),kind);}
    public Optional<UUID> ownerId(){return entityData.get(OWNER);}
    @Override public net.minecraft.world.scores.Team getTeam(){var owner=owner();return owner==null?super.getTeam():owner.getTeam();}
    public JujutsuNpc owner(){if(level() instanceof ServerLevel server&&ownerId().isPresent()&&server.getEntity(ownerId().get()) instanceof JujutsuNpc n)return n;return null;}
    public void bind(JujutsuNpc owner){entityData.set(OWNER,Optional.of(owner.getUUID()));beginEmergence();}
    public boolean emerging(){return entityData.get(EMERGE)>=0&&level().getGameTime()-entityData.get(EMERGE)<60;}
    public boolean dismissing(){return entityData.get(DISMISS)>=0;}
    public void dismiss(){if(!dismissing()){entityData.set(DISMISS,level().getGameTime());getNavigation().stop();attackTarget=null;entityData.set(ATTACK,0);}}
    public boolean canAct(){var owner=owner();return isAlive()&&!isNoAi()&&!emerging()&&!dismissing()&&!DomainService.isCaptive(getUUID())&&(ownerId().isEmpty()||owner!=null&&owner.isAlive()&&!owner.isNoAi());}
    public boolean enemy(LivingEntity target){var owner=owner();return target!=this&&target.isAlive()&&!isAlliedTo(target)&&(owner==null||!owner.isAlliedTo(target))&&(target instanceof net.minecraft.world.entity.monster.Enemy||target instanceof JujutsuNpc n&&n.isGojo());}
    public double forceFactor(Kind kind){return MahoragaRules.forceFactor(adaptation(kind));}
    public boolean hurtAs(Kind kind,DamageSource source,float amount){Kind before=forcedKind;forcedKind=kind;try{return hurt(source,amount);}finally{forcedKind=before;}}
    private Kind kind(DamageSource source){
        if(forcedKind!=null)return forcedKind;
        String id=source.typeHolder().unwrapKey().map(k->k.location().getPath()).orElse("");
        if(id.equals("blue"))return Kind.BLUE;if(id.equals("red"))return Kind.RED;if(id.equals("purple"))return Kind.PURPLE;
        if(id.equals("cleave")||id.equals("dismantle"))return Kind.SLASH;
        if(id.equals("fuga")||source.is(DamageTypeTags.IS_FIRE))return Kind.FIRE;
        if(DomainService.isCaptive(getUUID())&&id.equals("indirect_magic"))return Kind.VOID;
        return Kind.PHYSICAL;
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(level().isClientSide)return super.hurt(source,amount);
        if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return super.hurt(source,amount);
        Kind kind=kind(source);boolean result=super.hurt(source,amount*MahoragaRules.damageFactor(adaptation(kind)));
        if(result&&amount>0&&isAlive()){lastDamage=level().getGameTime();observe(kind);}return result;
    }
    public void observe(Kind kind){if(level().isClientSide||!isAlive()||dismissing())return;int i=kind.ordinal();long now=level().getGameTime();if(exposures[i]>0&&now-last[i]<5)return;if(exposures[i]==0){first[i]=now;advanced[i]=now-40;}exposures[i]=Math.min(64,exposures[i]+1);last[i]=now;}
    private void adapt(){long now=level().getGameTime();if(now-lastGlobalTurn<20)return;for(var kind:Kind.values()){int i=kind.ordinal(),stage=adaptation(kind);if(exposures[i]>0&&(stage<4?MahoragaRules.advance(kind,stage,exposures[i],now-first[i],now-advanced[i]):exposures[i]>=15&&now-advanced[i]>=(kind==Kind.PURPLE?160:60))){entityData.set(ADAPT,MahoragaRules.setStage(entityData.get(ADAPT),kind,Math.min(4,stage+1)));if(stage>=3)exposures[i]=12;advanced[i]=now;lastGlobalTurn=now;entityData.set(GROWTH_COUNT,adaptationCount()+1);turnWheel();heal(80);if(level() instanceof ServerLevel server){server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.8f,.65f,.3f),.8f),getX(),getY()+3.5,getZ(),12,.7,.08,.7,.02);server.playSound(null,blockPosition(),net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(UnlimitedVoid.ID,"mahoraga_turn")),net.minecraft.sounds.SoundSource.HOSTILE,.65f,1);}break;}}}
    @Override public void tick(){
        super.tick();growthTick();if(level().isClientSide||!isAlive())return;long now=level().getGameTime();
        if(ownerId().isPresent()){var owner=owner();if(owner==null){if(++missingOwner>200)dismiss();}else{missingOwner=0;if(!owner.isAlive()||owner.distanceToSqr(this)>96*96)dismiss();}}
        if(dismissing()){if(now-entityData.get(DISMISS)>=30)discard();return;}
        if(DomainService.isCaptive(getUUID())&&now%20==0)observe(Kind.VOID);adapt();
        if(!isNoAi()&&!emerging()&&now-lastDamage>=60&&now%40==0&&getHealth()<getMaxHealth())heal(5);
        if(attackType()>0){long age=now-entityData.get(ATTACK_START);int hit=MahoragaAttackMotion.contact(attackType());if(!attackLanded&&age>=hit){attackLanded=true;impact();}if(age>=MahoragaAttackMotion.DURATION){entityData.set(ATTACK,0);attackTarget=null;}}
    }
    public boolean startAttack(LivingEntity target){
        if(!canAct()||!enemy(target)||!hasLineOfSight(target)||attackType()>0||level().getGameTime()<recovery)return false;
        long now=level().getGameTime();double distance=distanceToSqr(target);int type;
        if(adaptation(Kind.INFINITY)>=4&&now>=spaceReady&&distance<24*24&&getRandom().nextInt(100)<15){type=4;spaceReady=now+240;}
        else if(distance>6*6&&distance<14*14&&onGround()&&now>=leapReady){type=3;leapReady=now+200;Vec3 delta=target.position().subtract(position()).normalize();setDeltaMovement(delta.scale(.85).add(0,.65,0));hurtMarked=true;}
        else if(distance<=4.6*4.6*growth*growth)type=getRandom().nextBoolean()?1:2;else return false;
        entityData.set(ATTACK,type);entityData.set(ATTACK_START,now);attackTarget=target.getUUID();attackLanded=false;recovery=now+36;getNavigation().stop();return true;
    }
    private void impact(){
        if(!canAct()||!(level() instanceof ServerLevel server)||attackTarget==null||!(server.getEntity(attackTarget) instanceof LivingEntity target)||!enemy(target)||!hasLineOfSight(target))return;
        if(attackType()==4){MahoragaService.spaceSlash(this,target);return;}
        double reach=(attackType()==1?4.6:5.3)*growth;if(distanceToSqr(target)>reach*reach)return;
        swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);float damage=attackType()==1?38:52;
        int count=0;for(var enemy:server.getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(attackType()==1?.3:1.8),this::enemy)){if(count++>=8)break;if(!hasLineOfSight(enemy)||distanceToSqr(enemy)>reach*reach)continue;if(enemy.hurt(damageSources().mobAttack(this),damage))enemy.knockback(1.2,getX()-enemy.getX(),getZ()-enemy.getZ());}
        if(attackType()!=1){Vec3 p=target.getBoundingBox().getCenter(),side=getLookAngle().cross(new Vec3(0,1,0)).normalize().scale(3);dev.unlimitedvoid.network.DomainNetwork.npcSlash(server,getUUID(),p.subtract(side).add(0,-2,0),p.add(side).add(0,2,0),false);}
    }
    public MahoragaEntity(EntityType<? extends MahoragaEntity> type,Level level){super(type,level);setPersistenceRequired();setMaxUpStep(1);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(GROWTH_COUNT,0L);entityData.define(WHEEL,0);entityData.define(TURN,-100L);entityData.define(EMERGE,-1L);entityData.define(ADAPT,0);entityData.define(ATTACK,0);entityData.define(ATTACK_START,0L);entityData.define(DISMISS,-1L);entityData.define(OWNER,Optional.empty());}
    public static AttributeSupplier attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,1000).add(Attributes.MOVEMENT_SPEED,.32).add(Attributes.FOLLOW_RANGE,48).add(Attributes.ARMOR,8).add(Attributes.KNOCKBACK_RESISTANCE,.85).build();}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new MahoragaCombatGoal(this));goalSelector.addGoal(3,new FollowSummoner(this));goalSelector.addGoal(5,new LookAtPlayerGoal(this,net.minecraft.world.entity.player.Player.class,12));}
    public float emergence(float partial){long start=entityData.get(EMERGE);float progress=start<0?1:DomainTimeline.smooth((level().getGameTime()+partial-start)/60f);long end=entityData.get(DISMISS);return end<0?progress:progress*(1-DomainTimeline.smooth((level().getGameTime()+partial-end)/30f));}
    public void beginEmergence(){entityData.set(EMERGE,level().getGameTime());}
    public void turnWheel(){entityData.set(WHEEL,entityData.get(WHEEL)+1);entityData.set(TURN,level().getGameTime());}
    public float wheelAngle(float partial){float step=entityData.get(WHEEL),p=DomainTimeline.smooth((level().getGameTime()+partial-entityData.get(TURN))/14f);return (step-(step>0?1-p:0))*(float)Math.PI/4;}
    public int attackType(){return entityData.get(ATTACK);}public float attackAge(float partial){return level().getGameTime()-entityData.get(ATTACK_START)+partial;}public float attackProgress(float partial){return attackType()==0?0:Math.max(0,Math.min(1,(level().getGameTime()+partial-entityData.get(ATTACK_START))/30f));}
    private static final class FollowSummoner extends net.minecraft.world.entity.ai.goal.Goal {
        private final MahoragaEntity actor;FollowSummoner(MahoragaEntity actor){this.actor=actor;setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return actor.canAct()&&actor.owner()!=null&&actor.distanceToSqr(actor.owner())>6*6;}
        @Override public boolean canContinueToUse(){return actor.canAct()&&actor.owner()!=null&&actor.distanceToSqr(actor.owner())>4*4;}
        @Override public void tick(){var owner=actor.owner();if(owner!=null){actor.getNavigation().moveTo(owner,1.2);actor.getLookControl().setLookAt(owner,20,20);}}
        @Override public void stop(){actor.getNavigation().stop();}
    }
    @Override protected net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        if(player.getItemInHand(hand).isEmpty()){if(!level().isClientSide){var states=net.minecraft.network.chat.Component.empty();boolean any=false;for(var kind:Kind.values())if(adaptation(kind)>0){if(any)states.append("、");states.append(net.minecraft.network.chat.Component.translatable("adaptation.unlimitedvoid."+kind.name().toLowerCase(java.util.Locale.ROOT))).append(" "+adaptation(kind)+"/4");any=true;}if(!any)states.append(net.minecraft.network.chat.Component.translatable("adaptation.unlimitedvoid.none"));player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.unlimitedvoid.mahoraga_status",getName(),(int)Math.ceil(getHealth()),(int)Math.ceil(getMaxHealth()),states),true);}return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);}return super.mobInteract(player,hand);
    }
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size){return 3.5f*Math.max(1,growth);}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putLong("GrowthCount",adaptationCount());tag.putInt("WheelTurns",entityData.get(WHEEL));ownerId().ifPresent(id->tag.putUUID("Summoner",id));tag.putInt("Adaptation",entityData.get(ADAPT));long now=level().getGameTime();for(var k:Kind.values()){int i=k.ordinal();var data=new net.minecraft.nbt.CompoundTag();data.putInt("Exposures",exposures[i]);data.putLong("Elapsed",Math.max(0,Math.min(1000000,now-first[i])));data.putLong("SinceStep",Math.max(0,Math.min(1000000,now-advanced[i])));tag.put("Adapt_"+k.name(),data);}tag.putInt("EmergenceAge",entityData.get(EMERGE)<0?60:(int)Math.max(0,Math.min(60,now-entityData.get(EMERGE))));}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(WHEEL,Math.max(0,tag.getInt("WheelTurns")));entityData.set(EMERGE,-1L);if(tag.hasUUID("Summoner"))entityData.set(OWNER,Optional.of(tag.getUUID("Summoner")));int packed=0;long now=level().getGameTime();for(var k:Kind.values()){int i=k.ordinal();packed=MahoragaRules.setStage(packed,k,MahoragaRules.stage(tag.getInt("Adaptation"),k));var data=tag.getCompound("Adapt_"+k.name());exposures[i]=Math.max(MahoragaRules.stage(packed,k)==4?12:0,Math.min(64,data.getInt("Exposures")));first[i]=now-Math.max(0,Math.min(1000000,data.getLong("Elapsed")));advanced[i]=now-Math.max(0,Math.min(1000000,data.getLong("SinceStep")));last[i]=now-5;}entityData.set(ADAPT,packed);entityData.set(GROWTH_COUNT,Math.max(MahoragaRules.adaptations(packed),tag.getLong("GrowthCount")));growth=previousGrowth=growthTarget=MahoragaRules.growth(adaptationCount());growthTicks=0;refreshDimensions();int emergence=Math.max(0,Math.min(60,tag.contains("EmergenceAge")?tag.getInt("EmergenceAge"):60));if(emergence<60)entityData.set(EMERGE,now-emergence);}
}
