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
    private static volatile int ticks,voiceStarts;
    private static volatile int launchStarts,impactStarts,launchTick,firstFlightTicks,secondFlightTicks;
    private static volatile net.minecraft.client.resources.sounds.SoundInstance lastVoice;
    private static volatile net.minecraft.client.resources.sounds.SoundInstance lastImpact;
    private static final java.util.UUID AUDIO_CANCEL_OWNER=new java.util.UUID(0,123);
    @SubscribeEvent public static void source(net.minecraftforge.client.event.sound.PlaySoundSourceEvent event){
        if(!Boolean.getBoolean("uv.fugaTest")||!event.getSound().getLocation().getNamespace().equals("unlimitedvoid"))return;
        String cue=event.getSound().getLocation().getPath();
        if(cue.equals("fuga_open")){voiceStarts++;lastVoice=event.getSound();}
        else if(cue.equals("fuga_launch")){launchStarts++;launchTick=ticks;requireClient(!event.getSound().isRelative(),"launch cue is positioned in the world");}
        else if(cue.equals("fuga_impact")){
            lastImpact=event.getSound();
            impactStarts++;int delay=ticks-launchTick;
            if(impactStarts==1)firstFlightTicks=delay;else if(impactStarts==2)secondFlightTicks=delay;
            requireClient(!event.getSound().isRelative(),"impact cue is positioned at the hit location");
        }else return;
        LogUtils.getLogger().info("FUGA_AUDIO_STARTED: cue={} tick={} relative={} position=({},{},{})",cue,ticks,event.getSound().isRelative(),event.getSound().getX(),event.getSound().getY(),event.getSound().getZ());
    }
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
        if(ticks==73){requireClient(voiceStarts==1&&launchStarts==0&&impactStarts==0&&FugaClient.drawProgress(mc.player.getUUID())==1,"voice and full draw precede both effect cues");capture(mc,"fuga-full-draw.png");}
        if(ticks==75)key(GLFW.GLFW_KEY_O);
        if(ticks==80)server(mc,p->{require(FugaService.cooldown(p)==0&&FugaService.casting(p.getUUID()),"O resets cooldown without interrupting cast");});
        if(ticks==82)key(GLFW.GLFW_KEY_H);
        if(ticks==85)capture(mc,"02-ready-bow-first-person.png");
        if(ticks==90)requireClient(FugaClient.drawProgress(mc.player.getUUID())==1,"hold full draw until spoken cue ends");
        if(ticks==100)requireClient(FugaClient.drawProgress(mc.player.getUUID())<0&&voiceStarts==1&&launchStarts==1&&impactStarts==1&&firstFlightTicks>0,"near hit plays launch then impact once after Fuga");
        if(ticks==101)capture(mc,"03-impact-first-person.png");
        if(ticks==118)capture(mc,"04-plume-first-person.png");
        if(ticks==250)server(mc,p->{require(!FugaService.casting(p.getUUID())&&FugaService.cooldown(p)==0,"cast completes after reset without extending cooldown");});
        if(ticks==265){mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);server(mc,p->p.teleportTo(p.serverLevel(),0,-60,0,0,3));}
        if(ticks==270)key(GLFW.GLFW_KEY_H);
        if(ticks==300){
            // Detached client camera gives the enlarged bow and full aura room in the frame.
            var observer=new net.minecraft.world.entity.decoration.ArmorStand(mc.level,24,-50,-24);
            observer.moveTo(24,-50,-24,45,14);observer.setOldPosAndRot();
            observer.setYHeadRot(45);observer.yHeadRotO=45;
            mc.options.hideGui=true;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);
        }
        if(ticks==312)capture(mc,"05-third-person-bow-pose.png");
        if(ticks==317){mc.options.hideGui=false;mc.setCameraEntity(mc.player);mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);}
        if(ticks==328){mc.options.setCameraType(CameraType.FIRST_PERSON);server(mc,p->{p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(p.serverLevel(),0,40,-260,0,0);});}
        if(ticks==340)capture(mc,"06-impact-third-person.png");
        if(ticks==360){capture(mc,"07-district-firestorm.png");LogUtils.getLogger().info("FUGA_FPS_DISTANT: {}",mc.getFps());}
        if(ticks==390)capture(mc,"07b-district-firestorm-loaded.png");
        if(ticks==400)server(mc,p->p.teleportTo(p.serverLevel(),0,-55,-48,0,0));
        if(ticks==410){capture(mc,"08-inside-firestorm.png");LogUtils.getLogger().info("FUGA_FPS_INSIDE: {}",mc.getFps());}
        if(ticks==464)server(mc,p->{require(!FugaService.casting(p.getUUID())&&FugaService.cooldown(p)>0,"natural cast ends while cooldown remains");});
        if(ticks==464)requireClient(launchStarts==2&&impactStarts==2&&secondFlightTicks>firstFlightTicks,"longer flight delays impact cue without repeating either stage");
        if(ticks==465)capture(mc,"08-cooldown-hud.png");
        if(ticks==466)key(GLFW.GLFW_KEY_O);
        if(ticks==468){server(mc,p->{require(FugaService.cooldown(p)==0,"O clears natural remaining cooldown");});capture(mc,"09-ready-hud.png");}
        if(ticks==485)key(GLFW.GLFW_KEY_H);
        if(ticks==493)server(mc,p->p.setGameMode(GameType.SPECTATOR));
        // SoundEngine keeps stopped channel handles for 20 ticks; check after that grace period,
        // still before this 2.75-second clip could have ended naturally.
        if(ticks==518)requireClient(voiceStarts==3&&launchStarts==2&&impactStarts==2&&lastVoice!=null&&!mc.getSoundManager().isActive(lastVoice)&&FugaClient.drawProgress(mc.player.getUUID())<0,"cancel stops voice and does not trigger launch or impact audio");
        if(ticks==535)ClientFugaAudio.impact(AUDIO_CANCEL_OWNER,mc.player.position().add(0,1,2));
        if(ticks==540)requireClient(impactStarts==3&&lastImpact!=null&&mc.getSoundManager().isActive(lastImpact),"long impact tail is actually playing before cancellation");
        if(ticks==545)ClientFugaAudio.stop(AUDIO_CANCEL_OWNER);
        if(ticks==562)requireClient(!mc.getSoundManager().isActive(lastImpact),"cancel stops positioned blast tail despite mod volume scaling");
        if(ticks==575){LogUtils.getLogger().info("FUGA_VISUAL_TEST_COMPLETE");mc.stop();}
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);LogUtils.getLogger().info("FUGA_PASS: {}",message);}
    private static void requireClient(boolean ok,String message){try{require(ok,message);}catch(Throwable error){failed=true;LogUtils.getLogger().error("FUGA_TEST_FAIL",error);}}
    private static void key(int key){KeyMapping.click(InputConstants.getKey(key,0));}
    private static void server(Minecraft mc,Consumer<ServerPlayer> action){var s=mc.getSingleplayerServer();var id=mc.player.getUUID();s.execute(()->{try{action.accept(s.getPlayerList().getPlayer(id));}catch(Throwable t){failed=true;LogUtils.getLogger().error("FUGA_TEST_FAIL",t);}});}
    private static void capture(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->LogUtils.getLogger().info("FUGA_CAPTURE {}",name));}
}
