package dev.unlimitedvoid.client;

import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.*;
import dev.unlimitedvoid.server.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
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
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class NpcWeightVisualTest {
    static boolean started;static int warmup,ticks,scene,step,lastAge=-1,frame,requested,hits,naturalGojo,naturalSukuna;
    static JujutsuNpc actor,victim;static boolean staggerSeen;static float twist,left,right;static long naturalStart;
    static void check(boolean ok,String message){if(!ok)throw new AssertionError("NPC_WEIGHT_FAIL "+message);LogUtils.getLogger().info("NPC_WEIGHT_PASS {}",message);}
    static void server(Minecraft mc,Consumer<ServerPlayer> work){mc.getSingleplayerServer().execute(()->{try{work.accept(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0));}catch(Throwable error){LogUtils.getLogger().error("NPC_WEIGHT_FAIL server",error);mc.execute(mc::stop);}});}
    static void clear(ServerPlayer p){for(var e:p.serverLevel().getEntitiesOfClass(JujutsuNpc.class,new AABB(-20,-65,-20,20,-40,20))){NpcTechniqueService.cancel(e);DomainService.dismissNpc(e);e.discard();}}
    static JujutsuNpc npc(ServerPlayer p,EntityType<JujutsuNpc> type,double z,boolean noAi,boolean goals){
        var n=type.create(p.serverLevel());n.moveTo(0,-60,z,z==0?180:0,0);n.setYBodyRot(z==0?180:0);n.setYHeadRot(z==0?180:0);n.setNoAi(noAi);
        if(!goals)try{for(var f:Mob.class.getDeclaredFields())if(f.getType()==GoalSelector.class){f.setAccessible(true);((GoalSelector)f.get(n)).removeAllGoals(g->true);}}catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        n.getAttribute(Attributes.MAX_HEALTH).setBaseValue(5000);n.setHealth(5000);p.serverLevel().addFreshEntity(n);return n;
    }
    static void start(ServerPlayer p,int nextScene,int nextStep){
        scene=nextScene;step=nextStep;hits=0;lastAge=-1;requested=0;staggerSeen=false;twist=left=right=0;
        if(nextStep==0){clear(p);actor=npc(p,nextScene==1?DomainEntities.SHINJUKU_GOJO.get():DomainEntities.TRUE_SUKUNA.get(),0,false,false);victim=npc(p,nextScene==1?DomainEntities.TRUE_SUKUNA.get():DomainEntities.SHINJUKU_GOJO.get(),-1.8,true,false);}
        actor.setPos(0,-60,0);actor.setDeltaMovement(0,0,0);victim.setPos(0,-60,-1.8);victim.setDeltaMovement(0,0,0);victim.invulnerableTime=0;
        boolean ok=nextScene==1?NpcMeleeService.start(actor,victim,nextStep==2?LimitlessKind.RED:LimitlessKind.BLUE):nextStep==2?SukunaCombatService.startSlash(actor,victim):SukunaCombatService.punch(actor,victim);
        check(ok&&actor.comboStep()==nextStep,"starts real combo "+nextScene+" step "+nextStep);
        check(nextScene==1?!NpcTechniqueService.start(actor,LimitlessKind.BLUE,victim,false):!SukunaCombatService.startSlash(actor,victim),"normal technique cannot overlap melee recovery");
        p.teleportTo(p.serverLevel(),4.2,-59.8,-4.5,48,5);
    }
    @SubscribeEvent public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent e){
        if(!Boolean.getBoolean("uv.npcWeightTest"))return;
        if(scene==3&&e.getSource().getEntity() instanceof JujutsuNpc n){if(n.isGojo())naturalGojo++;else naturalSukuna++;return;}
        if(e.getSource().getEntity()!=actor||e.getEntity()!=victim)return;
        check(Math.abs(actor.punchAge(0)-NpcAttackMotion.contact(actor.comboStep(),actor.punchKind()))<=1,"actual damage matches shared contact frame");hits++;
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e){
        if(!Boolean.getBoolean("uv.npcWeightTest")||e.phase!=TickEvent.Phase.END||actor==null)return;
        if(scene<3&&victim.staggered()){staggerSeen=true;victim.setNoAi(false);check(victim.isGojo()?!NpcMeleeService.start(victim,actor,LimitlessKind.BLUE):!SukunaCombatService.punch(victim,actor),"stagger blocks immediate counterattack");victim.setNoAi(true);}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("uv.npcWeightTest")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(!started&&mc.screen instanceof TitleScreen){started=true;mc.createWorldOpenFlows().createFreshLevel("NPC-Weight-"+System.currentTimeMillis(),new LevelSettings("NPC Weight QA",GameType.CREATIVE,false,net.minecraft.world.Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());}
        if(mc.player==null||mc.level==null||mc.screen!=null)return;if(warmup++<160)return;ticks++;
        if(ticks==1){mc.options.hideGui=true;mc.options.fov().set(42);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);server(mc,p->{
            p.setGameMode(GameType.SPECTATOR);var level=p.serverLevel();level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);level.setDayTime(6000);p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTimePacket(level.getGameTime(),6000,false));
            for(int x=-15;x<15;x++)for(int z=-15;z<12;z++)level.setBlockAndUpdate(new BlockPos(x,-61,z),Blocks.GRAY_CONCRETE.defaultBlockState());start(p,1,0);
        });}
        if(actor==null)return;
        if(scene==3){
            if(ticks%2==0)Screenshot.grab(mc.gameDirectory,String.format("weight-%03d.png",frame++),mc.getMainRenderTarget(),c->{});
            if(mc.level.getGameTime()-naturalStart>=120&&requested!=99){requested=99;server(mc,p->{check(naturalGojo>0&&naturalSukuna>0,"both unmodified combat goals exchange real hits");check(actor.isAlive()&&victim.isAlive(),"arena remains a sustained exchange");LogUtils.getLogger().info("NPC_WEIGHT_TEST_COMPLETE frames={}",frame);mc.execute(mc::stop);});}return;
        }
        var visible=mc.level.getEntity(actor.getId());if(!(visible instanceof JujutsuNpc n))return;
        if(n.comboStep()!=step||lastAge<0&&n.punchKind()<0)return;
        int age=(int)n.punchAge(0);int duration=NpcAttackMotion.duration(step,scene==2&&step==2?4:0);
        if(age>=0&&age<duration&&age>lastAge){
            lastAge=age;Screenshot.grab(mc.gameDirectory,String.format("weight-%03d.png",frame++),mc.getMainRenderTarget(),c->{});
            var model=((JujutsuNpcRenderer)mc.getEntityRenderDispatcher().getRenderer(n)).getModel();
            twist=Math.max(twist,Math.abs(model.body.yRot));left=Math.max(left,Math.abs(model.leftArm.xRot));right=Math.max(right,Math.abs(model.rightArm.xRot));
        }
        if(age>=duration+3&&requested==0){requested=1;float t=twist,l=left,r=right;int currentScene=scene,currentStep=step;
            server(mc,p->{
                check(hits==1,"one real hit per attack");check(currentStep==1?!staggerSeen:staggerSeen,currentStep==1?"anti-stunlock guard prevents consecutive hit locks":"successful hit causes short synchronized stagger");check(t>.25&&(l>1||r>1),"rendered shoulder turn and extended arm");
                if(currentStep<2)start(p,currentScene,currentStep+1);
                else if(currentScene==1){check(NpcTechniqueService.start(actor,LimitlessKind.BLUE,victim,false),"technique can follow completed combo recovery");NpcTechniqueService.cancel(actor);start(p,2,0);}
                else{
                    check(!actor.staggered(),"attacker remains active after combo");
                    clear(p);actor=npc(p,DomainEntities.STUDENT_GOJO.get(),0,false,true);victim=npc(p,DomainEntities.SUKUNA_16.get(),-2,false,true);scene=3;naturalStart=p.serverLevel().getGameTime();requested=0;
                    p.teleportTo(p.serverLevel(),5,-59.8,-5,48,5);
                }
            });
        }
        if(ticks>900)throw new AssertionError("NPC_WEIGHT_FAIL timeout");
    }
}
