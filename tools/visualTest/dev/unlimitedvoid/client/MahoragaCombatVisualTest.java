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
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.resources.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.Consumer;
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class MahoragaCombatVisualTest {
    static boolean started;static int warmup,ticks,voidHits,ownedHits;static MahoragaEntity actor;static JujutsuNpc gojo,owner;static Husk victim;static UUID summonId;
    static void require(boolean value,String message){if(!value)throw new AssertionError("MAHORAGA_COMBAT_FAIL "+message);LogUtils.getLogger().info("MAHORAGA_COMBAT_PASS {}",message);}
    static void server(Minecraft mc,Consumer<ServerPlayer> work){mc.getSingleplayerServer().execute(()->{try{work.accept(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0));}catch(Throwable error){LogUtils.getLogger().error("MAHORAGA_COMBAT_FAIL server",error);mc.execute(mc::stop);}});}
    static MahoragaEntity mahoraga(ServerPlayer p,double x,double z,boolean noAi){var m=DomainEntities.MAHORAGA.get().create(p.serverLevel());m.moveTo(x,-60,z,180,0);m.setNoAi(noAi);m.getAttribute(Attributes.ARMOR).setBaseValue(0);p.serverLevel().addFreshEntity(m);return m;}
    static JujutsuNpc npc(ServerPlayer p,EntityType<JujutsuNpc> type,double x,double z,boolean noAi){var n=type.create(p.serverLevel());n.moveTo(x,-60,z,0,0);n.setNoAi(noAi);p.serverLevel().addFreshEntity(n);return n;}
    static Husk husk(ServerPlayer p,double x,double z){var h=new Husk(EntityType.HUSK,p.serverLevel());h.moveTo(x,-60,z,0,0);h.setNoAi(true);h.setNoGravity(false);h.setPersistenceRequired();h.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);h.setHealth(1000);p.serverLevel().addFreshEntity(h);return h;}
    static DamageSource source(ServerPlayer p,String name){var key=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation(UnlimitedVoid.ID,name));return new DamageSource(p.serverLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key),p,p);}
    static void clear(ServerPlayer p){for(var e:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(-80,-70,-80,80,-40,80),e->e!=p)){if(e instanceof JujutsuNpc n){DomainService.dismissNpc(n);NpcTechniqueService.cancel(n);FugaService.cancel(n.getUUID());}e.discard();}}
    @SubscribeEvent public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event){if(!(Boolean.getBoolean("uv.mahoragaCombatTest")||Boolean.getBoolean("uv.mahoragaOwnedTest")))return;if(event.getSource().getEntity() instanceof MahoragaEntity&&event.getEntity()==victim){if(ticks>=1400&&ticks<=1450)voidHits++;if(ticks>=1525)ownedHits++;}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!(Boolean.getBoolean("uv.mahoragaCombatTest")||Boolean.getBoolean("uv.mahoragaOwnedTest"))||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(!started&&mc.screen instanceof TitleScreen){started=true;mc.createWorldOpenFlows().createFreshLevel("Mahoraga-Combat-QA-"+System.currentTimeMillis(),new LevelSettings("Mahoraga Combat QA",GameType.CREATIVE,false,net.minecraft.world.Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());}
        if(mc.player==null||mc.level==null||mc.screen!=null)return;if(warmup++<200)return;if(Boolean.getBoolean("uv.mahoragaOwnedTest")&&ticks==0)ticks=1524;ticks++;
        if(ticks==5)server(mc,p->{p.serverLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);p.serverLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);p.serverLevel().setDayTime(6000);clear(p);p.teleportTo(p.serverLevel(),0,-60,-8,0,0);});
        if(ticks==10)server(mc,p->{actor=mahoraga(p,0,0,true);require(actor.getHealth()==1000&&actor.getMaxHealth()==1000,"dedicated 1000HP summon");require(actor.hurt(source(p,"blue"),100)&&actor.getHealth()==900,"first Blue hit takes full damage before adaptation");require(actor.adaptation(MahoragaRules.Kind.BLUE)==0,"first hit grants no instant immunity");});
        if(ticks>=20&&ticks<=260&&ticks%10==0)server(mc,p->{actor.invulnerableTime=0;actor.hurt(source(p,"blue"),2);});
        if(ticks==55)server(mc,p->require(actor.adaptation(MahoragaRules.Kind.BLUE)==0,"repeated exposure still needs time"));
        if(ticks==105)server(mc,p->require(actor.adaptation(MahoragaRules.Kind.BLUE)>0&&actor.adaptation(MahoragaRules.Kind.BLUE)<4,"Blue progresses gradually"));
        if(ticks==290)server(mc,p->{require(actor.adaptation(MahoragaRules.Kind.BLUE)==4,"repeated real Blue damage completes adaptation");require(actor.forceFactor(MahoragaRules.Kind.BLUE)==0,"full Blue adaptation cancels attraction");require(actor.adaptation(MahoragaRules.Kind.RED)==0&&actor.adaptation(MahoragaRules.Kind.PURPLE)==0,"other techniques do not inherit Blue immunity");actor.invulnerableTime=0;float hp=actor.getHealth();actor.hurt(source(p,"red"),100);require(Math.abs(actor.getHealth()-(hp-100))<.01,"unseen Red still deals full damage");actor.invulnerableTime=0;hp=actor.getHealth();actor.hurt(source(p,"blue"),100);require(Math.abs(actor.getHealth()-(hp-10))<.01,"adapted Blue still deals ten percent damage");var tag=new net.minecraft.nbt.CompoundTag();actor.save(tag);tag.remove("UUID");var copy=(MahoragaEntity)EntityType.loadEntityRecursive(tag,p.serverLevel(),e->e);require(copy!=null&&copy.adaptation(MahoragaRules.Kind.BLUE)==4&&copy.adaptation(MahoragaRules.Kind.RED)==0,"independent adaptation survives NBT");require(copy.getHealth()==actor.getHealth(),"NBT does not refill health");copy.hurt(copy.damageSources().genericKill(),Float.MAX_VALUE);require(!copy.isAlive(),"kill bypass cannot be adapted away");var fresh=mahoraga(p,8,0,true);fresh.hurt(source(p,"purple"),5000);require(!fresh.isAlive()&&fresh.adaptation(MahoragaRules.Kind.PURPLE)==0,"lethal first Purple kills instead of adapting or reviving");clear(p);});
        if(ticks==350)server(mc,p->{actor=mahoraga(p,0,0,false);gojo=npc(p,DomainEntities.SHINJUKU_GOJO.get(),0,2,true);});
        if(ticks==400)server(mc,p->{require(gojo.getHealth()==gojo.getMaxHealth(),"Infinity initially blocks Mahoraga contact");require(actor.adaptation(MahoragaRules.Kind.INFINITY)<4,"Infinity cannot be bypassed on first contact");});
        if(ticks==850)server(mc,p->{require(actor.adaptation(MahoragaRules.Kind.INFINITY)==4,"natural failed attacks teach Infinity adaptation");require(gojo.getHealth()<gojo.getMaxHealth(),"adapted Mahoraga actually hits Infinity protected Gojo");gojo.invulnerableTime=0;float hp=gojo.getHealth();MahoragaService.spaceSlash(actor,gojo);require(gojo.getHealth()<hp,"adapted spatial cut causes actual damage");require(p.getHealth()==20,"Mahoraga does not attack player observer");clear(p);});
        if(ticks==880)server(mc,p->{for(int y=-60;y<=-54;y++)for(int z=-5;z<=5;z++)p.serverLevel().setBlockAndUpdate(new BlockPos(0,y,z),Blocks.STONE.defaultBlockState());gojo=npc(p,DomainEntities.AWAKENED_GOJO.get(),-4,0,false);actor=mahoraga(p,4,0,true);require(DomainService.expandNpc(gojo,true),"Void starts around fresh Mahoraga");});
        if(ticks==1150)server(mc,p->{require(DomainService.isCaptive(actor.getUUID()),"fresh Mahoraga really suffers Void paralysis");require(actor.adaptation(MahoragaRules.Kind.VOID)<4,"Void paralysis is not immediately immune");});
        if(ticks==1400)server(mc,p->{require(actor.adaptation(MahoragaRules.Kind.VOID)==4,"wheel adapts while actor is immobilized");require(!DomainService.isCaptive(actor.getUUID())&&actor.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(DomainService.STASIS_MOVEMENT)==null,"adapted Void releases stasis without ending domain");actor.setNoAi(false);require(actor.canAct(),"actor can fight inside active Void");victim=husk(p,4,6);});
        if(ticks==1450)server(mc,p->{require(voidHits>0,"adapted actor actually attacks from inside Void");require(ClosedBarrier.contains(actor.getUUID()),"adaptation does not delete physical closed barrier");gojo.setNoAi(true);clear(p);for(int y=-60;y<=-54;y++)for(int z=-5;z<=5;z++)p.serverLevel().setBlockAndUpdate(new BlockPos(0,y,z),Blocks.AIR.defaultBlockState());});
        if(ticks==1525)server(mc,p->{p.serverLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);p.serverLevel().setDayTime(6000);clear(p);require(DomainService.snapshot(p).isEmpty(),"Void cleans up before summon stage");owner=npc(p,DomainEntities.MEGUMI_SUKUNA.get(),0,0,false);victim=husk(p,0,10);require(SukunaCombatService.summon(owner,victim),"Megumi creates a real bound Mahoraga");summonId=owner.getPersistentData().getUUID("uv_mahoraga_uuid");require(p.serverLevel().getEntity(summonId) instanceof MahoragaEntity,"summon is a persistent entity not an effect placeholder");actor=(MahoragaEntity)p.serverLevel().getEntity(summonId);require(actor.ownerId().orElseThrow().equals(owner.getUUID()),"summon binds owner UUID");require(actor.emerging()&&!actor.canAct()&&owner.summoningMahoraga(),"shadow emergence and summoner pose precede combat");require(!SukunaCombatService.summon(owner,victim),"one living summon per owner");var tag=new net.minecraft.nbt.CompoundTag();owner.save(tag);tag.remove("UUID");var copy=(JujutsuNpc)EntityType.loadEntityRecursive(tag,p.serverLevel(),e->e);require(copy.getPersistentData().hasUUID("uv_mahoraga_uuid"),"used summon binding persists in owner NBT");p.setGameMode(GameType.SPECTATOR);p.teleportTo(p.serverLevel(),5,-60,-9,42,-5);});
        if(ticks==1550){mc.options.hideGui=true;mc.options.fov().set(55);Screenshot.grab(mc.gameDirectory,"mahoraga-shadow-emergence.png",mc.getMainRenderTarget(),c->{});}
        if(ticks==1595)server(mc,p->require(!actor.emerging()&&!owner.summoningMahoraga()&&actor.canAct(),"fully emerged summon resumes its own AI"));
        if(ticks==1610||ticks==1640)server(mc,p->LogUtils.getLogger().info("MAHORAGA_OWNED_DIAG pos={} target={} canAct={} attack={} victimHP={} hits={}",actor.position(),actor.getTarget(),actor.canAct(),actor.attackType(),victim.getHealth(),ownedHits));
        if(ticks==1665)server(mc,p->{require(ownedHits>0,"bound summon independently fights summoner enemy pos="+actor.position()+" target="+actor.getTarget()+" hp="+victim.getHealth());owner.setNoAi(true);require(!actor.canAct(),"NoAI owner pauses companion combat");owner.setNoAi(false);});
        if(ticks==1670)server(mc,p->{owner.hurt(owner.damageSources().genericKill(),Float.MAX_VALUE);require(!owner.isAlive(),"owner death bypasses transformation");});
        if(ticks==1710)server(mc,p->require(actor.isRemoved(),"owner death withdraws summon through shadow fade"));
        if(ticks==1740){if(Boolean.getBoolean("uv.shaderTest"))try{var manager=Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager").invoke(null);var pipeline=manager.getClass().getMethod("getPipelineNullable").invoke(manager);require(pipeline!=null&&pipeline.getClass().getSimpleName().equals("IrisRenderingPipeline"),"real shader pipeline active");}catch(ReflectiveOperationException e){throw new RuntimeException(e);}require(mc.getResourceManager().getResource(new ResourceLocation(UnlimitedVoid.ID,"sounds/mahoraga_turn.ogg")).isPresent(),"original wheel click asset present");LogUtils.getLogger().info("MAHORAGA_COMBAT_TEST_COMPLETE");mc.stop();}
    }
}
