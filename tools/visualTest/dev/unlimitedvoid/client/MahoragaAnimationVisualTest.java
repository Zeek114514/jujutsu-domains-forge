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
public final class MahoragaAnimationVisualTest {
    static boolean started;static int warmup,ticks,current,lastScene,lastAge=-1,frame,requested,hits;
    static MahoragaEntity actor;static net.minecraft.world.entity.monster.Husk victim;
    static float elbowMax,kneeMax,twistMax;static boolean jumped,landed;
    static void require(boolean ok,String text){if(!ok)throw new AssertionError("MAHORAGA_ANIMATION_FAIL "+text);LogUtils.getLogger().info("MAHORAGA_ANIMATION_PASS {}",text);}
    static void server(Minecraft mc,Consumer<ServerPlayer> work){mc.getSingleplayerServer().execute(()->{try{work.accept(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0));}catch(Throwable e){LogUtils.getLogger().error("MAHORAGA_ANIMATION_FAIL server",e);mc.execute(mc::stop);}});}
    static void scene(Minecraft mc,ServerPlayer p,int type){
        var level=p.serverLevel();if(actor!=null)actor.discard();if(victim!=null)victim.discard();hits=0;jumped=landed=false;
        actor=DomainEntities.MAHORAGA.get().create(level);actor.moveTo(0,-60,0,180,0);actor.setOnGround(true);level.addFreshEntity(actor);
        victim=new net.minecraft.world.entity.monster.Husk(net.minecraft.world.entity.EntityType.HUSK,level);victim.moveTo(0,-60,type==3?-10:type==4?-8:-2.8,0,0);victim.setNoAi(true);victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(5000);victim.setHealth(5000);level.addFreshEntity(victim);
        if(type==4){var tag=new net.minecraft.nbt.CompoundTag();actor.save(tag);tag.putInt("Adaptation",MahoragaRules.setStage(0,MahoragaRules.Kind.INFINITY,4));actor.readAdditionalSaveData(tag);}
        long seed=0;for(;;seed++){var r=net.minecraft.util.RandomSource.create(seed);if(type==3||type==4&&r.nextInt(100)<15||type<3&&r.nextBoolean()==(type==1))break;}
        actor.getRandom().setSeed(seed);require(actor.startAttack(victim)&&actor.attackType()==type,"real server starts attack "+type);current=type;
        if(type==3)p.teleportTo(level,10,-59,-6,80,-8);else if(type==4)p.teleportTo(level,9,-59.5,-7,72,-8);else p.teleportTo(level,8.5,-59.8,-10,45,-7);
        mc.execute(()->mc.options.fov().set(type>=3?45:40));
    }
    @SubscribeEvent public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event){if(!Boolean.getBoolean("uv.mahoragaAnimationTest")||event.getEntity()!=victim||event.getSource().getEntity()!=actor)return;require(Math.abs(actor.attackAge(0)-MahoragaAttackMotion.contact(current))<=1,"real damage occurs on shared contact frame for attack "+current);hits++;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("uv.mahoragaAnimationTest")||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(!started&&mc.screen instanceof TitleScreen){started=true;mc.createWorldOpenFlows().createFreshLevel("Mahoraga-Animation-QA-"+System.currentTimeMillis(),new LevelSettings("Mahoraga Animation QA",GameType.CREATIVE,false,net.minecraft.world.Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());}
        if(mc.player==null||mc.level==null||mc.screen!=null)return;if(warmup++<200)return;ticks++;
        if(ticks==5){mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.options.hideGui=true;server(mc,p->{var level=p.serverLevel();p.setGameMode(GameType.SPECTATOR);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);level.setDayTime(6000);p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTimePacket(level.getGameTime(),6000,false));for(var e:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(-30,-70,-30,30,-35,30),e->e!=p))e.discard();for(int x=-20;x<=20;x++)for(int z=-22;z<=12;z++)level.setBlockAndUpdate(new BlockPos(x,-61,z),Blocks.GRAY_CONCRETE.defaultBlockState());for(int x=-20;x<=20;x++)for(int y=-60;y<=-44;y++)level.setBlockAndUpdate(new BlockPos(x,y,9),Blocks.GRAY_CONCRETE.defaultBlockState());scene(mc,p,1);});}
        if(current==0||actor==null)return;var entity=mc.level.getEntity(actor.getId());if(!(entity instanceof MahoragaEntity visible))return;
        if(lastScene!=current){lastScene=current;lastAge=-1;elbowMax=kneeMax=twistMax=0;}
        int age=(int)visible.attackAge(0);
        if(age>=0&&age<=29&&age>lastAge&&visible.attackType()==current){
            lastAge=age;Screenshot.grab(mc.gameDirectory,String.format(java.util.Locale.ROOT,"attack-%03d.png",frame++),mc.getMainRenderTarget(),c->{});
            var model=((MahoragaRenderer)mc.getEntityRenderDispatcher().getRenderer(visible)).getModel();var root=model.root();
            elbowMax=Math.max(elbowMax,Math.abs(root.getChild(current==1?"left_arm":"right_arm").getChild("forearm").xRot));kneeMax=Math.max(kneeMax,root.getChild("left_leg").getChild("shin").xRot);twistMax=Math.max(twistMax,Math.abs(root.getChild("body").yRot));
            if(!visible.onGround())jumped=true;else if(jumped)landed=true;
        }
        if(age>=33&&requested!=current){requested=current;float elbow=elbowMax,knee=kneeMax,twist=twistMax;int type=current;server(mc,p->{require(hits==1,"one actual target hit per attack "+type);require(elbow>.4f&&knee>.2f&&twist>.1f,"rendered elbow knee and torso visibly articulate for attack "+type);if(type==3)require(jumped&&landed,"leap has real airborne phase and returns to ground");if(type<4)scene(mc,p,type+1);else{require(frame>=90,"actual animation frames captured");if(Boolean.getBoolean("uv.shaderTest"))try{var manager=Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager").invoke(null);var pipeline=manager.getClass().getMethod("getPipelineNullable").invoke(manager);require(pipeline!=null&&pipeline.getClass().getSimpleName().equals("IrisRenderingPipeline"),"actual shader pipeline active");}catch(ReflectiveOperationException e){throw new RuntimeException(e);}LogUtils.getLogger().info("MAHORAGA_ANIMATION_TEST_COMPLETE frames={}",frame);mc.execute(mc::stop);}});}
    }
}
