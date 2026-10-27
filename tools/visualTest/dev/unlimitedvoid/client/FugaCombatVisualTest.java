package dev.unlimitedvoid.client;

import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.server.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Disposable world, actual server damage, actual network observer, skins and renderer. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class FugaCombatVisualTest {
    static boolean started;static volatile boolean failed;static int ticks,frames,menuTicks,connectionAttempts;
    static UUID victim,ally,far,blocked,domainVictim,protectedVictim,pet,fireResistant,disabledVictim;
    static FugaClient.Effect groundCase;
    static final Map<UUID,Integer> hits=new ConcurrentHashMap<>();
    static long renderNanos;static int maxQuads;static long lastRender,lateStart;
    static boolean framebufferChecked;
    static int launches,impacts;
    static final UUID LATE_OWNER=new UUID(888,1);
    @SubscribeEvent public static void audio(net.minecraftforge.client.event.sound.PlaySoundSourceEvent event){
        if(!Boolean.getBoolean("uv.fugaCombatTest"))return;
        String path=event.getSound().getLocation().getPath();if(path.equals("fuga_launch"))launches++;if(path.equals("fuga_impact"))impacts++;
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event){
        if(Boolean.getBoolean("uv.fugaCombatTest")&&event.getSource().getMsgId().equals("unlimitedvoid.fuga"))hits.merge(event.getEntity().getUUID(),1,Integer::sum);
    }
    @SubscribeEvent public static void frame(RenderLevelStageEvent event){
        if(!Boolean.getBoolean("uv.fugaCombatTest")||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        if(ticks>440&&ticks<470&&!framebufferChecked){
            var before=FramebufferState.capture();ShrineFloorRenderer.render(event,VoidClient.renderStates());var after=FramebufferState.capture();
            require(before.equals(after),"mirror preserves active framebuffer and viewport");framebufferChecked=true;
            LogUtils.getLogger().info("FUGA_FRAMEBUFFER caller={} main={}",before.draw(),Minecraft.getInstance().getMainRenderTarget().frameBufferId);
        }
        if(ticks>=650&&ticks<=710){frames++;maxQuads=Math.max(maxQuads,FugaRenderer.quads());long now=System.nanoTime();if(lastRender!=0)renderNanos+=now-lastRender;lastRender=now;}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("uv.fugaCombatTest")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(Boolean.getBoolean("uv.fugaObserver")&&mc.player==null&&++menuTicks%200==0&&mc.screen!=null){LogUtils.getLogger().info("FUGA_OBSERVER_SCREEN {} {}",mc.screen.getClass().getSimpleName(),mc.screen.getNarrationMessage().getString());capture(mc,"network-connection-state.png");}
        if(Boolean.getBoolean("uv.fugaObserver")&&mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen&&menuTicks%40==0){
            require(connectionAttempts<3,"network connection establishes within isolated retry budget");connect(mc);
        }
        if(failed){mc.stop();return;}
        if(!started&&mc.screen instanceof TitleScreen){
            if(Boolean.getBoolean("uv.fugaObserver")&&menuTicks<80)return;
            started=true;
            if(Boolean.getBoolean("uv.fugaObserver"))connect(mc);
            else mc.createWorldOpenFlows().createFreshLevel("Fuga-Combat-QA-"+System.currentTimeMillis(),new LevelSettings("Fuga combat QA",GameType.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }
        if(mc.player==null||mc.level==null||mc.screen!=null)return;ticks++;
        if(Boolean.getBoolean("uv.fugaObserver")){observer(mc);return;}
        if(ticks==21&&Boolean.getBoolean("uv.fugaNetwork")&&mc.getSingleplayerServer().getPlayerList().getPlayerCount()<2){ticks--;return;}
        if(ticks==20)mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        if(ticks==20)server(mc,p->{
            var l=p.serverLevel();l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);l.setDayTime(6000);
            DomainConfig.TERRAIN_DAMAGE.set(false);p.teleportTo(l,0,-60,0,0,6);p.getAbilities().flying=true;p.onUpdateAbilities();
            victim=pig(p,0,10,100);far=pig(p,0,48,100);ally=pig(p,-2,10,100);blocked=pig(p,7,10,100);
            var wolf=EntityType.WOLF.create(l);wolf.moveTo(-1,-60,9,0,0);wolf.setTame(true);wolf.setOwnerUUID(p.getUUID());wolf.setNoAi(true);l.addFreshEntity(wolf);pet=wolf.getUUID();
            fireResistant=pig(p,-3,10,100);((net.minecraft.world.entity.LivingEntity)l.getEntity(fireResistant)).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,20000));
            var board=l.getScoreboard();var team=board.addPlayerTeam("fuga-qa");board.addPlayerToTeam(p.getScoreboardName(),team);board.addPlayerToTeam(l.getEntity(ally).getScoreboardName(),team);
            for(int y=-60;y<=-56;y++)for(int z=7;z<=13;z++)l.setBlockAndUpdate(new BlockPos(4,y,z),Blocks.STONE.defaultBlockState());
            if(Boolean.getBoolean("uv.fugaNetwork")){
                try{p.server.setUsesAuthentication(false);p.server.getConnection().startTcpServerListener(java.net.InetAddress.getByName("127.0.0.1"),25576);require(true,"loopback-only network listener starts");}
                catch(java.io.IOException ex){throw new RuntimeException(ex);}
                try{Files.writeString(Path.of(System.getProperty("uv.readyFile")),"ready");}catch(Exception ex){throw new RuntimeException(ex);}
            }
        });
        if(ticks==40)key(GLFW.GLFW_KEY_H);
        if(ticks==76)capture(mc,"combat-hands-full-draw.png");
        if(ticks==82)require(FugaClient.handProgress()==1,"first-person full draw remains active");
        if(ticks==82&&Boolean.getBoolean("uv.shaderTest")){
            try{
                Object manager=Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager").invoke(null);
                Object pipeline=manager.getClass().getMethod("getPipelineNullable").invoke(manager);
                require(pipeline!=null&&pipeline.getClass().getSimpleName().equals("IrisRenderingPipeline"),"real shader pipeline is active rather than fallback");
            }catch(ReflectiveOperationException error){require(false,"shader pipeline introspection: "+error);}
        }
        if(ticks==98)capture(mc,"combat-hands-release.png");
        if(ticks==140)server(mc,p->{
            LogUtils.getLogger().info("FUGA_COMBAT_HITS {}",hits);
            require(hits.getOrDefault(victim,0)==1,"ordinary Fuga hits target once");
            require(hits.getOrDefault(ally,0)==0,"team protection");require(hits.getOrDefault(far,0)==0,"visual firestorm does not imply ordinary 200-block damage");
            require(hits.getOrDefault(blocked,0)==0,"wall blocks ordinary fire blast");
            require(p.serverLevel().getBlockState(new BlockPos(4,-59,10)).is(Blocks.STONE),"no terrain destruction");
            require(p.getHealth()==p.getMaxHealth(),"caster protected");
            require(hits.getOrDefault(pet,0)==0,"owned pet protected");
            require(((net.minecraft.world.entity.LivingEntity)p.serverLevel().getEntity(fireResistant)).getHealth()==100,"fire resistance respected");
        });
        if(ticks==142)require(launches==1&&impacts==1,"owner launch and impact audio each trigger once");
        if((ticks==150||ticks==155)&&Boolean.getBoolean("uv.fugaNetwork"))server(mc,p->{
            if(lateStart==0)lateStart=p.serverLevel().getGameTime()-FugaTimeline.CHARGE-3-20;
            for(var other:p.serverLevel().players())if(other!=p)dev.unlimitedvoid.network.DomainNetwork.fugaTo(other,LATE_OWNER,1,new Vec3(0,-59,0),new Vec3(0,-59,12),17,3,lateStart);
        });
        if(ticks==220)server(mc,p->{require(hits.getOrDefault(victim,0)==1,"no duplicate damage during aftermath");for(UUID id:new UUID[]{victim,far,ally,blocked,pet,fireResistant}){var e=p.serverLevel().getEntity(id);if(e!=null)e.discard();}});
        if(ticks==240)server(mc,p->{
            for(int y=-60;y<=-56;y++)for(int z=7;z<=13;z++)p.serverLevel().setBlockAndUpdate(new BlockPos(4,y,z),Blocks.AIR.defaultBlockState());
            DomainService.generate(p,DomainKind.SHRINE,p.position().add(0,1,0),new Vec3(0,0,1));
            disabledVictim=pig(p,0,10,100);DomainConfig.FUGA_COMBAT.set(false);FugaService.request(p);
        });
        if(ticks==300){
            groundCase=new FugaClient.Effect(new UUID(900,1),Vec3.ZERO,new Vec3(0,-59,0),1,mc.level.getGameTime());
            Vec3 sample=groundCase.ground(mc.level)[0];
            server(mc,p->{
                var pos=BlockPos.containing(sample.x,-60,sample.z);
                p.serverLevel().setBlockAndUpdate(pos,Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.BOTTOM));
                p.serverLevel().setBlockAndUpdate(new BlockPos(pos.getX(),-10,pos.getZ()),Blocks.STONE.defaultBlockState());
            });
        }
        if(ticks==314){groundCase.resolveGround(mc.level,1);require(Math.abs(groundCase.ground(mc.level)[0].y-(-59.38))<.01,"fire tiles follow slab surface below cave roof");}
        if(ticks==350)server(mc,p->{require(hits.getOrDefault(disabledVictim,0)==0,"combat config disables Fuga damage");var e=p.serverLevel().getEntity(disabledVictim);if(e!=null)e.discard();DomainConfig.FUGA_COMBAT.set(true);});
        if(ticks==265)server(mc,p->{DomainService.generate(p,DomainKind.VOID,new Vec3(24,-59,40),new Vec3(0,0,1));});
        if(ticks==510)server(mc,p->{
            require(DomainService.fugaDust(p.getUUID())==100,"own active Shrine accumulates powder");
            domainVictim=pig(p,0,70,200);protectedVictim=pig(p,24,40,200);
            for(UUID id:new UUID[]{domainVictim,protectedVictim})p.serverLevel().getEntity(id).getPersistentData().putDouble("uv_cursed_energy",1000000);
            // Fixed impact before the far domain target; unlike ordinary Fuga, powder traverses the district.
            p.serverLevel().setBlockAndUpdate(new BlockPos(0,-59,12),Blocks.STONE.defaultBlockState());
            p.serverLevel().setBlockAndUpdate(new BlockPos(0,-58,12),Blocks.STONE.defaultBlockState());
            FugaService.resetCooldown(p);
        });
        if(ticks==514)key(GLFW.GLFW_KEY_H);
        if(ticks==584)server(mc,p->{
            require(DomainService.fugaDust(p.getUUID())<100,"powder consumed at actual impact");
            require(hits.getOrDefault(protectedVictim,0)==0,"complete external Void barrier shelters its inside target");
        });
        if(ticks==612)server(mc,p->{require(hits.getOrDefault(domainVictim,0)==1,"powder ignition reaches beyond ordinary damage radius");});
        if(ticks==620){
            server(mc,p->p.teleportTo(p.serverLevel(),30,-50,-20,35,-12));
            // Actual production renderer with 8 simultaneous server-equivalent received effects.
            for(int i=0;i<8;i++)FugaClient.receive(new UUID(800,i),1,new Vec3(i*3,-59,8),new Vec3(i*3,-59,24),i,3,mc.level.getGameTime()-FugaTimeline.CHARGE-3);
        }
        if(ticks==640)capture(mc,"eight-blasts-near.png");
        if(ticks==650)server(mc,p->{p.teleportTo(p.serverLevel(),40,-35,-35,27,-20);});
        if(ticks==672)capture(mc,"eight-blasts-distant.png");
        if(ticks==712){require(maxQuads<=FugaRenderBudget.MAX_QUADS,"eight effects respect global quad ceiling");LogUtils.getLogger().info("FUGA_PERF frames={} meanFrameMs={} maxQuads={} fps={}",frames,frames>1?renderNanos/1e6/(frames-1):0,maxQuads,mc.getFps());}
        if(ticks==720)server(mc,p->{
            var ids=new ArrayList<UUID>();for(int i=0;i<96;i++)ids.add(pig(p,38+(i%12)*.35,-37+(i/12)*.35,1000));
            var struck=new HashSet<UUID>();Vec3 center=new Vec3(40,-59,-35);long begin=System.nanoTime();
            int first=FugaCombat.pulse(p,p.getEyePosition(),center,32,null,struck);
            int second=FugaCombat.pulse(p,p.getEyePosition(),center,32,null,struck);
            int third=FugaCombat.pulse(p,p.getEyePosition(),center,32,null,struck);
            double millis=(System.nanoTime()-begin)/1e6;
            require(first==32&&second==32&&third==32&&struck.size()==96,"96 living targets obey per-pulse budget and one-hit deduplication");
            LogUtils.getLogger().info("FUGA_SERVER_PERF targets=96 pulses=3 totalMs={}",millis);
            for(UUID id:ids){var e=p.serverLevel().getEntity(id);if(e!=null)e.discard();}
        });
        if(ticks==750){
            require(mc.getSingleplayerServer()!=null,"integrated server remains healthy after full combat cycle");
            LogUtils.getLogger().info("FUGA_COMBAT_TEST_COMPLETE shader={} network={}",Boolean.getBoolean("uv.shaderTest"),Boolean.getBoolean("uv.fugaNetwork"));mc.stop();
        }
    }
    static void observer(Minecraft mc){
        if(ticks==50)capture(mc,"network-observer-charge.png");
        if(ticks==140){require(mc.getSingleplayerServer()==null,"observer is a real remote network client");require(!mc.level.players().isEmpty(),"remote player list received");}
        if(ticks==180){
            try{var field=FugaClient.class.getDeclaredField("EFFECTS");field.setAccessible(true);var effects=(Map<?,?>)field.get(null);require(!effects.isEmpty(),"observer received active synchronized effects");
                var late=(FugaClient.Effect)effects.get(LATE_OWNER);require(late!=null&&late.age(mc.level,0)>FugaTimeline.CHARGE+late.flight+20&&late.impactSoundPlayed,"late and duplicate packets retain authoritative age without replaying impact");
                require(launches==1&&impacts==1,"remote audio is not repeated by late-state snapshots");}
            catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        }
        if(ticks==210)capture(mc,"network-observer-burst.png");
        if(ticks==260){LogUtils.getLogger().info("FUGA_NETWORK_OBSERVER_COMPLETE");mc.stop();}
    }
    static void connect(Minecraft mc){connectionAttempts++;ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25576"),new ServerData("Fuga isolated network QA","127.0.0.1:25576",false),false);}
    static UUID pig(ServerPlayer p,double x,double z,float health){
        Pig e=EntityType.PIG.create(p.serverLevel());e.moveTo(x,-60,z,0,0);e.setNoAi(true);e.setPersistenceRequired();e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);e.setHealth(health);p.serverLevel().addFreshEntity(e);return e.getUUID();
    }
    static void require(boolean ok,String message){if(!ok){failed=true;LogUtils.getLogger().error("FUGA_COMBAT_FAIL {}",message);throw new AssertionError(message);}LogUtils.getLogger().info("FUGA_COMBAT_PASS {}",message);}
    static void key(int key){KeyMapping.click(com.mojang.blaze3d.platform.InputConstants.getKey(key,0));}
    static void server(Minecraft mc,Consumer<ServerPlayer> action){var s=mc.getSingleplayerServer();UUID id=mc.player.getUUID();s.execute(()->{try{action.accept(s.getPlayerList().getPlayer(id));}catch(Throwable error){failed=true;LogUtils.getLogger().error("FUGA_COMBAT_FAIL",error);}});}
    static void capture(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->LogUtils.getLogger().info("FUGA_COMBAT_CAPTURE {}",name));}
}
