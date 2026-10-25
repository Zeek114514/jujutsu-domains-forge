package dev.unlimitedvoid.server;

import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.ShrineEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class ShrineStructures {
    private static final Map<UUID,ShrineEntity> ACTIVE=new HashMap<>();
    public static ShrineEntity get(UUID owner){return ACTIVE.get(owner);}
    public static boolean manages(ShrineEntity entity){return ACTIVE.containsValue(entity);}
    public static void spawn(UUID owner,ServerLevel level,Vec3 center,Vec3 axis){
        remove(owner);
        ShrineEntity entity=new ShrineEntity(DomainEntities.SHRINE.get(),level);
        Vec3 h=new Vec3(axis.x,0,axis.z).normalize();
        if(h.lengthSqr()<0.01)h=new Vec3(0,0,1);
        entity.configure(ShrineGeometry.anchor(center,axis),(float)Math.toDegrees(Math.atan2(h.x,h.z)));
        ACTIVE.put(owner,entity);
        if(!level.addFreshEntity(entity)){ACTIVE.remove(owner);entity.discard();}
    }
    public static void animate(UUID owner,int age,int closing){ShrineEntity entity=ACTIVE.get(owner);if(entity!=null)entity.animate(age,closing);}
    public static void remove(UUID owner){ShrineEntity entity=ACTIVE.remove(owner);if(entity!=null)entity.discard();}
    public static void retain(Set<UUID> owners){for(UUID owner:List.copyOf(ACTIVE.keySet()))if(!owners.contains(owner))remove(owner);}
    public static void clear(){for(UUID owner:List.copyOf(ACTIVE.keySet()))remove(owner);}
}
