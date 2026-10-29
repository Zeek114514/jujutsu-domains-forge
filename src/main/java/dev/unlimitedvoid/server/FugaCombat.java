package dev.unlimitedvoid.server;

import dev.unlimitedvoid.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.*;

/** One hit per entity per cast; no explosions, chunk loads or permanent terrain writes. */
public final class FugaCombat {
    private static final ResourceKey<DamageType> FIRE=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation(UnlimitedVoid.ID,"fuga"));
    private FugaCombat(){}
    public static int pulse(LivingEntity caster,Vec3 origin,Vec3 impact,float burst,DomainService.FugaDust dust,Set<UUID> struck){
        if(!DomainConfig.FUGA_COMBAT.get()||!FugaCombatRules.hitWindow(burst)||struck.size()>=FugaCombatRules.TARGET_LIMIT)return 0;
        var level=(net.minecraft.server.level.ServerLevel)caster.level();double radius=dust==null?DomainConfig.FUGA_RADIUS.get():dust.radius();
        double front=FugaCombatRules.front(burst,radius);if(front<=0)return 0;
        double maximum=dust==null?DomainConfig.FUGA_DAMAGE.get():DomainConfig.FUGA_DUST_DAMAGE.get();
        var source=new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FIRE),caster,caster);
        var box=new AABB(impact.subtract(front,front,front),impact.add(front,front,front));
        var candidates=level.getEntitiesOfClass(LivingEntity.class,box,t->DomainService.fugaEligible(t,caster)&&!struck.contains(t.getUUID()));
        candidates.sort(Comparator.comparingDouble(t->t.distanceToSqr(impact)));
        int count=0;
        int offset=candidates.isEmpty()?0:Math.floorMod(((int)burst/4)*FugaCombatRules.SCAN_BUDGET,candidates.size());
        for(int scanned=0;scanned<Math.min(candidates.size(),FugaCombatRules.SCAN_BUDGET);scanned++){
            var target=candidates.get((offset+scanned)%candidates.size());
            if(count>=FugaCombatRules.HIT_BUDGET||struck.size()>=FugaCombatRules.TARGET_LIMIT)break;
            Vec3 point=target.getBoundingBox().getCenter();double distance=point.distanceTo(impact);
            if(distance>front || DomainService.fugaSheltered(level,origin,point))continue;
            if(dust!=null && (point.distanceToSqr(dust.center())>dust.radius()*dust.radius()||DomainService.fugaContested(dust.owner(),point)))continue;
            if(dust==null && level.clip(new ClipContext(impact.add(0,.25,0),target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,caster)).getType()!=HitResult.Type.MISS)continue;
            struck.add(target.getUUID());count++;
            float amount=FugaCombatRules.damage(distance,radius,maximum)*(caster instanceof dev.unlimitedvoid.entity.JujutsuNpc n?n.attackScale():1);
            if(amount>0&&target.hurt(source,amount)&&target.isAlive())target.setSecondsOnFire(dust==null?4:8);
        }
        return count;
    }
}
