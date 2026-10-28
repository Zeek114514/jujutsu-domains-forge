package dev.unlimitedvoid.server;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
/** Real persistent summon, one per summoner lifetime. Never generates chunks or terrain. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID)
public final class MahoragaService {
    @SubscribeEvent public static void summon(MahoragaSummonEvent event){
        var owner=event.caster();if(!(owner.level() instanceof ServerLevel level)||!owner.isAlive()||owner.isNoAi()||!owner.isMegumi()||owner.getPersistentData().hasUUID("uv_mahoraga_uuid")||DomainService.isCaptive(owner.getUUID()))return;
        if(level.getEntitiesOfClass(MahoragaEntity.class,owner.getBoundingBox().inflate(128),m->m.ownerId().isPresent()).size()>=8)return;
        var summon=DomainEntities.MAHORAGA.get().create(level);Vec3 forward=new Vec3(owner.getLookAngle().x,0,owner.getLookAngle().z).normalize();if(forward.lengthSqr()<.5)forward=new Vec3(0,0,1);Vec3 side=new Vec3(forward.z,0,-forward.x);
        for(double radius:new double[]{3.5,6,12})for(var direction:new Vec3[]{forward.scale(-1),side,side.scale(-1),forward}){
            Vec3 candidate=owner.position().add(direction.scale(radius));if(!level.hasChunkAt(BlockPos.containing(candidate)))continue;
            var ground=ShrinePlacement.center(level,candidate.add(0,1,0));if(ground.isEmpty())continue;Vec3 feet=ground.get().add(0,-1,0);
            if(!level.getFluidState(BlockPos.containing(feet.add(0,.1,0))).isEmpty())continue;
            summon.moveTo(feet.x,feet.y,feet.z,owner.getYRot(),0);if(!level.noCollision(summon,summon.getBoundingBox()))continue;
            summon.bind(owner);if(!level.addFreshEntity(summon))return;owner.beginMahoragaSummon(summon.getUUID());event.markSummoned();return;
        }
    }
    @SubscribeEvent public static void ownerLeaves(EntityLeaveLevelEvent event){if(event.getEntity() instanceof JujutsuNpc owner&&event.getLevel() instanceof ServerLevel level&&owner.getPersistentData().hasUUID("uv_mahoraga_uuid")&&level.getEntity(owner.getPersistentData().getUUID("uv_mahoraga_uuid")) instanceof MahoragaEntity summon)summon.dismiss();}
    public static void spaceSlash(MahoragaEntity caster,LivingEntity target){
        if(!caster.canAct()||caster.adaptation(MahoragaRules.Kind.INFINITY)<4||!caster.enemy(target)||!(caster.level() instanceof ServerLevel level))return;
        Vec3 from=caster.getEyePosition(),direction=target.getBoundingBox().getCenter().subtract(from).normalize();if(direction.lengthSqr()<.9)return;
        Vec3 to=from;for(double d=.5;d<=24;d+=.5){Vec3 point=from.add(direction.scale(d));if(!level.hasChunkAt(BlockPos.containing(point)))break;to=point;}
        to=level.clip(new net.minecraft.world.level.ClipContext(from,to,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,caster)).getLocation();
        dev.unlimitedvoid.network.DomainNetwork.npcSlash(level,caster.getUUID(),from,to,true);int count=0;
        for(var enemy:level.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(1.8),caster::enemy)){if(count++>=16)break;if(PurpleDetonation.closest(from,to,enemy.getBoundingBox().getCenter()).distanceToSqr(enemy.getBoundingBox().getCenter())<1.8*1.8&&caster.hasLineOfSight(enemy))enemy.hurt(ShrineRules.source(caster,true),92);}
    }
    public static double force(LivingEntity entity,MahoragaRules.Kind kind){return entity instanceof MahoragaEntity mahoraga?mahoraga.forceFactor(kind):1;}
    private MahoragaService(){}
}
