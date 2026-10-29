package dev.unlimitedvoid.client;
import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.function.Consumer;
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class MahoragaVisualTest {
    static boolean started;static int warmup,ticks;static MahoragaEntity actor;
    static void require(boolean ok,String text){if(!ok)throw new AssertionError("MAHORAGA_FAIL "+text);LogUtils.getLogger().info("MAHORAGA_PASS {}",text);}
    static void server(Minecraft mc,Consumer<ServerPlayer> work){mc.getSingleplayerServer().execute(()->{try{work.accept(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0));}catch(Throwable e){LogUtils.getLogger().error("MAHORAGA_FAIL server",e);mc.execute(mc::stop);}});}
    static void camera(Minecraft mc,double x,double y,double z,float yaw,float pitch){server(mc,p->p.teleportTo(p.serverLevel(),x,y,z,yaw,pitch));}
    static void capture(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->{});}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("uv.mahoragaTest")||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(!started&&mc.screen instanceof TitleScreen){started=true;mc.createWorldOpenFlows().createFreshLevel("Mahoraga-Model-QA-"+System.currentTimeMillis(),new LevelSettings("Mahoraga Model QA",GameType.CREATIVE,false,net.minecraft.world.Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());}
        if(mc.player==null||mc.level==null||mc.screen!=null)return;if(warmup++<200)return;ticks++;
        if(ticks==5){mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.options.hideGui=true;mc.options.fov().set(35);server(mc,p->{var level=p.serverLevel();p.setGameMode(GameType.SPECTATOR);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);level.setDayTime(6000);for(var e:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(-20,-70,-20,20,-40,20),e->e!=p))e.discard();for(int x=-15;x<=15;x++)for(int z=-12;z<=8;z++)level.setBlockAndUpdate(new BlockPos(x,-61,z),Blocks.GRAY_CONCRETE.defaultBlockState());for(int x=-15;x<=15;x++)for(int y=-60;y<=-47;y++)level.setBlockAndUpdate(new BlockPos(x,y,7),Blocks.GRAY_CONCRETE.defaultBlockState());actor=DomainEntities.MAHORAGA.get().create(level);actor.moveTo(0,-60,0,180,0);actor.setNoAi(true);actor.setYBodyRot(180);actor.setYHeadRot(180);level.addFreshEntity(actor);p.teleportTo(level,0,-60,-7.5,0,-2);require(actor.getMaxHealth()==1000&&actor.getHealth()==1000,"1000HP dedicated actor");require(actor.getBbWidth()>1.3&&actor.getBbHeight()>3,"giant physical dimensions");});}
        if(ticks==30)capture(mc,"mahoraga-front.png");
        if(ticks==35)camera(mc,5,-60,-6.5,38,-2);
        if(ticks==55)capture(mc,"mahoraga-three-quarter.png");
        if(ticks==60){camera(mc,0,-60,-7.5,0,-2);server(mc,p->{actor.setYRot(0);actor.setYBodyRot(0);actor.setYHeadRot(0);});}
        if(ticks==80)capture(mc,"mahoraga-back.png");
        if(ticks==85){mc.options.fov().set(50);camera(mc,0,-58.6,-3.5,0,-2);server(mc,p->{actor.setYRot(180);actor.setYBodyRot(180);actor.setYHeadRot(180);});}
        if(ticks==105)capture(mc,"mahoraga-head-wheel.png");
        if(ticks==110)server(mc,p->{float before=actor.wheelAngle(0);actor.turnWheel();require(actor.wheelAngle(0)==before,"wheel begins a smooth turn without teleporting");});
        if(ticks==130){capture(mc,"mahoraga-wheel-turned.png");server(mc,p->{require(Math.abs(actor.wheelAngle(0)-(float)Math.PI/4)<.001,"eight-handle wheel advances exactly one eighth turn");var tag=new net.minecraft.nbt.CompoundTag();actor.save(tag);tag.remove("UUID");var copy=(MahoragaEntity)net.minecraft.world.entity.EntityType.loadEntityRecursive(tag,p.serverLevel(),a->a);require(copy!=null&&copy.getHealth()==1000&&Math.abs(copy.wheelAngle(0)-(float)Math.PI/4)<.001,"wheel and health survive NBT");});}
        if(ticks==145){require(mc.getEntityRenderDispatcher().getRenderer(DomainEntities.MAHORAGA.get().create(mc.level)) instanceof MahoragaRenderer,"dedicated giant renderer registered");require(mc.getResourceManager().getResource(new net.minecraft.resources.ResourceLocation(UnlimitedVoid.ID,"textures/entity/mahoraga.png")).isPresent(),"UV texture present");if(Boolean.getBoolean("uv.shaderTest"))try{var manager=Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager").invoke(null);var pipeline=manager.getClass().getMethod("getPipelineNullable").invoke(manager);require(pipeline!=null&&pipeline.getClass().getSimpleName().equals("IrisRenderingPipeline"),"real shader pipeline active");}catch(ReflectiveOperationException e){throw new RuntimeException(e);}LogUtils.getLogger().info("MAHORAGA_TEST_COMPLETE");mc.stop();}
    }
}
