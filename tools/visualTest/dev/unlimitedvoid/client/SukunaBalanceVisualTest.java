package dev.unlimitedvoid.client;
import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.*;
import dev.unlimitedvoid.server.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.Consumer;
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class SukunaBalanceVisualTest {
    static boolean started;static int ticks,warmup;static JujutsuNpc gojo,sukuna,student;static Husk victim;static Vec3 position;static float health,damage;static int roofMelee;static final List<JujutsuNpc> attackers=new ArrayList<>(),targets=new ArrayList<>();
    static void require(boolean value,String message){if(!value)throw new AssertionError("SUKUNA_BALANCE_FAIL "+message);LogUtils.getLogger().info("SUKUNA_BALANCE_PASS {}",message);}
    static void server(Minecraft mc,Consumer<ServerPlayer> work){mc.getSingleplayerServer().execute(()->{try{work.accept(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0));}catch(Throwable error){LogUtils.getLogger().error("SUKUNA_BALANCE_FAIL server",error);mc.execute(mc::stop);}});}
    static JujutsuNpc npc(ServerPlayer p,EntityType<JujutsuNpc> type,double x,double z,boolean noAi){var n=type.create(p.serverLevel());n.moveTo(x,-60,z,0,0);n.setNoAi(noAi);p.serverLevel().addFreshEntity(n);return n;}
    static Husk husk(ServerPlayer p,double x,double z){var h=new Husk(EntityType.HUSK,p.serverLevel());h.moveTo(x,-60,z,0,0);h.setNoAi(true);h.setNoGravity(true);h.setPersistenceRequired();h.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);h.setHealth(1000);p.serverLevel().addFreshEntity(h);return h;}
    static void clear(ServerPlayer p){for(var n:p.serverLevel().getEntitiesOfClass(JujutsuNpc.class,new AABB(-80,-70,-80,80,-40,80))){DomainService.dismissNpc(n);FugaService.cancel(n.getUUID());NpcTechniqueService.cancel(n);n.discard();}for(var h:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(-80,-70,-80,80,-40,80),e->e instanceof net.minecraft.world.entity.monster.Enemy))h.discard();}
    @SubscribeEvent public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event){if((Boolean.getBoolean("uv.sukunaBalanceTest")||Boolean.getBoolean("uv.sukunaArmyTest"))&&event.getEntity()==student&&event.getSource().getEntity() instanceof JujutsuNpc n&&!n.isGojo())damage+=event.getAmount();if(Boolean.getBoolean("uv.sukunaBalanceTest")&&ticks>=750&&ticks<=850&&event.getEntity()==victim&&event.getSource().getEntity()==sukuna&&event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK))roofMelee++;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!(Boolean.getBoolean("uv.sukunaBalanceTest")||Boolean.getBoolean("uv.sukunaArmyTest"))||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(!started&&mc.screen instanceof TitleScreen){started=true;mc.createWorldOpenFlows().createFreshLevel("Sukuna-Balance-QA-"+System.currentTimeMillis(),new LevelSettings("Sukuna Balance QA",GameType.CREATIVE,false,net.minecraft.world.Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());}
        if(mc.player==null||mc.level==null||mc.screen!=null)return;if(warmup++<200)return;if(Boolean.getBoolean("uv.sukunaArmyTest")&&ticks==0)ticks=909;ticks++;
        if(ticks==5)server(mc,p->{p.serverLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);p.serverLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);p.serverLevel().setDayTime(6000);clear(p);p.teleportTo(p.serverLevel(),0,-60,-8,0,0);});
        if(ticks==20)server(mc,p->{var types=List.of(DomainEntities.SUKUNA_16.get(),DomainEntities.MEGUMI_SUKUNA.get(),DomainEntities.TRUE_SUKUNA.get());for(int i=0;i<3;i++){var n=npc(p,types.get(i),i*8,0,false);var g=npc(p,DomainEntities.SHINJUKU_GOJO.get(),i*8,2,true);attackers.add(n);targets.add(g);require(SukunaCombatService.punch(n,g),"form starts contact attack "+i);}});
        if(ticks==25)server(mc,p->{for(int i=0;i<3;i++){require(Math.abs(targets.get(i).getHealth()-(1000-24*attackers.get(i).attackScale()*targets.get(i).damageTakenScale()))<.02,"domain amplification lands real contact damage through Infinity "+i);attackers.get(i).setNoAi(true);}});
        if(ticks==30)server(mc,p->{for(int i=0;i<3;i++){var n=attackers.get(i);var g=targets.get(i);n.setNoAi(false);g.setPos(i*8,-60,8);g.invulnerableTime=0;float hp=g.getHealth();require(SukunaCombatService.slash(n,g)&&g.getHealth()==hp,"distant ordinary Dismantle still respects Infinity "+i);g.setPos(i*8,-60,2);g.invulnerableTime=0;require(SukunaCombatService.slash(n,g)&&Math.abs(g.getHealth()-(hp-44*n.attackScale()*g.damageTakenScale()))<.02,"nearby Sukuna can fight through Infinity "+i);n.setNoAi(true);}gojo=targets.get(0);float hp=gojo.getHealth();require(!gojo.hurt(gojo.damageSources().mobAttack(new net.minecraft.world.entity.monster.Zombie(p.serverLevel())),8)&&gojo.getHealth()==hp,"ordinary monsters do not bypass Infinity");clear(p);});
        if(ticks==50)server(mc,p->{gojo=npc(p,DomainEntities.AWAKENED_GOJO.get(),0,0,false);sukuna=npc(p,DomainEntities.SUKUNA_16.get(),0,10,true);victim=husk(p,4,7);require(DomainService.expandNpc(gojo,true),"solo Void starts for mobility test");});
        if(ticks==310)server(mc,p->{require(!DomainService.isCaptive(sukuna.getUUID())&&sukuna.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(DomainService.STASIS_MOVEMENT)==null,"Sukuna receives no Void movement lock");require(DomainService.isCaptive(victim.getUUID()),"Void still immobilizes ordinary monsters");gojo.teleportTo(0,-60,0);gojo.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);gojo.getNavigation().stop();sukuna.teleportTo(-8,-60,4);position=sukuna.position();health=gojo.getHealth();sukuna.setNoAi(false);});
        if(ticks==350)server(mc,p->{require(sukuna.position().distanceToSqr(position)>2*2,"Sukuna actually moves inside active Void pos="+sukuna.position()+" start="+position+" target="+sukuna.getTarget());sukuna.teleportTo(0,-60,2);gojo.invulnerableTime=0;health=gojo.getHealth();require(SukunaCombatService.slash(sukuna,gojo)&&gojo.getHealth()<health,"Sukuna lands a real close attack inside active Void");gojo.setNoAi(true);sukuna.setNoAi(true);});
        if(ticks==405)server(mc,p->{require(DomainService.snapshot(p).isEmpty(),"first field cleans up");clear(p);});
        if(ticks==410)server(mc,p->{gojo=npc(p,DomainEntities.SHINJUKU_GOJO.get(),-2,0,false);sukuna=npc(p,DomainEntities.SUKUNA_16.get(),2,0,false);var team=p.serverLevel().getScoreboard().addPlayerTeam("shell-qa");p.serverLevel().getScoreboard().addPlayerToTeam(gojo.getScoreboardName(),team);p.serverLevel().getScoreboard().addPlayerToTeam(sukuna.getScoreboardName(),team);require(DomainService.expandNpc(gojo,true)&&DomainService.snapshot(p).size()==2,"clash starts with shrine center inside Void");});
        if(ticks==640)server(mc,p->{require(DomainService.barrierIntegrity(gojo.getUUID())<1,"open field reaches and cuts the OUTSIDE of a forming barrier");});
        if(ticks==710)server(mc,p->{var states=DomainService.snapshot(p);require(states.stream().noneMatch(s->s.kind()==DomainKind.VOID&&s.closing()<0),"Shrine breaks Unlimited Void instead of losing the clash");require(states.stream().anyMatch(s->s.kind()==DomainKind.SHRINE&&s.closing()<0),"Shrine remains after the closed shell breaks");});
        if(ticks==750)server(mc,p->{require(sukuna.getY()>-53,"caster initially stands on raised shrine roof");gojo.setNoAi(true);victim=husk(p,2,12);victim.getPersistentData().putDouble("uv_cursed_energy",1000000);});
        if(ticks==850)server(mc,p->{require(sukuna.getY()<-57,"Sukuna walks off roof and returns to ground pos="+sukuna.position()+" target="+sukuna.getTarget());require(roofMelee>0,"Sukuna resumes ordinary combat after leaving shrine");sukuna.setNoAi(true);});
        if(ticks==905)server(mc,p->{clear(p);require(DomainService.snapshot(p).isEmpty(),"second field cleans up");});
        if(ticks==910)server(mc,p->{p.serverLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);clear(p);student=npc(p,DomainEntities.STUDENT_GOJO.get(),0,0,false);attackers.clear();attackers.add(npc(p,DomainEntities.SUKUNA_16.get(),-1.5,5,false));attackers.add(npc(p,DomainEntities.SUKUNA_16.get(),1.5,5,false));if(Boolean.getBoolean("uv.sukunaArmyTest")){attackers.get(1).discard();attackers.set(1,npc(p,DomainEntities.MEGUMI_SUKUNA.get(),1.5,5,false));attackers.add(npc(p,DomainEntities.TRUE_SUKUNA.get(),0,7,false));}p.setGameMode(GameType.SPECTATOR);p.teleportTo(p.serverLevel(),9,-59,-8,40,8);});
        if(ticks==1410)server(mc,p->{require(damage>200,"two autonomous Sukuna land sustained damage on student/awakened Gojo");require(attackers.stream().noneMatch(n->DomainService.isCaptive(n.getUUID())),"neither Sukuna becomes a helpless Void prisoner");LogUtils.getLogger().info("SUKUNA_BALANCE_BATTLE gojoAlive={} form={} hp={} sukunaHp={} damage={}",student.isAlive(),student.getName().getString(),student.getHealth(),attackers.stream().map(JujutsuNpc::getHealth).toList(),damage);});
        if(ticks==1800){Screenshot.grab(mc.gameDirectory,"sukuna-balance-battle.png",mc.getMainRenderTarget(),c->{});server(mc,p->LogUtils.getLogger().info("SUKUNA_BALANCE_FINAL gojoAlive={} hp={} sukunaHp={} damage={}",student.isAlive(),student.getHealth(),attackers.stream().map(JujutsuNpc::getHealth).toList(),damage));}
        if(ticks==1820){LogUtils.getLogger().info("SUKUNA_BALANCE_TEST_COMPLETE");mc.stop();}
    }
}
