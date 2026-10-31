package dev.unlimitedvoid.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A persistent, peaceful photo-skinned display character. */
public final class InnocentSunDog extends PathfinderMob {
    public static final int KICK_CONTACT=12, KICK_DURATION=30;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Long> KICK_START=net.minecraft.network.syncher.SynchedEntityData.defineId(InnocentSunDog.class,net.minecraft.network.syncher.EntityDataSerializers.LONG);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Long> RECOIL_START=net.minecraft.network.syncher.SynchedEntityData.defineId(InnocentSunDog.class,net.minecraft.network.syncher.EntityDataSerializers.LONG);
    long nextPrankAt;
    boolean prankActive;
    public static boolean shouldPrank(float roll){return roll>=0&&roll<.20f;}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(KICK_START,-1000L);entityData.define(RECOIL_START,-1000L);}
    public boolean isSunDog(){return getType()==dev.unlimitedvoid.DomainEntities.INNOCENT_SUN_DOG.get();}
    public float kickAge(float partial){return level().getGameTime()-entityData.get(KICK_START)+partial;}
    public float recoilAge(float partial){return level().getGameTime()-entityData.get(RECOIL_START)+partial;}
    void beginKick(){entityData.set(KICK_START,level().getGameTime());nextPrankAt=level().getGameTime()+160;}
    void recoil(){entityData.set(RECOIL_START,level().getGameTime());}
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("PrankDelay",(int)Math.max(0,Math.min(160,nextPrankAt-level().getGameTime())));}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);nextPrankAt=level().getGameTime()+Math.max(0,Math.min(160,tag.getInt("PrankDelay")));}
    public InnocentSunDog(EntityType<? extends InnocentSunDog> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.MOVEMENT_SPEED, .25).add(Attributes.FOLLOW_RANGE, 16).build();
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SunDogPrankGoal(this));
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.Goal(){
            {setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
            @Override public boolean canUse(){return !isSunDog()&&getLeashHolder() instanceof InnocentSunDog holder&&holder.isSunDog();}
            @Override public void tick(){
                if(!(getLeashHolder() instanceof InnocentSunDog holder)||!holder.isAlive()){getNavigation().stop();return;}
                if(holder.prankActive){getNavigation().stop();setYHeadRot(yBodyRot);}
                else if(distanceToSqr(holder)>4){getNavigation().moveTo(holder,1.1);getLookControl().setLookAt(holder,30,30);}
                else getNavigation().stop();
            }
        });
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, .8));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }
}
