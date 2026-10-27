package dev.unlimitedvoid.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.server.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Real requests, actual server damage, optional TCP observer; disposable world only. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class PurpleComboVisualTest {
    static boolean started;static volatile boolean failed;static int ticks,menus,retries;
    static volatile Vec3 center;static UUID near,far,outside,ally,blocked;
    static final Map<UUID,Float> damage=new ConcurrentHashMap<>();
    static final Map<UUID,Integer> hits=new ConcurrentHashMap<>();
    static final Map<String,Integer> cues=new ConcurrentHashMap<>();
    static final Set<UUID> grows=new HashSet<>(),blasts=new HashSet<>();
    static int latestBlastTick;
    @SubscribeEvent public static void hurt(LivingHurtEvent e){if(Boolean.getBoolean("uv.comboTest")&&e.getSource().getMsgId().equals("unlimitedvoid.purple")){damage.merge(e.getEntity().getUUID(),e.getAmount(),Float::sum);hits.merge(e.getEntity().getUUID(),1,Integer::sum);}}
    @SubscribeEvent public static void sound(net.minecraftforge.client.event.sound.PlaySoundSourceEvent e){if(Boolean.getBoolean("uv.comboTest")&&e.getSound() instanceof LimitlessClient.Cue cue)cues.merge(cue.cast+":"+e.getSound().getLocation().getPath(),1,Integer::sum);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("uv.comboTest")||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());if(failed){mc.stop();return;}
        boolean observer=Boolean.getBoolean("uv.fugaObserver");if(mc.player==null)menus++;
        if(observer&&mc.player==null&&menus%80==0&&mc.screen!=null)LogUtils.getLogger().info("PURPLE_COMBO_SCREEN {} {}",mc.screen.getClass().getSimpleName(),mc.screen.getNarrationMessage().getString());
        if(observer&&mc.screen instanceof DisconnectedScreen&&menus%40==0){require(retries<3,"observer retry bound");connect(mc);}
        if(!started&&mc.screen instanceof TitleScreen){if(observer&&menus<80)return;started=true;
            if(observer)connect(mc);
            else mc.createWorldOpenFlows().createFreshLevel("Purple-Combo-QA-"+System.currentTimeMillis(),new LevelSettings("Purple Combo QA",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }
        if(mc.player==null||mc.level==null||mc.screen!=null)return;ticks++;
        if(observer){observe(mc);return;}
        if(ticks==21&&Boolean.getBoolean("uv.fugaNetwork")&&mc.getSingleplayerServer().getPlayerList().getPlayerCount()<2){ticks--;return;}
        if(ticks==20){mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);server(mc,p->{var l=p.serverLevel();l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);l.setDayTime(6000);p.teleportTo(l,0,-60,0,0,-90);
            require(!DomainConfig.LIMITLESS_TERRAIN.get(),"terrain stays disabled by default");
            if(Boolean.getBoolean("uv.fugaNetwork"))try{p.server.setUsesAuthentication(false);p.server.getConnection().startTcpServerListener(java.net.InetAddress.getByName("127.0.0.1"),25576);Files.writeString(Path.of(System.getProperty("uv.readyFile")),"ready");}catch(Exception ex){throw new RuntimeException(ex);}
        });}
        if(ticks==40)key(GLFW.GLFW_KEY_Z);
        if(ticks==80)server(mc,p->{var blue=LimitlessService.snapshot(p).stream().filter(s->s.kind()==LimitlessKind.BLUE&&s.phase()==PurpleDetonation.AIR).findFirst();require(blue.isPresent(),"Blue holds in the sky");center=blue.orElseThrow().position();require(center.y>p.getY()+35,"Blue remains above the ground");
            near=pig(p,center.add(15,0,0));far=pig(p,center.add(55,0,0));outside=pig(p,center.add(66,0,0));ally=pig(p,center.add(-18,0,0));blocked=pig(p,center.add(0,0,20));
            var board=p.serverLevel().getScoreboard();var team=board.addPlayerTeam("purple-qa");board.addPlayerToTeam(p.getScoreboardName(),team);board.addPlayerToTeam(p.serverLevel().getEntity(ally).getScoreboardName(),team);wall(p,false);
        });
        if(ticks==90)key(GLFW.GLFW_KEY_X);
        if(ticks==108)camera(mc);
        if(ticks==130)server(mc,p->{var states=LimitlessService.snapshot(p);var growing=states.stream().filter(s->s.phase()==PurpleDetonation.GROWING).findFirst();require(growing.isPresent(),"Blue then Red creates growing Purple");require(states.stream().noneMatch(s->s.kind()!=LimitlessKind.PURPLE),"fusion consumes both ingredients");require(growing.orElseThrow().blastRadius()==64,"blast radius synchronized from server");p.setGameMode(GameType.SURVIVAL);p.setHealth(p.getMaxHealth());});
        if(ticks==134)capture(mc,"fusion.png");
        if(ticks==154)capture(mc,"grown.png");
        if(ticks==184)capture(mc,"detonation.png");
        if(ticks>=120&&ticks<=210&&ticks%2==0)capture(mc,String.format("frame-%04d.png",ticks));
        if(ticks==220)server(mc,p->{
            require(hits.getOrDefault(near,0)==1&&damage.getOrDefault(near,0f)>380,"near target receives one massive hit");
            require(hits.getOrDefault(far,0)==1&&damage.getOrDefault(far,0f)>150,"wave reaches target 55 blocks away");
            require(!hits.containsKey(outside),"outside radius remains unharmed");require(!hits.containsKey(ally),"team protection preserved");require(!hits.containsKey(blocked),"solid wall shelters target");
            require(hits.getOrDefault(p.getUUID(),0)==1&&p.getHealth()>0&&p.getHealth()<p.getMaxHealth(),"caster takes reduced backlash and survives at firing distance");
            require(p.serverLevel().getBlockState(BlockPos.containing(center.add(0,0,10))).is(Blocks.STONE),"default detonation preserves blocks");p.setGameMode(GameType.CREATIVE);p.setHealth(p.getMaxHealth());
        });
        if(ticks==230)server(mc,p->{LimitlessService.reset(p);for(var id:new UUID[]{near,far,outside,ally,blocked}){var target=p.serverLevel().getEntity(id);if(target!=null)target.discard();}wall(p,true);p.teleportTo(p.serverLevel(),0,-60,0,0,-90);});
        if(ticks==240)key(GLFW.GLFW_KEY_X);
        if(ticks==278)server(mc,p->require(LimitlessService.snapshot(p).stream().anyMatch(s->s.kind()==LimitlessKind.RED&&s.phase()==PurpleDetonation.AIR),"Red also holds in the sky"));
        if(ticks==280)key(GLFW.GLFW_KEY_Z);
        if(ticks==330)server(mc,p->require(LimitlessService.snapshot(p).stream().anyMatch(s->s.phase()==PurpleDetonation.GROWING),"Red then Blue also fuses"));
        if(ticks==370)server(mc,p->require(LimitlessService.snapshot(p).stream().anyMatch(s->s.phase()==PurpleDetonation.BLAST),"reverse combination detonates"));
        if(ticks==410){require(count("purple_impact")==2&&count("purple_voice")==0,"two automatic blasts without replaying manual Purple voice");require(cues.values().stream().allMatch(n->n==1),"repeated snapshots do not duplicate cues");}
        if(ticks==430)server(mc,p->LimitlessService.reset(p));
        if(ticks==440)key(GLFW.GLFW_KEY_X);
        if(ticks==480)key(GLFW.GLFW_KEY_Z);
        if(ticks==526)server(mc,p->{require(LimitlessService.snapshot(p).stream().anyMatch(s->s.phase()==PurpleDetonation.GROWING),"cancellation fixture is a growing fusion");p.setGameMode(GameType.SPECTATOR);});
        if(ticks==544){require(LimitlessClient.effectCount()==0&&count("purple_impact")==2,"spectator cancels growing fusion without explosion");server(mc,p->require(LimitlessService.snapshot(p).isEmpty(),"server removes canceled fusion"));}
        if(ticks==550)server(mc,p->{p.setGameMode(GameType.CREATIVE);LimitlessService.reset(p);p.teleportTo(p.serverLevel(),0,-60,0,0,-90);});
        if(ticks==560)key(GLFW.GLFW_KEY_X);
        if(ticks==630)server(mc,p->require(LimitlessService.snapshot(p).stream().anyMatch(s->s.phase()==PurpleDetonation.AIR),"unused airborne Red persists long enough to aim"));
        if(ticks==820){server(mc,p->require(LimitlessService.snapshot(p).isEmpty(),"unused airborne orb expires"));}
        if(ticks==830){require(LimitlessRenderer.ready(),"orb and shockwave shader remains healthy");if(Boolean.getBoolean("uv.shaderTest"))checkShader();LogUtils.getLogger().info("PURPLE_COMBO_TEST_COMPLETE shader={} network={}",Boolean.getBoolean("uv.shaderTest"),Boolean.getBoolean("uv.fugaNetwork"));mc.stop();}
    }
    static void observe(Minecraft mc){
        for(var effect:LimitlessClient.effects()){var state=effect.state;if(state.phase()==PurpleDetonation.GROWING){grows.add(state.id());requireRadius(state.blastRadius());}if(state.phase()==PurpleDetonation.BLAST&&blasts.add(state.id()))latestBlastTick=ticks;}
        if(blasts.size()>=2&&ticks-latestBlastTick>24){require(mc.getSingleplayerServer()==null&&grows.size()>=2,"remote receives both growing fusions and blasts");require(count("purple_impact")==2&&cues.values().stream().allMatch(n->n==1),"remote blast cues deduplicated");LogUtils.getLogger().info("PURPLE_COMBO_OBSERVER_COMPLETE");mc.stop();}
    }
    static void requireRadius(float radius){if(radius!=64)require(false,"remote radius mismatch");}
    static void connect(Minecraft mc){retries++;ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25576"),new ServerData("Purple combo loopback","127.0.0.1:25576",false),false);}
    static int count(String path){return cues.entrySet().stream().filter(e->e.getKey().endsWith(":"+path)).mapToInt(Map.Entry::getValue).sum();}
    static void checkShader(){try{var manager=Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager").invoke(null);var pipeline=manager.getClass().getMethod("getPipelineNullable").invoke(manager);require(pipeline!=null&&pipeline.getClass().getSimpleName().equals("IrisRenderingPipeline"),"real shader pipeline active");}catch(ReflectiveOperationException e){throw new RuntimeException(e);}}
    static void wall(ServerPlayer p,boolean remove){for(int x=-2;x<=2;x++)for(int y=-4;y<=4;y++)p.serverLevel().setBlockAndUpdate(BlockPos.containing(center.add(x,y,10)),(remove?Blocks.AIR:Blocks.STONE).defaultBlockState());}
    static UUID pig(ServerPlayer p,Vec3 point){var pig=EntityType.PIG.create(p.serverLevel());pig.moveTo(point.x,point.y,point.z,0,0);pig.setNoAi(true);pig.setNoGravity(true);pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000);pig.setHealth(2000);p.serverLevel().addFreshEntity(pig);return pig.getUUID();}
    static void camera(Minecraft mc){var camera=new ArmorStand(mc.level,85,-22,-85);camera.moveTo(85,-22,-85,45,-2);camera.setOldPosAndRot();camera.setYHeadRot(45);camera.yHeadRotO=45;mc.options.hideGui=true;mc.setCameraEntity(camera);}
    static void server(Minecraft mc,Consumer<ServerPlayer> action){var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{try{action.accept(server.getPlayerList().getPlayer(id));}catch(Throwable error){failed=true;LogUtils.getLogger().error("PURPLE_COMBO_FAIL",error);}});}
    static void key(int key){KeyMapping.click(InputConstants.getKey(key,0));}
    static void capture(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->{});}
    static void require(boolean ok,String message){if(!ok){failed=true;LogUtils.getLogger().error("PURPLE_COMBO_FAIL {}",message);throw new AssertionError(message);}LogUtils.getLogger().info("PURPLE_COMBO_PASS {}",message);}
}
