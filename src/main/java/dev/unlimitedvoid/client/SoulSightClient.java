package dev.unlimitedvoid.client;
import dev.unlimitedvoid.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Bounded outlines for living souls and inanimate objects, without changing any entity's glowing flag. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class SoulSightClient {
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        var mc=Minecraft.getInstance();
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES||mc.level==null||mc.player==null||!mc.player.hasEffect(CursedToolEffects.SOUL_SIGHT.get()))return;
        var camera=event.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();var lines=buffers.getBuffer(RenderType.lines());var pose=event.getPoseStack();int count=0;
        for(var e:mc.level.getEntities(mc.player,mc.player.getBoundingBox().inflate(24),e->e instanceof LivingEntity||e instanceof ItemEntity||e instanceof HangingEntity)){
            if(count++>=64)break;
            var box=e.getBoundingBox().inflate(.035).move(-camera.x,-camera.y,-camera.z);
            LevelRenderer.renderLineBox(pose,lines,box,.55f,.96f,1f,.7f);
        }
        if(mc.hitResult instanceof BlockHitResult hit){
            var pos=hit.getBlockPos();int boxes=0;
            for(var box:mc.level.getBlockState(pos).getShape(mc.level,pos).toAabbs()){
                if(boxes++>=16)break;
                LevelRenderer.renderLineBox(pose,lines,box.move(pos).inflate(.008).move(-camera.x,-camera.y,-camera.z),.8f,1f,1f,.8f);
            }
        }
        buffers.endBatch(RenderType.lines());
    }
}
