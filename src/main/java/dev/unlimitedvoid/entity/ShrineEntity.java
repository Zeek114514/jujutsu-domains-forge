package dev.unlimitedvoid.entity;

import dev.unlimitedvoid.*;
import dev.unlimitedvoid.server.ShrineStructures;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import java.util.*;

/** One stationary, temporary shrine entity with a shared multi-box collision shape. */
public final class ShrineEntity extends Entity implements IEntityAdditionalSpawnData {
    private float facing;
    private Vec3 builtAt=Vec3.ZERO;
    private int initialAge;
    private Vec3 anchor=Vec3.ZERO;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> AGE=net.minecraft.network.syncher.SynchedEntityData.defineId(ShrineEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> CLOSING=net.minecraft.network.syncher.SynchedEntityData.defineId(ShrineEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private List<VoxelShape> collision=List.of();
    public ShrineEntity(EntityType<? extends ShrineEntity> type,Level level) {super(type,level);setNoGravity(true);}
    public void configure(Vec3 anchor,float yaw) {facing=yaw;this.anchor=anchor;setPos(anchor.add(0,-10.27,0));setYRot(yaw);rebuild();}
    private void rebuild(){builtAt=position();collision=ShrineGeometry.worldBoxes(position(),facing).stream().map(Shapes::create).toList();}
    public List<VoxelShape> collisionShapes(){return collision;}
    public float facing(){return facing;}
    public int visualAge(){return entityData.get(AGE);}
    public int closing(){return entityData.get(CLOSING);}
    public void animate(int age,int closing){
        entityData.set(AGE,age);entityData.set(CLOSING,closing);
        float rise=DomainTimeline.rise(age),fade=DomainTimeline.fade(closing);
        setPos(anchor.add(0,-10.27*(1-rise*fade),0));rebuild();
    }
    @Override protected void defineSynchedData(){entityData.define(AGE,0);entityData.define(CLOSING,-1);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){}
    @Override protected void addAdditionalSaveData(CompoundTag tag){}
    @Override public boolean canBeCollidedWith(){return false;} // exact shapes are supplied by the common collision hook
    @Override public boolean isPushable(){return false;}
    @Override public boolean isPickable(){return false;} // do not let the broad-phase box steal aimed attacks
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return false;}
    @Override public void tick(){
        super.tick();setDeltaMovement(Vec3.ZERO);
        if(level().isClientSide) {
            setPos(anchor.add(0,-10.27*(1-DomainTimeline.rise(visualAge())*DomainTimeline.fade(closing())),0));
        }
        if(collision.isEmpty() || position().distanceToSqr(builtAt)>0.000001)rebuild();
        if(!level().isClientSide && (!ShrineStructures.manages(this) || tickCount>1000))discard();
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
    @Override public void writeSpawnData(FriendlyByteBuf buffer){buffer.writeFloat(facing);buffer.writeDouble(anchor.x);buffer.writeDouble(anchor.y);buffer.writeDouble(anchor.z);}
    @Override public void readSpawnData(FriendlyByteBuf buffer){facing=buffer.readFloat();anchor=new Vec3(buffer.readDouble(),buffer.readDouble(),buffer.readDouble());rebuild();}
}
