package dev.unlimitedvoid.client;

import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.server.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Opt-in fresh-world combat checks and production-renderer screenshots. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class DomainSealVisualTest {
    static boolean started;static volatile boolean failed;static int ticks;
    static final Map<UUID,Float> damage=new ConcurrentHashMap<>();
    static final Map<UUID,Integer> hits=new ConcurrentHashMap<>();
    static UUID near,far,edge,outside;
    @SubscribeEvent public static void hurt(LivingHurtEvent e){
        if(Boolean.getBoolean("uv.sealTest")&&e.getSource().getMsgId().equals("unlimitedvoid.fuga")){
            damage.merge(e.getEntity().getUUID(),e.getAmount(),Float::sum);hits.merge(e.getEntity().getUUID(),1,Integer::sum);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("uv.sealTest")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(failed){mc.stop();return;}
        if(!started&&mc.screen instanceof TitleScreen){
            started=true;mc.createWorldOpenFlows().createFreshLevel("Seals-Disposable-"+System.currentTimeMillis(),
                new LevelSettings("Domain seal QA",GameType.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }
        if(mc.player==null||mc.level==null||mc.screen!=null)return;ticks++;
        if(ticks==20){mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);server(mc,p->{
            var l=p.serverLevel();l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);
            l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);l.setDayTime(6000);
            p.teleportTo(l,0,-60,0,0,0);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));
            require(DomainConfig.FUGA_RADIUS.get()==200&&DomainConfig.FUGA_DAMAGE.get()==240&&DomainConfig.FUGA_DUST_DAMAGE.get()==480,"legacy defaults migrate to 200/240/480");
            require(DomainConfig.FUGA_BALANCE_REVISION.get()==1&&!DomainConfig.TERRAIN_DAMAGE.get(),"migration preserves terrain switch");
            near=pig(p,3,12);far=pig(p,0,120);edge=pig(p,0,208);outside=pig(p,0,220);
        });}
        if(ticks==35)server(mc,p->{
            var fixtures=Set.of(near,far,edge,outside);
            for(var e:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(-220,-64,-210,220,0,240)))
                if(e!=p&&!fixtures.contains(e.getUUID()))e.discard();
            var struck=new HashSet<UUID>();var impact=new Vec3(0,-59,10);
            int count=FugaCombat.pulse(p,p.getEyePosition(),impact,32,null,struck);
            LogUtils.getLogger().info("DOMAIN_SEAL_PULSE count={} damage={} edge={} candidates={}",count,damage,p.serverLevel().getEntity(edge),p.serverLevel().getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(-5,-64,0,5,-50,240)).size());
            require(struck.containsAll(List.of(near,far,edge))&&count<=FugaCombatRules.HIT_BUDGET,"full wave reaches 198 blocks within hit budget");
            FugaCombat.pulse(p,p.getEyePosition(),impact,36,null,struck);
            require(hits.getOrDefault(near,0)==1&&hits.getOrDefault(far,0)==1&&hits.getOrDefault(edge,0)==1,"second pulse deduplicated");
            require(damage.getOrDefault(near,0f)>230,"near damage exceeds 230 HP");
            require(damage.getOrDefault(far,0f)>130,"110-block damage exceeds 130 HP");
            require(damage.getOrDefault(edge,0f)>=60,"198-block damage at least 60 HP");
            require(!hits.containsKey(outside),"outside target spared");
            LogUtils.getLogger().info("DOMAIN_SEAL_DAMAGE near={} far={} edge={}",damage.get(near),damage.get(far),damage.get(edge));
            for(var id:new UUID[]{near,far,edge,outside}){var e=p.serverLevel().getEntity(id);if(e!=null)e.discard();}
        });
        if(ticks==40)key(GLFW.GLFW_KEY_U);
        if(ticks==70){
            checkSeal(mc,DomainKind.VOID);capture(mc,"void-seal-first.png");
            if(Boolean.getBoolean("uv.shaderTest"))try{
                var manager=Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager").invoke(null);
                var pipeline=manager.getClass().getMethod("getPipelineNullable").invoke(manager);
                require(pipeline!=null&&pipeline.getClass().getSimpleName().equals("IrisRenderingPipeline"),"real shader pipeline active");
            }catch(ReflectiveOperationException e){require(false,"shader introspection: "+e);}
        }
        if(ticks==80)mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if(ticks==90)capture(mc,"void-seal-third.png");
        if(ticks==100)mc.options.setCameraType(CameraType.FIRST_PERSON);
        if(ticks==207){require(DomainSeals.of(mc.player.getUUID(),0)==null,"Gojo seal releases after chant");capture(mc,"void-seal-released.png");}
        if(ticks==215)key(GLFW.GLFW_KEY_U);
        if(ticks==260)key(GLFW.GLFW_KEY_K);
        if(ticks==270)key(GLFW.GLFW_KEY_J);
        if(ticks==280)key(GLFW.GLFW_KEY_U);
        if(ticks==310){checkSeal(mc,DomainKind.SHRINE);capture(mc,"shrine-seal-first.png");}
        if(ticks==320)mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if(ticks==330)capture(mc,"shrine-seal-third.png");
        if(ticks==340){mc.options.setCameraType(CameraType.FIRST_PERSON);key(GLFW.GLFW_KEY_U);}
        if(ticks==355){require(DomainSeals.of(mc.player.getUUID(),0)==null,"cancel clears seal");require(mc.player.getMainHandItem().is(Items.DIAMOND_SWORD),"held equipment unchanged");capture(mc,"shrine-seal-cancelled.png");}
        if(ticks==365){LogUtils.getLogger().info("DOMAIN_SEAL_TEST_COMPLETE");mc.stop();}
    }
    static void checkSeal(Minecraft mc,DomainKind kind){var s=DomainSeals.of(mc.player.getUUID(),0);require(s!=null&&s.kind()==kind&&s.weight()==1,"full-weight "+kind+" seal");}
    static UUID pig(ServerPlayer p,double x,double z){
        var l=p.serverLevel();int cx=(int)Math.floor(x/16),cz=(int)Math.floor(z/16);for(int pathZ=0;pathZ<=cz;pathZ++){l.setChunkForced(cx,pathZ,true);l.getChunk(cx,pathZ);}
        var e=EntityType.PIG.create(l);e.moveTo(x,-60,z,0,0);e.setNoAi(true);e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);e.setHealth(1000);l.addFreshEntity(e);return e.getUUID();
    }
    static void require(boolean ok,String message){if(!ok){failed=true;LogUtils.getLogger().error("DOMAIN_SEAL_FAIL {}",message);throw new AssertionError(message);}LogUtils.getLogger().info("DOMAIN_SEAL_PASS {}",message);}
    static void server(Minecraft mc,Consumer<ServerPlayer> a){var s=mc.getSingleplayerServer();var id=mc.player.getUUID();s.execute(()->{try{a.accept(s.getPlayerList().getPlayer(id));}catch(Throwable e){failed=true;LogUtils.getLogger().error("DOMAIN_SEAL_FAIL",e);}});}
    static void key(int key){KeyMapping.click(com.mojang.blaze3d.platform.InputConstants.getKey(key,0));}
    static void capture(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->LogUtils.getLogger().info("DOMAIN_SEAL_CAPTURE {}",name));}
}
