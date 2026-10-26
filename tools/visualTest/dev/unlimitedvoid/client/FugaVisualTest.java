package dev.unlimitedvoid.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.UnlimitedVoid;
import dev.unlimitedvoid.server.FugaService;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.function.Consumer;

/** Fresh disposable world, offline account, real key mappings and production renderer. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class FugaVisualTest {
    private static boolean started;
    private static volatile boolean failed;
    private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("uv.fugaTest")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());if(failed){mc.stop();return;}
        if(!started&&mc.screen instanceof TitleScreen){
            started=true;mc.createWorldOpenFlows().createFreshLevel("Fuga-Disposable-"+System.currentTimeMillis(),
                new LevelSettings("Fuga visual QA",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }
        if(mc.player==null||mc.level==null||mc.screen!=null)return;ticks++;
        if(ticks==20){mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);server(mc,p->{
            var l=p.serverLevel();l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,l.getServer());
            l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,l.getServer());l.setDayTime(6000);
            p.teleportTo(l,0,-60,0,0,10);
        });}
        if(ticks==40)key(GLFW.GLFW_KEY_H);
        if(ticks==50)server(mc,p->{require(FugaService.casting(p.getUUID())&&FugaService.cooldown(p)>0,"H starts cast and cooldown");});
        if(ticks==65)capture(mc,"01-charge-first-person.png");
        if(ticks==75)key(GLFW.GLFW_KEY_O);
        if(ticks==80)server(mc,p->{require(FugaService.cooldown(p)==0&&FugaService.casting(p.getUUID()),"O resets cooldown without interrupting cast");});
        if(ticks==82)key(GLFW.GLFW_KEY_H);
        if(ticks==85)capture(mc,"02-ready-bow-first-person.png");
        if(ticks==101)capture(mc,"03-impact-first-person.png");
        if(ticks==118)capture(mc,"04-plume-first-person.png");
        if(ticks==250)server(mc,p->{require(!FugaService.casting(p.getUUID())&&FugaService.cooldown(p)==0,"cast completes after reset without extending cooldown");});
        if(ticks==265)mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if(ticks==270)key(GLFW.GLFW_KEY_H);
        if(ticks==312)capture(mc,"05-third-person-bow-pose.png");
        if(ticks==317)mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if(ticks==328){mc.options.setCameraType(CameraType.FIRST_PERSON);server(mc,p->{p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(p.serverLevel(),0,40,-260,0,0);});}
        if(ticks==340)capture(mc,"06-impact-third-person.png");
        if(ticks==360){capture(mc,"07-district-firestorm.png");LogUtils.getLogger().info("FUGA_FPS_DISTANT: {}",mc.getFps());}
        if(ticks==390)capture(mc,"07b-district-firestorm-loaded.png");
        if(ticks==400)server(mc,p->p.teleportTo(p.serverLevel(),0,-55,-48,0,0));
        if(ticks==410){capture(mc,"08-inside-firestorm.png");LogUtils.getLogger().info("FUGA_FPS_INSIDE: {}",mc.getFps());}
        if(ticks==455)server(mc,p->{require(!FugaService.casting(p.getUUID())&&FugaService.cooldown(p)>0,"natural cast ends while cooldown remains");});
        if(ticks==458)capture(mc,"08-cooldown-hud.png");
        if(ticks==460)key(GLFW.GLFW_KEY_O);
        if(ticks==465){server(mc,p->{require(FugaService.cooldown(p)==0,"O clears natural remaining cooldown");});capture(mc,"09-ready-hud.png");}
        if(ticks==480){LogUtils.getLogger().info("FUGA_VISUAL_TEST_COMPLETE");mc.stop();}
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);LogUtils.getLogger().info("FUGA_PASS: {}",message);}
    private static void key(int key){KeyMapping.click(InputConstants.getKey(key,0));}
    private static void server(Minecraft mc,Consumer<ServerPlayer> action){var s=mc.getSingleplayerServer();var id=mc.player.getUUID();s.execute(()->{try{action.accept(s.getPlayerList().getPlayer(id));}catch(Throwable t){failed=true;LogUtils.getLogger().error("FUGA_TEST_FAIL",t);}});}
    private static void capture(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->LogUtils.getLogger().info("FUGA_CAPTURE {}",name));}
}
