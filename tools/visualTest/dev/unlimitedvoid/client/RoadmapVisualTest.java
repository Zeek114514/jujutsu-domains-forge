package dev.unlimitedvoid.client;

import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.*;
import dev.unlimitedvoid.network.*;
import dev.unlimitedvoid.server.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class RoadmapVisualTest {
    static boolean started,failed;static int connections;static int ticks,warmup,menu,remoteTicks;static JujutsuNpc actor,other,keeper;
    static UUID arenaActor;static double followX;static long stressStart=-1,serverStart;static int samples;static double totalMs,maxMs;static final List<Double> tickMs=new ArrayList<>();
    static boolean remoteSeen,stressDone;static int wave;static int observerStage;static long frameAt;static final List<Double> frameMs=Collections.synchronizedList(new ArrayList<>());
    static final Path READY=Path.of(System.getProperty("uv.readyFile","roadmap-ready"));
    static void check(boolean ok,String text){if(!ok){failed=true;throw new AssertionError("ROADMAP_FAIL "+text);}LogUtils.getLogger().info("ROADMAP_PASS {}",text);}
    static void server(Minecraft mc,Consumer<ServerPlayer> work){mc.getSingleplayerServer().execute(()->{try{work.accept(mc.getSingleplayerServer().getPlayerList().getPlayers().stream().filter(p->p.getGameProfile().getName().equals("FugaHost")).findFirst().orElseThrow());}catch(Throwable ex){failed=true;LogUtils.getLogger().error("ROADMAP_FAIL server",ex);mc.execute(mc::stop);}});}
    static JujutsuNpc npc(ServerPlayer p,boolean gojo,double x,double z,boolean goals){
        var n=(gojo?DomainEntities.STUDENT_GOJO:DomainEntities.SUKUNA_16).get().create(p.serverLevel());n.moveTo(x,-60,z,gojo?0:180,0);n.setYBodyRot(gojo?0:180);n.setYHeadRot(gojo?0:180);
        if(!goals)try{for(var f:Mob.class.getDeclaredFields())if(f.getType()==GoalSelector.class){f.setAccessible(true);((GoalSelector)f.get(n)).removeAllGoals(g->true);}}catch(Exception ex){throw new RuntimeException(ex);}
        p.serverLevel().addFreshEntity(n);return n;
    }
    static void click(String label){var mc=Minecraft.getInstance();check(mc.screen instanceof NpcManagementScreen,"native NPC screen open before "+label);for(var child:mc.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(label)){b.onPress();return;}throw new AssertionError("ROADMAP_FAIL missing button "+label);}
    static void shot(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->{});}
    @SubscribeEvent public static void render(net.minecraftforge.client.event.RenderLevelStageEvent event){if(!Boolean.getBoolean("uv.roadmapTest")||event.getStage()!=net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;long now=System.nanoTime();if(frameAt>0&&ticks>350)frameMs.add((now-frameAt)/1e6);frameAt=now;}
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event){
        if(!Boolean.getBoolean("uv.roadmapTest"))return;
        if(event.phase==TickEvent.Phase.START){serverStart=System.nanoTime();return;}
        if(stressStart<0||stressDone)return;double ms=(System.nanoTime()-serverStart)/1e6;tickMs.add(ms);samples++;totalMs+=ms;maxMs=Math.max(maxMs,ms);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("uv.roadmapTest")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        boolean observer=Boolean.getBoolean("uv.fugaObserver");
        if(observer&&mc.screen instanceof DisconnectedScreen&&++menu%40==0){check(connections<3,"bounded observer reconnect");LogUtils.getLogger().info("ROADMAP_NETWORK_RETRY {}",mc.screen.getNarrationMessage().getString());connect(mc);}
        if(failed){mc.stop();return;}
        if(!started&&mc.screen instanceof TitleScreen){
            if(observer&&++menu<40)return;started=true;
            if(observer)connect(mc);
            else mc.createWorldOpenFlows().createFreshLevel("Roadmap-QA-"+System.currentTimeMillis(),new LevelSettings("Roadmap QA",GameType.CREATIVE,false,net.minecraft.world.Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }
        if(mc.level==null||mc.player==null)return;
        if(observer){observe(mc);return;}
        if(warmup++<140)return;ticks++;
        if(ticks==1)server(mc,p->{
            var level=p.serverLevel();level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);level.setDayTime(6000);p.setGameMode(GameType.CREATIVE);
            for(var mob:level.getEntitiesOfClass(Mob.class,new AABB(-100,-70,-100,100,100,100)))mob.discard();
            for(int x=-8;x<=30;x++)for(int z=-10;z<=18;z++)level.setBlockAndUpdate(new BlockPos(x,-61,z),Blocks.GRAY_CONCRETE.defaultBlockState());
            actor=npc(p,true,0,0,false);other=npc(p,false,0,2,false);actor.setOnGround(true);other.setOnGround(true);other.beginStrike(actor.getUUID());actor.getRandom().setSeed(4096);check(NpcTactics.tick(actor,other)&&actor.tactic()==1,"real windup triggers safe sidestep decision");
            p.teleportTo(level,0,-60,-3,0,0);
            if(Boolean.getBoolean("uv.fugaNetwork"))try{p.server.setUsesAuthentication(false);p.server.getConnection().startTcpServerListener(java.net.InetAddress.getByName("127.0.0.1"),25576);Files.writeString(READY,"ready");}catch(Exception ex){throw new RuntimeException(ex);}
        });
        if(ticks==20)server(mc,p->{check(actor.getX()*actor.getX()+actor.getZ()*actor.getZ()>.01,"tactical decision produces actual walking");actor.setPos(0,-60,-4);actor.setOnGround(true);actor.nextTacticAt=0;actor.tacticUntil=0;other.stagger(false);check(NpcTactics.tick(actor,other)&&actor.tactic()==3,"recovery opening triggers pursuit decision");
            var veteran=DomainEntities.AWAKENED_GOJO.get().create(p.serverLevel());veteran.moveTo(3,-60,0,0,0);veteran.setHealth(veteran.getMaxHealth()*.2f);veteran.setOnGround(true);p.serverLevel().addFreshEntity(veteran);other.setPos(3,-60,2);check(NpcTactics.tick(veteran,other)&&veteran.tactic()==2,"wounded reversal user chooses safe retreat");veteran.discard();
            actor.discard();other.discard();actor=npc(p,true,0,0,true);arenaActor=actor.getUUID();});
        if(ticks==28){check(CombatHud.focused() instanceof JujutsuNpc,"crosshair status card finds unobstructed actor");shot("roadmap-hud.png");}
        if(ticks==30)server(mc,p->NpcControlService.request(p,actor.getId(),9));
        if(ticks==40){shot("roadmap-npc-menu.png");click("原地待命");}
        if(ticks==50)server(mc,p->check(NpcControlService.mode(actor)==2&&actor.getNavigation().isDone(),"wait button authoritatively stops movement"));
        if(ticks==55)click("跟随我");
        if(ticks==65)server(mc,p->{check(NpcControlService.mode(actor)==1&&actor.getPersistentData().getUUID("uv_follow").equals(p.getUUID()),"follow button binds requester");followX=actor.getX();p.teleportTo(p.serverLevel(),4,-60,-2,45,0);});
        if(ticks==100)server(mc,p->check(Math.abs(actor.getX()-followX)>.15,"follow order physically follows player"));
        if(ticks==105)click("自由行动");
        if(ticks==115)server(mc,p->check(NpcControlService.mode(actor)==0,"free order restores combat policy"));
        if(ticks==120)click("红方");
        if(ticks==130)server(mc,p->{other=npc(p,false,actor.getX()+2,actor.getZ(),true);other.getPersistentData().putInt("uv_order",2);NpcControlService.request(p,other.getId(),5);check(NpcControlService.team(actor)==1&&NpcControlService.team(other)==2,"red blue team assignment actor="+NpcControlService.team(actor)+" other="+NpcControlService.team(other)+" distance="+p.distanceToSqr(other)+" LOS="+p.hasLineOfSight(other)+" tags="+other.getPersistentData());});
        if(ticks==140)click("开始对战");
        if(ticks==150)server(mc,p->{check(p.getPersistentData().getCompound("uv_arena").getList("Actors",10).size()==2,"arena stores bounded pre-battle snapshots");check(NpcControlService.canFight(other)&&NpcTechniqueService.enemy(actor,other),"arena releases both sides into combat");});
        if(ticks==190)click("重置对战");
        if(ticks==205)server(mc,p->{check(!p.getPersistentData().contains("uv_arena"),"arena reset consumes only its own session");actor=(JujutsuNpc)p.serverLevel().getEntity(arenaActor);check(actor!=null&&actor.getHealth()==actor.getMaxHealth()&&NpcControlService.team(actor)==1,"arena reset restores original identity health and team");for(var e:p.serverLevel().getEntitiesOfClass(JujutsuNpc.class,new AABB(-10,-65,-10,15,-40,10)))e.discard();});
        if(ticks==210){mc.setScreen(null);server(mc,p->{
            p.setGameMode(GameType.SURVIVAL);p.getPersistentData().remove("uv_progression");ProgressionService.data(p).putFloat("Energy",100);
            int before=LimitlessService.activeCount();LimitlessService.request(p,LimitlessKind.RED);check(LimitlessService.activeCount()==before,"survival cannot bypass locked technique by request");
            p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(DomainItems.BLINDFOLD.get()));net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND));p.setShiftKeyDown(false);
            check(ProgressionService.data(p).getInt("Path")==1&&p.getMainHandItem().isEmpty(),"real item interaction chooses and consumes initial path item");
            LimitlessService.request(p,LimitlessKind.BLUE);check(LimitlessService.activeCount()==before+1&&ProgressionService.data(p).getFloat("Energy")==88,"unlocked blue creates cast and spends exact energy");
            var data=ProgressionService.data(p);data.putInt("Xp",120);data.putFloat("Energy",100);check(ProgressionService.spend(p,1)&&data.getFloat("Energy")==76,"level four unlocks red and charges energy");
            data.putInt("Xp",280);data.putFloat("Energy",0);check(!ProgressionService.spend(p,2),"empty cursed energy rejects unlocked purple");data.putFloat("Energy",100);check(ProgressionService.spend(p,2),"level eight unlocks purple");
            var saved=new net.minecraft.nbt.CompoundTag();p.saveWithoutId(saved);check(saved.getCompound("ForgeData").contains("uv_progression"),"progression present in real player save NBT");
            var clone=new ServerPlayer(p.server,p.serverLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RoadmapClone"));ProgressionService.clone(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone,p,true));check(ProgressionService.xp(clone)==ProgressionService.xp(p),"death clone preserves progression");
            p.setGameMode(GameType.CREATIVE);data.putInt("Path",0);data.putFloat("Energy",0);check(ProgressionService.spend(p,4)&&data.getFloat("Energy")==0,"creative bypasses unlock and energy gates");ProgressionService.sync(p,false);
        });}
        if(ticks==235)ProgressionNetwork.request(true);
        if(ticks==250){check(mc.screen instanceof ProgressionClient.ProgressScreen,"P progression page arrives through network");shot("roadmap-progression.png");int original=ClientQuality.quality();for(int i=0;i<3;i++){check(ClientQuality.particlesPerTick()<=300&&ClientQuality.aliveParticles()<=4500,"quality profile remains bounded");ClientQuality.cycle();}check(ClientQuality.quality()==original,"all three quality profiles cycle and save");mc.setScreen(null);}
        if(ticks==270)server(mc,p->{keeper=npc(p,true,0,0,true);keeper.setCustomName(Component.literal("QA 管理角色"));keeper.getPersistentData().putInt("uv_order",0);keeper.getPersistentData().putInt("uv_team",1);
            var summon=DomainEntities.MAHORAGA.get().create(p.serverLevel());summon.moveTo(0,-60,4,0,0);p.serverLevel().addFreshEntity(summon);summon.bind(keeper);
            var blue=npc(p,true,3,4,false);blue.getPersistentData().putInt("uv_team",2);check(NpcControlService.team(summon)==1&&summon.enemy(blue),"summoned Mahoraga inherits custom team");keeper.getPersistentData().putInt("uv_order",2);check(!summon.enemy(blue),"summoner wait order suppresses companion combat");summon.discard();blue.discard();p.teleportTo(p.serverLevel(),0,-60,-4,0,0);});
        if(ticks>280&&ticks%10==0)server(mc,p->{
            var guests=p.server.getPlayerList().getPlayers().stream().filter(q->q!=p).toList();
            if(!remoteSeen&&!guests.isEmpty()){remoteSeen=true;for(var q:guests){q.setGameMode(GameType.SURVIVAL);q.getAbilities().invulnerable=true;q.onUpdateAbilities();q.teleportTo(p.serverLevel(),3,-60,-3,30,0);ProgressionService.sync(q,false);}}
            boolean ack=Files.exists(READY.resolveSibling("roadmap-observer-done"));
            if(remoteSeen&&ack&&stressStart<0){check(NpcControlService.mode(keeper)==2,"remote survival request cannot change admin NPC order");for(int i=0;i<16;i++){var n=npc(p,i%2==0,8+(i%4)*2,4+(i/4)*2,true);n.getPersistentData().putInt("uv_team",i%2==0?1:2);}for(var q:guests)q.teleportTo(p.serverLevel(),2,-59.5,-4,-45,8);stressStart=p.serverLevel().getGameTime();p.teleportTo(p.serverLevel(),19,-59.5,-7,25,8);}
            if(stressStart>=0&&!stressDone){
                int age=(int)(p.serverLevel().getGameTime()-stressStart);
                if(age/260>wave&&age<1250){wave=age/260;var victims=p.serverLevel().getEntitiesOfClass(JujutsuNpc.class,new AABB(-10,-65,-10,35,50,25));for(var n:victims)if(n!=keeper){NpcControlService.stop(n);n.discard();}for(int i=0;i<16;i++){var n=npc(p,i%2==0,8+(i%4)*2,4+(i/4)*2,true);n.getPersistentData().putInt("uv_team",i%2==0?1:2);}}
                if(age%260==20)FugaService.request(p);
                if(age%260==80)mc.execute(()->shot("roadmap-group-wave-"+wave+".png"));
            }
            if(stressStart>=0&&p.serverLevel().getGameTime()-stressStart>=1300&&!stressDone){stressDone=true;check(LimitlessService.activeCount()+NpcTechniqueService.activeCount()<=8&&DomainService.snapshot(p).size()<=8,"group battle respects technique and domain caps");check(FugaParticles.alive()<=ClientQuality.aliveParticles(),"client particles stay within profile cap");
                tickMs.sort(Double::compare);var frames=new ArrayList<Double>();synchronized(frameMs){frames.addAll(frameMs);}frames.sort(Double::compare);LogUtils.getLogger().info("ROADMAP_PERF ticks={} meanMs={} p95Ms={} maxMs={} frameP95Ms={}",samples,totalMs/Math.max(1,samples),tickMs.get(Math.max(0,(int)(tickMs.size()*.95)-1)),maxMs,frames.isEmpty()?0:frames.get(Math.max(0,(int)(frames.size()*.95)-1)));mc.execute(()->{shot("roadmap-group-battle.png");LogUtils.getLogger().info("ROADMAP_TEST_COMPLETE");mc.stop();});}
        });
        if(ticks>3400)throw new AssertionError("ROADMAP_FAIL timeout");
    }
    static void connect(Minecraft mc){connections++;ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25576"),new ServerData("Roadmap isolated QA","127.0.0.1:25576",false),false);}
    static void observe(Minecraft mc){
        remoteTicks++;ticks=remoteTicks;
        if(observerStage==4){if(remoteTicks>1240){LogUtils.getLogger().info("ROADMAP_OBSERVER_COMPLETE");mc.stop();}return;}
        if(mc.gameMode.getPlayerMode()!=GameType.SURVIVAL)return;
        var target=mc.level.entitiesForRendering();JujutsuNpc n=null;for(var e:target)if(e instanceof JujutsuNpc j&&j.getName().getString().equals("QA 管理角色")){n=j;break;}if(n==null)return;
        if(observerStage==0){check(mc.getSingleplayerServer()==null,"observer is real remote network client");NpcControlNetwork.request(n.getId(),0);observerStage=1;remoteTicks=0;}
        else if(observerStage==1&&remoteTicks>12){NpcControlNetwork.request(n.getId(),3);observerStage=2;remoteTicks=0;}
        else if(observerStage==2&&remoteTicks>15){NpcControlNetwork.request(n.getId(),0);observerStage=3;remoteTicks=0;}
        else if(observerStage==3&&remoteTicks>15){check(NpcManagementScreen.lastState!=null&&NpcManagementScreen.lastState.mode()==2,"remote authoritative order remains unchanged after unauthorized packet");check(n.getMaxHealth()==400,"remote actor attributes and form synchronized");shot("roadmap-network-observer.png");try{Files.writeString(READY.resolveSibling("roadmap-observer-done"),"pass");}catch(Exception ex){throw new RuntimeException(ex);}observerStage=4;remoteTicks=0;}
        else if(observerStage==4&&remoteTicks>1240){LogUtils.getLogger().info("ROADMAP_OBSERVER_COMPLETE");mc.stop();}
    }
}
