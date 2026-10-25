package dev.unlimitedvoid.mixin;

import dev.unlimitedvoid.entity.ShrineEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.*;

/** Add exact shrine shapes at the common movement solver on both logical sides. */
@Mixin(Entity.class)
public abstract class ShrineCollisionMixin {
    @ModifyVariable(method={"collideBoundingBox","m_198894_"},at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false)
    private static List<VoxelShape> unlimitedvoid$shrineCollisions(List<VoxelShape> original,
            Entity mover,Vec3 movement,AABB box,Level level,List<VoxelShape> targetShapes){
        if(mover instanceof ShrineEntity || mover!=null && mover.isSpectator())return original;
        AABB area=box.expandTowards(movement).inflate(0.000001);
        List<Entity> shrines=level.getEntities(mover,area,e->e instanceof ShrineEntity && e.isAlive());
        if(shrines.isEmpty())return original;
        List<VoxelShape> shapes=new ArrayList<>(original);
        for(Entity entity:shrines)for(VoxelShape shape:((ShrineEntity)entity).collisionShapes())
            if(shape.bounds().intersects(area))shapes.add(shape);
        return shapes;
    }
}
