package dev.unlimitedvoid.client;

import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.JujutsuNpc;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
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
public final class NpcVisualTest {
    static boolean started;static int ticks,warmup;static JujutsuNpc gojo,sukuna;
    static Vec3 gojoStart,sukunaStart;
    static void require(boolean ok,String message){if(!ok)throw new AssertionError("NPC_FAIL "+message);LogUtils.getLogger().info("NPC_PASS {}",message);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("uv.npcTest")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(!started&&mc.screen instanceof TitleScreen){started=true;mc.createWorldOpenFlows().createFreshLevel("Npc-QA-"+System.currentTimeMillis(),new LevelSettings("NPC QA",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());}
        if(mc.player==null||mc.level==null||mc.screen!=null)return;if(warmup++<200)return;ticks++;
        if(ticks==5){require(mc.getEntityRenderDispatcher().getRenderer(DomainEntities.STUDENT_GOJO.get().create(mc.level))!=null,"Gojo renderer registered");require(mc.getEntityRenderDispatcher().getRenderer(DomainEntities.SUKUNA_16.get().create(mc.level))!=null,"Sukuna renderer registered");}
        if(ticks==10)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var level=p.serverLevel();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);level.setDayTime(6000);p.teleportTo(level,0,-60,-6,0,0);
            var eggs=new Item[]{DomainItems.STUDENT_GOJO.get(),DomainItems.SUKUNA_16.get()};
            int x=-2;
            for(var egg:eggs){
                var pos=new BlockPos(x,-61,0);level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(egg));
                require(p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(x+.5,-60,.5),Direction.UP,pos,false))).consumesAction(),"real spawn egg interaction "+egg);
                x+=3;
            }
            var actors=level.getEntitiesOfClass(JujutsuNpc.class,new AABB(-10,-65,-5,10,-50,5));
            require(actors.size()==2,"two NPCs spawned from eggs");
            gojo=actors.stream().filter(JujutsuNpc::isGojo).findFirst().orElseThrow();sukuna=actors.stream().filter(a->!a.isGojo()).findFirst().orElseThrow();
            for(var actor:actors){actor.setYRot(180);actor.setYBodyRot(180);actor.setYHeadRot(180);require(actor.isPersistenceRequired(),"persistent display actor");require(actor.getTarget()==null,"no combat target");}
            gojoStart=gojo.position();sukunaStart=sukuna.position();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        });
        if(ticks==35){mc.options.hideGui=true;mc.options.fov().set(45);camera(mc,0,-60,-3.8,0,8);}
        if(ticks==75)Screenshot.grab(mc.gameDirectory,"npc-front.png",mc.getMainRenderTarget(),c->{});
        if(ticks==90)mc.getSingleplayerServer().execute(()->{
            require(gojo.position().distanceToSqr(gojoStart)<.02&&sukuna.position().distanceToSqr(sukunaStart)<.02,"no wandering");
            require(gojo.getTarget()==null&&sukuna.getTarget()==null,"no automatic combat");
            var tag=new CompoundTag();require(gojo.save(tag),"NPC supports world save");tag.remove("UUID");
            var reloaded=EntityType.loadEntityRecursive(tag,gojo.level(),a->a);
            require(reloaded instanceof JujutsuNpc&&((JujutsuNpc)reloaded).isGojo(),"entity type restored from NBT");
            require(((JujutsuNpc)reloaded).isPersistenceRequired(),"persistence restored from NBT");
            require(reloaded.getX()==gojo.getX()&&reloaded.getY()==gojo.getY(),"saved position restored");
            var level=gojo.level();reloaded.setPos(5,-60,0);require(level.addFreshEntity(reloaded),"saved NPC can be added to world");
        });
        if(ticks==80)camera(mc,5,-60,-5,45,6);
        if(ticks==85)Screenshot.grab(mc.gameDirectory,"npc-side.png",mc.getMainRenderTarget(),c->{});
        if(ticks==100){mc.options.fov().set(35);camera(mc,gojoStart.x,-60,-2.1,0,-3);}
        if(ticks==120)Screenshot.grab(mc.gameDirectory,"npc-gojo-close.png",mc.getMainRenderTarget(),c->{});
        if(ticks==125)camera(mc,sukunaStart.x,-60,-2.1,0,-3);
        if(ticks==145)Screenshot.grab(mc.gameDirectory,"npc-sukuna-close.png",mc.getMainRenderTarget(),c->{});
        if(ticks==170){
            require(mc.level.getEntitiesOfClass(JujutsuNpc.class,new AABB(-10,-65,-10,10,-50,10)).size()==3,"all actors synchronized to client");
            for(var name:new String[]{"student_gojo","sukuna_16"}){
                var texture=new net.minecraft.resources.ResourceLocation(UnlimitedVoid.ID,"textures/entity/"+name+".png");
                require(mc.getResourceManager().getResource(texture).isPresent(),"skin resource present "+name);
            }
            if(Boolean.getBoolean("uv.shaderTest"))try{var manager=Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager").invoke(null);var pipe=manager.getClass().getMethod("getPipelineNullable").invoke(manager);require(pipe!=null&&pipe.getClass().getSimpleName().equals("IrisRenderingPipeline"),"real shader pipeline active");}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
            LogUtils.getLogger().info("NPC_TEST_COMPLETE shader={}",Boolean.getBoolean("uv.shaderTest"));mc.stop();
        }
    }
    static void camera(Minecraft mc,double x,double y,double z,float yaw,float pitch){
        mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(mc.player);
        mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.teleportTo(p.serverLevel(),x,y,z,yaw,pitch);});
    }
}
