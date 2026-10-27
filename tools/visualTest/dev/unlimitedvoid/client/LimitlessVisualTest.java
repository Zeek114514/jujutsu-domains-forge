package dev.unlimitedvoid.client;

import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.InputConstants;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.server.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.nio.file.*;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class LimitlessVisualTest {
    static boolean started;static volatile boolean failed;static int ticks,menus,retries;
    static UUID blue,ally,red,far,purpleA,purpleB,purpleC,behind;
    static final Map<UUID,Float> damage=new ConcurrentHashMap<>();static final Map<UUID,Integer> hits=new ConcurrentHashMap<>();
    static int chargeSounds,blueSounds,redSounds,purpleSounds;static double blueInitial;
    static int breaks;
    @SubscribeEvent public static void breaking(net.minecraftforge.event.level.BlockEvent.BreakEvent e){if(Boolean.getBoolean("uv.limitlessTest")){breaks++;LogUtils.getLogger().info("LIMITLESS_TERRAIN_BREAK {}",e.getPos());}}
    @SubscribeEvent public static void hurt(LivingHurtEvent e){if(!Boolean.getBoolean("uv.limitlessTest"))return;String kind=e.getSource().getMsgId();if(kind.equals("unlimitedvoid.blue")||kind.equals("unlimitedvoid.red")||kind.equals("unlimitedvoid.purple")){damage.merge(e.getEntity().getUUID(),e.getAmount(),Float::sum);hits.merge(e.getEntity().getUUID(),1,Integer::sum);}}
    @SubscribeEvent public static void sound(net.minecraftforge.client.event.sound.PlaySoundSourceEvent e){if(!Boolean.getBoolean("uv.limitlessTest"))return;switch(e.getSound().getLocation().getPath()){case "limitless_charge"->chargeSounds++;case "blue_cast"->blueSounds++;case "red_cast"->redSounds++;case "purple_cast"->purpleSounds++;}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("uv.limitlessTest")||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());if(failed){mc.stop();return;}
        boolean observer=Boolean.getBoolean("uv.fugaObserver");
        if(mc.player==null)menus++;
        if(observer&&mc.screen instanceof DisconnectedScreen&&menus%40==0){require(retries<3,"observer retry bound");connect(mc);}
        if(!started&&mc.screen instanceof TitleScreen){
            if(observer&&menus<80)return;started=true;
            if(observer)connect(mc);else mc.createWorldOpenFlows().createFreshLevel("Limitless-QA-"+System.currentTimeMillis(),new LevelSettings("Limitless QA",GameType.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }
        if(mc.player==null||mc.level==null||mc.screen!=null)return;ticks++;
        if(observer){observer(mc);return;}
        if(ticks==21&&Boolean.getBoolean("uv.fugaNetwork")&&mc.getSingleplayerServer().getPlayerList().getPlayerCount()<2){ticks--;return;}
        if(ticks==20){mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);server(mc,p->{
            var l=p.serverLevel();l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);l.setDayTime(6000);p.teleportTo(l,0,-60,0,0,5);
            require(!DomainConfig.LIMITLESS_TERRAIN.get(),"terrain destruction off by default");p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));
            blue=pig(p,3,15);ally=pig(p,-3,15);var board=l.getScoreboard();var team=board.addPlayerTeam("limitless-qa");board.addPlayerToTeam(p.getScoreboardName(),team);board.addPlayerToTeam(l.getEntity(ally).getScoreboardName(),team);
            if(Boolean.getBoolean("uv.fugaNetwork"))try{p.server.setUsesAuthentication(false);p.server.getConnection().startTcpServerListener(java.net.InetAddress.getByName("127.0.0.1"),25576);Files.writeString(Path.of(System.getProperty("uv.readyFile")),"ready");}catch(Exception e){throw new RuntimeException(e);}
        });}
        if(ticks==40){hold(GLFW.GLFW_KEY_Z,true);key(GLFW.GLFW_KEY_Z);}
        if(ticks==65){require(LimitlessClient.pose(mc.player.getUUID())!=null,"Blue first-person pose active");capture(mc,"blue-charge.png");}
        if(ticks==90)hold(GLFW.GLFW_KEY_Z,false);
        if(ticks==110){capture(mc,"blue-attraction.png");server(mc,p->{
            var s=LimitlessService.snapshot(p).stream().filter(c->c.kind()==LimitlessKind.BLUE&&c.phase()==2).findFirst();require(s.isPresent(),"charged Blue anchors on ground");require(s.get().charge()==40,"held Blue reaches full charge");
            var target=(LivingEntity)p.serverLevel().getEntity(blue);blueInitial=new net.minecraft.world.phys.Vec3(3,-60,15).distanceTo(s.get().position());require(hits.getOrDefault(blue,0)>0,"Blue deals pulse damage");require(!hits.containsKey(ally),"Blue team protection");
        });}
        if(ticks==145)server(mc,p->{var s=LimitlessService.snapshot(p).stream().filter(c->c.kind()==LimitlessKind.BLUE&&c.phase()==2).findFirst();var target=p.serverLevel().getEntity(blue);require(s.isPresent()&&target.position().distanceTo(s.get().position())<blueInitial,"Blue pulls targets closer over time");});
        if(ticks==180)server(mc,p->{discard(p,blue,ally);p.teleportTo(p.serverLevel(),0,-60,0,0,5);red=pig(p,0,12);far=pig(p,0,40);});
        if(ticks==200)key(GLFW.GLFW_KEY_X);
        if(ticks==210)capture(mc,"red-charge.png");
        if(ticks==229)capture(mc,"red-impact.png");
        if(ticks==245)server(mc,p->{require(damage.getOrDefault(red,0f)>35,"Red causes high damage");require(p.serverLevel().getEntity(red).getZ()>13,"Red knocks target away");require(!hits.containsKey(far),"Red local radius respected");discard(p,red,far);});
        if(ticks==265)server(mc,p->{p.teleportTo(p.serverLevel(),0,-60,0,0,0);purpleA=pig(p,0,14);purpleB=pig(p,0,28);purpleC=pig(p,0,46);behind=pig(p,0,70);for(int y=-61;y<=-54;y++)for(int x=-6;x<=6;x++)p.serverLevel().setBlockAndUpdate(new BlockPos(x,y,60),Blocks.STONE.defaultBlockState());});
        if(ticks==280)key(GLFW.GLFW_KEY_C);
        if(ticks==310){capture(mc,"purple-blue-red.png");if(Boolean.getBoolean("uv.shaderTest"))try{var m=Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager").invoke(null);var p=m.getClass().getMethod("getPipelineNullable").invoke(m);require(p!=null&&p.getClass().getSimpleName().equals("IrisRenderingPipeline"),"real shader pipeline active");}catch(Exception e){require(false,"shader introspection "+e);}}
        if(ticks==340)capture(mc,"purple-fusion.png");
        if(ticks==352){mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);capture(mc,"purple-launch.png");}
        if(ticks==380){mc.options.setCameraType(CameraType.FIRST_PERSON);server(mc,p->{
            for(var id:new UUID[]{purpleA,purpleB,purpleC}){require(hits.getOrDefault(id,0)==1,"Purple hits path target exactly once");require(damage.getOrDefault(id,0f)==160,"Purple 160 HP damage");}
            require(!hits.containsKey(behind),"wall stops Purple");require(p.serverLevel().getBlockState(new BlockPos(0,-58,60)).is(Blocks.STONE),"terrain retained");
            require(p.getHealth()==p.getMaxHealth(),"caster protected");require(p.getMainHandItem().is(Items.DIAMOND_SWORD),"inventory unchanged");
            LimitlessService.reset(p);require(LimitlessService.cooldown(p,LimitlessKind.PURPLE)==0,"server reset clears technique cooldowns");
        });}
        if(ticks==390){require(blueSounds==1&&redSounds==1&&purpleSounds==1,"each launch cue once despite repeated snapshots");require(chargeSounds==3,"three charge cues");require(LimitlessClient.pose(mc.player.getUUID())==null,"cast pose clears");}
        if(ticks==395)server(mc,p->{p.teleportTo(p.serverLevel(),0,-60,50,0,0);DomainConfig.LIMITLESS_TERRAIN.set(true);for(int y=-61;y<=-54;y++)for(int x=-6;x<=6;x++)p.serverLevel().setBlockAndUpdate(new BlockPos(x,y,64),Blocks.BEDROCK.defaultBlockState());});
        if(ticks==400)key(GLFW.GLFW_KEY_C);
        if(ticks==410)server(mc,p->{require(LimitlessService.casting(p.getUUID()),"second Purple charge accepted");LogUtils.getLogger().info("LIMITLESS_TERRAIN_CAST pos={} terrain={} states={}",p.position(),DomainConfig.LIMITLESS_TERRAIN.get(),LimitlessService.snapshot(p));});
        if(ticks==505)server(mc,p->{LogUtils.getLogger().info("LIMITLESS_TERRAIN_RESULT breaks={} state={} casts={}",breaks,p.serverLevel().getBlockState(new BlockPos(0,-58,60)),LimitlessService.snapshot(p));require(p.serverLevel().getBlockState(new BlockPos(0,-58,60)).isAir(),"opt-in Purple breaks stone along path");require(p.serverLevel().getBlockState(new BlockPos(0,-58,64)).is(Blocks.BEDROCK),"opt-in terrain respects unbreakable blocks");DomainConfig.LIMITLESS_TERRAIN.set(false);p.teleportTo(p.serverLevel(),0,-60,0,0,0);});
        if(ticks==520){hold(GLFW.GLFW_KEY_Z,true);key(GLFW.GLFW_KEY_Z);}
        if(ticks==530)server(mc,p->p.setGameMode(GameType.SPECTATOR));
        if(ticks==545){hold(GLFW.GLFW_KEY_Z,false);require(LimitlessClient.pose(mc.player.getUUID())==null,"spectator cancellation clears pose");server(mc,p->require(!LimitlessService.casting(p.getUUID()),"spectator cancellation clears server cast"));}
        if(ticks==560){LogUtils.getLogger().info("LIMITLESS_TEST_COMPLETE shader={} network={}",Boolean.getBoolean("uv.shaderTest"),Boolean.getBoolean("uv.fugaNetwork"));mc.stop();}
    }
    static void observer(Minecraft mc){if(ticks==70){require(mc.getSingleplayerServer()==null,"real remote client");require(LimitlessClient.effectCount()>0,"remote receives Blue");capture(mc,"observer-blue.png");}if(ticks==300){require(LimitlessClient.effects().stream().anyMatch(e->e.state.kind()==LimitlessKind.PURPLE),"remote receives Purple fusion");capture(mc,"observer-purple-fusion.png");}if(ticks==400){LogUtils.getLogger().info("LIMITLESS_OBSERVER_COMPLETE");mc.stop();}}
    static void connect(Minecraft mc){retries++;ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25576"),new ServerData("Limitless loopback QA","127.0.0.1:25576",false),false);}
    static UUID pig(ServerPlayer p,double x,double z){var e=EntityType.PIG.create(p.serverLevel());e.moveTo(x,-60,z,0,0);e.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);e.setHealth(500);p.serverLevel().addFreshEntity(e);return e.getUUID();}
    static void discard(ServerPlayer p,UUID... ids){for(var id:ids){var e=p.serverLevel().getEntity(id);if(e!=null)e.discard();}}
    static void require(boolean ok,String msg){if(!ok){failed=true;LogUtils.getLogger().error("LIMITLESS_FAIL {}",msg);throw new AssertionError(msg);}LogUtils.getLogger().info("LIMITLESS_PASS {}",msg);}
    static void server(Minecraft mc,Consumer<ServerPlayer> a){var s=mc.getSingleplayerServer();var id=mc.player.getUUID();s.execute(()->{try{a.accept(s.getPlayerList().getPlayer(id));}catch(Throwable e){failed=true;LogUtils.getLogger().error("LIMITLESS_FAIL",e);}});}
    static void key(int key){KeyMapping.click(InputConstants.getKey(key,0));}
    static void hold(int key,boolean down){KeyMapping.set(InputConstants.getKey(key,0),down);}
    static void capture(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->LogUtils.getLogger().info("LIMITLESS_CAPTURE {}",name));}
}
