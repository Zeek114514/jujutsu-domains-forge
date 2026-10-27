package dev.unlimitedvoid.client;

import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.server.DomainService;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Reference-style slash capture; independent disposable world, no release claims. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class ShrineSlashVisualTest {
    static boolean started;static int ticks,warmup,peak;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("uv.slashTest")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(!started&&mc.screen instanceof TitleScreen){started=true;mc.createWorldOpenFlows().createFreshLevel("Shrine-Slash-Preview-"+System.currentTimeMillis(),new LevelSettings("Shrine Slash Preview",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());}
        if(mc.player==null||mc.level==null||mc.screen!=null)return;if(warmup++<100)return;ticks++;
        if(ticks==20){mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.options.hideGui=true;mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var level=p.serverLevel();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);level.setDayTime(6000);p.teleportTo(level,0,-60,22,180,-15);
            for(int i=0;i<4;i++){var golem=EntityType.IRON_GOLEM.create(level);golem.moveTo(i%2==0?-7:7,-60,7+i*2,180,0);golem.setNoAi(true);golem.setInvulnerable(true);golem.getPersistentData().putDouble("uv_cursed_energy",1000000);level.addFreshEntity(golem);}
            if(DomainService.generate(p,DomainKind.SHRINE,new Vec3(0,-59,0),new Vec3(0,0,1))!=1)throw new AssertionError("preview domain creation");
        });}
        if(ticks>=198&&ticks<=278&&ticks%2==0){peak=Math.max(peak,ShrineRenderer.slashCount);capture(mc,String.format("frame-%04d.png",ticks));}
        if(ticks==216)capture(mc,"slashes.png");
        if(ticks==220&&Boolean.getBoolean("uv.shaderTest"))try{
            var manager=Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager").invoke(null);
            var pipeline=manager.getClass().getMethod("getPipelineNullable").invoke(manager);
            if(pipeline==null||!pipeline.getClass().getSimpleName().equals("IrisRenderingPipeline"))throw new AssertionError("shader pipeline inactive");
            LogUtils.getLogger().info("SHRINE_SLASH_SHADER_ACTIVE");
        }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
        if(ticks==282){if(peak<=0||peak>180)throw new AssertionError("slash count budget "+peak);LogUtils.getLogger().info("SHRINE_SLASH_PREVIEW_COMPLETE peak={} captureOnly=true",peak);mc.stop();}
    }
    static void capture(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->{});}
}
