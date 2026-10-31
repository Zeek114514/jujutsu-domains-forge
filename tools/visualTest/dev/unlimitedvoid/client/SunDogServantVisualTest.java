package dev.unlimitedvoid.client;

import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.InnocentSunDog;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class SunDogServantVisualTest {
    static boolean started; static int ticks,warmup;
    static void require(boolean ok,String message){if(!ok)throw new AssertionError("SUN_DOG_SERVANT_FAIL "+message);LogUtils.getLogger().info("SUN_DOG_SERVANT_PASS {}",message);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("uv.sunDogServantTest")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(!started&&mc.screen instanceof TitleScreen){started=true;mc.createWorldOpenFlows().createFreshLevel("Sun-Dog-QA-"+System.currentTimeMillis(),new LevelSettings("Sun Dog QA",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());}
        if(mc.player==null||mc.level==null||mc.screen!=null)return;
        if(warmup++<140)return;ticks++;
        if(ticks==1){
            CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(),true,mc.level.registryAccess());
            require(DomainItems.JUJUTSU_TAB.get().getDisplayItems().stream().anyMatch(s->s.is(DomainItems.SUN_DOG_SERVANT.get())),"spawn egg in dedicated tab");
            require(DomainItems.SUN_DOG_SERVANT.get().getDescription().getString().equals("孙狗的奴隶"),"Chinese egg label");
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var level=p.serverLevel();
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);level.setDayTime(6000);
                var pos=new BlockPos(0,-61,0);level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(DomainItems.SUN_DOG_SERVANT.get()));
                require(p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(.5,-60,.5),Direction.UP,pos,false))).consumesAction(),"real egg use");
                var actors=level.getEntitiesOfClass(InnocentSunDog.class,new AABB(-2,-62,-2,2,-56,2));require(actors.size()==1,"one matching NPC spawned");
                var npc=actors.get(0);require(npc.getHealth()==100&&npc.getMaxHealth()==100,"100 health");
                require(npc.getTarget()==null&&npc.isPersistenceRequired(),"peaceful and persistent");
                require(net.minecraftforge.common.ForgeSpawnEggItem.fromEntityType(npc.getType())==DomainItems.SUN_DOG_SERVANT.get(),"pick block maps to egg");
                var tag=new CompoundTag();npc.save(tag);tag.remove("UUID");
                var restored=EntityType.loadEntityRecursive(tag,level,a->a);require(restored instanceof InnocentSunDog&&((InnocentSunDog)restored).isPersistenceRequired(),"NBT round trip preserves identity and persistence");
                npc.setNoAi(true);npc.setYRot(180);npc.setYBodyRot(180);npc.setYHeadRot(180);
                p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setGameMode(GameType.SPECTATOR);
                p.teleportTo(level,.5,-60,-3.3,0,0);
            });
        }
        if(ticks==70){
            var actors=mc.level.getEntitiesOfClass(InnocentSunDog.class,new AABB(-2,-62,-2,2,-56,2));require(actors.size()==1,"entity synchronized to client");
            var renderer=mc.getEntityRenderDispatcher().getRenderer(actors.get(0));require(renderer instanceof InnocentSunDogRenderer,"photo renderer registered");
            try(var stream=mc.getResourceManager().getResource(renderer.getTextureLocation(actors.get(0))).orElseThrow().open();var image=com.mojang.blaze3d.platform.NativeImage.read(stream)){require(image.getWidth()==1024&&image.getHeight()==1024,"HD skin loaded at 1024 square");}catch(java.io.IOException ex){throw new RuntimeException(ex);}
            mc.options.hideGui=true;
        }
        if(ticks==90)Screenshot.grab(mc.gameDirectory,"sun-dog-servant-ingame.png",mc.getMainRenderTarget(),c->{});
        if(ticks==110){LogUtils.getLogger().info("SUN_DOG_SERVANT_TEST_COMPLETE");mc.stop();}
    }
}
