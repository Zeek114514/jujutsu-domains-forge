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
        goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, .8));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }
}
