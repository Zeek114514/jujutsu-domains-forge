package dev.unlimitedvoid.server;

import dev.unlimitedvoid.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;

/** Game adaptation of cursed energy, Cleave and Dismantle. */
public final class ShrineRules {
    public static final ResourceKey<DamageType> CLEAVE=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation(UnlimitedVoid.ID,"cleave"));
    public static final ResourceKey<DamageType> DISMANTLE=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation(UnlimitedVoid.ID,"dismantle"));
    public static double energy(Entity target) {
        if(target.getPersistentData().contains("uv_cursed_energy")) {
            double value=target.getPersistentData().getDouble("uv_cursed_energy");
            return Double.isFinite(value) ? Math.max(0,Math.min(1000000,value)) : 0;
        }
        return target instanceof LivingEntity && !(target instanceof ArmorStand) ? DomainConfig.LIVING_ENERGY.get() : 0;
    }
    public static double power(LivingEntity caster) {
        return caster.getPersistentData().contains("uv_cursed_energy") ? Math.max(1,energy(caster)) : DomainConfig.CASTER_ENERGY.get();
    }
    public static float cleaveDamage(LivingEntity target,double power) {
        if(target instanceof dev.unlimitedvoid.entity.MahoragaEntity)return (float)Math.max(12,Math.min(96,48*power/100));
        double energy=Math.max(1,energy(target));
        if(power>=energy) return target.getMaxHealth()+target.getAbsorptionAmount()+4;
        return (float)Math.max(2,(target.getMaxHealth()+target.getAbsorptionAmount())*power/energy*0.35);
    }
    public static DamageSource source(LivingEntity caster,boolean cleave) {
        var holder=caster.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(cleave?CLEAVE:DISMANTLE);
        return new DamageSource(holder,caster,caster);
    }
    public static boolean nonlivingTarget(Entity entity) {
        return entity.isAlive() && (entity instanceof ItemEntity || entity instanceof ArmorStand || entity instanceof HangingEntity
                || entity instanceof Boat || entity instanceof AbstractMinecart);
    }
    public static void dismantle(Entity entity,ServerPlayer caster) {
        double energy=energy(entity),power=power(caster);
        boolean cleave=energy>0;
        if((cleave && power>=energy) || (!cleave && (entity instanceof ItemEntity || entity instanceof ArmorStand))) entity.discard();
        else entity.hurt(source(caster,cleave),cleave ? (float)Math.max(0.2,12*power/energy) : 12);
    }
    /** Bounded random slice samples. Never scans a 200-block sphere in one tick or loads terrain. */
    public static int cutTerrain(ServerLevel level,ServerPlayer caster,Vec3 center,double radius,int age) {
        return cutTerrain(level,caster,center,radius,age,point->DomainService.terrainCutAllowed(level,point));
    }
    public static int cutTerrain(ServerLevel level,ServerPlayer caster,Vec3 center,double radius,int age,java.util.function.Predicate<Vec3> allowed) {
        if(!DomainConfig.TERRAIN_DAMAGE.get()) return 0;
        int removed=0,budget=DomainConfig.BLOCK_BUDGET.get();
        RandomSource random=RandomSource.create(caster.getUUID().getLeastSignificantBits()^((long)age*0x9E3779B9L));
        BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        for(int i=0;i<4096 && removed<budget;i++) {
            double x=(random.nextDouble()*2-1)*radius,z=(random.nextDouble()*2-1)*radius;
            // Most cuts sweep the ground surface; periodic vertical planes reach the full volume.
            double y=i%4==0 ? (random.nextDouble()*2-1)*radius : random.nextInt(16)-5;
            if(x*x+y*y+z*z>radius*radius) continue;
            pos.set(center.x+x,center.y+y,center.z+z);
            if(!allowed.test(Vec3.atCenterOf(pos)))continue;
            if(pos.getY()<level.getMinBuildHeight() || pos.getY()>=level.getMaxBuildHeight() || !level.hasChunkAt(pos)) continue;
            var state=level.getBlockState(pos);
            if(state.isAir() || !state.getFluidState().isEmpty() || state.getDestroySpeed(level,pos)<0) continue;
            if(ForgeHooks.onBlockBreakEvent(level,caster.gameMode.getGameModeForPlayer(),caster,pos)<0) continue;
            if(level.destroyBlock(pos,false,caster)) removed++;
        }
        return removed;
    }
}
