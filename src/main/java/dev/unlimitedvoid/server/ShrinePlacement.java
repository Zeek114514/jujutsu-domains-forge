package dev.unlimitedvoid.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.IntFunction;

/** Choose the nearest supporting block surface below the casting position. */
public final class ShrinePlacement {
    private ShrinePlacement() {}

    public static Optional<Vec3> center(ServerLevel level,Vec3 requestedCenter) {
        Vec3 feet=requestedCenter.add(0,-1,0);
        BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        int x=Mth.floor(feet.x),z=Mth.floor(feet.z);
        if(!level.hasChunkAt(BlockPos.containing(feet)))return Optional.empty();
        OptionalDouble ground=groundBelow(feet,level.getMinBuildHeight(),level.getMaxBuildHeight(),y->{
            pos.set(x,y,z);
            return level.getBlockState(pos).getCollisionShape(level,pos).toAabbs();
        });
        return ground.isPresent()?Optional.of(new Vec3(feet.x,ground.getAsDouble()+1,feet.z)):Optional.empty();
    }

    // Collision boxes are block-local; sample the exact X/Z to support stairs/slabs.
    static OptionalDouble groundBelow(Vec3 feet,int minimum,int maximum,IntFunction<List<AABB>> boxes) {
        int start=Math.min(maximum-1,Mth.floor(feet.y+0.05));
        double x=feet.x-Math.floor(feet.x),z=feet.z-Math.floor(feet.z);
        for(int y=start;y>=minimum;y--){
            double highest=Double.NEGATIVE_INFINITY;
            for(AABB box:boxes.apply(y)){
                double top=y+box.maxY;
                if(x>=box.minX && x<box.maxX && z>=box.minZ && z<box.maxZ
                    && top<=feet.y+0.05)highest=Math.max(highest,top);
            }
            if(Double.isFinite(highest))return OptionalDouble.of(highest);
        }
        return OptionalDouble.empty();
    }
}
