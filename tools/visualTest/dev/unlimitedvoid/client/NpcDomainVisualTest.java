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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.Consumer;
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class NpcDomainVisualTest {
    static boolean started;static int ticks,warmup,summons;static final List<JujutsuNpc> actors=new ArrayList<>(),fighters=new ArrayList<>();static final List<Husk> victims=new ArrayList<>();static JujutsuNpc source,gojo;static Husk victim;
    static void require(boolean ok,String message){if(!ok)throw new AssertionError("NPC_DOMAIN_FAIL "+message);LogUtils.getLogger().info("NPC_DOMAIN_PASS {}",message);}
    static void server(Minecraft mc,Consumer<ServerPlayer> action){mc.getSingleplayerServer().execute(()->{try{action.accept(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0));}catch(Throwable error){LogUtils.getLogger().error("NPC_DOMAIN_FAIL server task",error);mc.execute(mc::stop);}});}
    static JujutsuNpc npc(ServerPlayer p,EntityType<JujutsuNpc> type,double x,double z,boolean noAi){var n=type.create(p.serverLevel());n.moveTo(x,-60,z,0,0);n.setNoAi(noAi);p.serverLevel().addFreshEntity(n);return n;}
    static Husk husk(ServerPlayer p,double x,double z){var h=new Husk(EntityType.HUSK,p.serverLevel());h.moveTo(x,-60,z,0,0);h.setNoAi(true);h.setNoGravity(true);h.setPersistenceRequired();h.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);h.getAttribute(Attributes.ARMOR).setBaseValue(0);h.setHealth(1000);p.serverLevel().addFreshEntity(h);return h;}
    static void clear(ServerPlayer p){for(var n:p.serverLevel().getEntitiesOfClass(JujutsuNpc.class,new AABB(-80,-70,-80,80,-40,80))){DomainService.dismissNpc(n);FugaService.cancel(n.getUUID());NpcTechniqueService.cancel(n);n.discard();}for(var e:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(-80,-70,-80,80,-40,80),t->t instanceof net.minecraft.world.entity.monster.Enemy))e.discard();}
    static void idle(){for(var n:actors)if(n.isAlive())n.setNoAi(true);}
    static void pair(ServerPlayer p,JujutsuNpc first,JujutsuNpc second){idle();first.setNoAi(false);second.setNoAi(false);require(DomainService.expandNpc(first,true),"selected NPC opens its correct domain "+first.getName().getString());var states=DomainService.snapshot(p);require(states.size()==2,"opponent counters during the same call");require(states.stream().filter(s->s.kind()==DomainKind.VOID).count()==1&&states.stream().filter(s->s.kind()==DomainKind.SHRINE).count()==1,"one domain of each family");require(states.get(0).age()==states.get(1).age(),"counter has no tick delay");}
    @SubscribeEvent public static void summoned(MahoragaSummonEvent e){if(Boolean.getBoolean("uv.npcDomainTest")){summons++;require(e.caster().isMegumi()&&e.target()!=null,"only Megumi posts summon extension event with target");}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!(Boolean.getBoolean("uv.npcDomainTest")||Boolean.getBoolean("uv.npcNaturalTest"))||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(!started&&mc.screen instanceof TitleScreen){started=true;mc.createWorldOpenFlows().createFreshLevel("Npc-Domains-QA-"+System.currentTimeMillis(),new LevelSettings("NPC Domains QA",GameType.CREATIVE,false,net.minecraft.world.Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());}
        if(mc.player==null||mc.level==null||mc.screen!=null)return;if(warmup++<200)return;if(Boolean.getBoolean("uv.npcNaturalTest")&&ticks==0)ticks=1250;ticks++;
        if(ticks==5)server(mc,p->{p.serverLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);p.serverLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);p.serverLevel().setDayTime(6000);clear(p);p.teleportTo(p.serverLevel(),0,-60,-6,0,0);});
        if(ticks==10)server(mc,p->{
            source=npc(p,DomainEntities.SUKUNA_16.get(),0,0,true);source.setHealth(400);var id=source.getUUID();p.setGameMode(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(DomainItems.SUKUNA_FINGER.get(),3));source.interact(p,InteractionHand.MAIN_HAND);
            require(source.isMegumi()&&!source.isTrueSukuna()&&source.getMaxHealth()==900&&source.getHealth()==450,"finger produces 900HP Megumi while preserving health proportion");require(source.getUUID().equals(id)&&p.getMainHandItem().getCount()==2,"finger preserves identity and consumes one");source.interact(p,InteractionHand.MAIN_HAND);require(p.getMainHandItem().getCount()==2,"repeat finger cannot bypass to true form");
            var tag=new CompoundTag();source.save(tag);tag.remove("UUID");var restored=(JujutsuNpc)EntityType.loadEntityRecursive(tag,p.serverLevel(),a->a);require(restored.isMegumi()&&!restored.isTrueSukuna()&&restored.getHealth()==450,"Megumi identity health survives NBT");
            source.hurt(p.serverLevel().damageSources().indirectMagic(p,p),2000);require(source.isTrueSukuna()&&!source.isMegumi()&&source.getHealth()==1000&&source.getUUID().equals(id),"fatal damage triggers one full-health true transformation");require(!source.becomeTrueSukuna(),"true form cannot refill repeatedly");source.save(tag);tag.remove("UUID");tag.remove("Megumi");restored=(JujutsuNpc)EntityType.loadEntityRecursive(tag,p.serverLevel(),a->a);require(restored.isTrueSukuna(),"existing true-form saves remain true");
            var bypass=npc(p,DomainEntities.MEGUMI_SUKUNA.get(),4,0,true);bypass.hurt(p.serverLevel().damageSources().genericKill(),Float.MAX_VALUE);require(!bypass.isAlive(),"kill bypass does not revive Megumi");clear(p);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setGameMode(GameType.CREATIVE);
        });
        if(ticks==20)server(mc,p->{fighters.clear();victims.clear();var types=List.of(DomainEntities.SUKUNA_16.get(),DomainEntities.MEGUMI_SUKUNA.get(),DomainEntities.TRUE_SUKUNA.get());for(int i=0;i<3;i++){var n=npc(p,types.get(i),i*6,0,false);var h=husk(p,i*6,2);fighters.add(n);victims.add(h);require(SukunaCombatService.punch(n,h),"Sukuna form starts physical punch "+i);}});
        if(ticks==25)server(mc,p->{for(int i=0;i<3;i++){require(victims.get(i).getHealth()==982,"Sukuna form lands one physical hit "+i);fighters.get(i).setNoAi(true);}});
        if(ticks==30)server(mc,p->{for(int i=0;i<3;i++){var n=fighters.get(i);var h=victims.get(i);n.setNoAi(false);h.invulnerableTime=0;h.setPos(i*6,-60,8);float hp=h.getHealth();require(SukunaCombatService.slash(n,h)&&h.getHealth()==hp-24,"each Sukuna uses ranged Dismantle "+i);h.invulnerableTime=0;h.setPos(i*6,-60,2);hp=h.getHealth();require(SukunaCombatService.slash(n,h)&&h.getHealth()==hp-32,"each Sukuna uses nearby Cleave "+i);require(!SukunaCombatService.slash(n,p),"slashes protect players "+i);n.setNoAi(true);}p.teleportTo(p.serverLevel(),2.5,-60,-3,25,3);});
        if(ticks==34){require(NpcSlashes.rendered>0,"client renders synchronized tapered slash cues");Screenshot.grab(mc.gameDirectory,"npc-sukuna-slashes.png",mc.getMainRenderTarget(),c->{});}
        if(ticks==40)server(mc,p->{gojo=npc(p,DomainEntities.SHINJUKU_GOJO.get(),0,8,true);source=fighters.get(0);source.setNoAi(false);float hp=gojo.getHealth();require(SukunaCombatService.slash(source,gojo)&&gojo.getHealth()==hp,"ordinary slash respects passive Infinity");source.setNoAi(true);clear(p);});
        if(ticks==50)server(mc,p->{fighters.clear();victims.clear();var types=List.of(DomainEntities.SUKUNA_16.get(),DomainEntities.MEGUMI_SUKUNA.get(),DomainEntities.TRUE_SUKUNA.get());p.setGameMode(GameType.SURVIVAL);p.teleportTo(p.serverLevel(),8,-60,12,135,0);for(int i=0;i<3;i++){var n=npc(p,types.get(i),-6+i*6,0,false);fighters.add(n);victims.add(husk(p,-6+i*6,14));require(FugaService.request(n),"each Sukuna may cast Fuga "+i);}});
        if(ticks==95){require(FugaClient.drawProgress(fighters.get(0).getUUID())>=0,"NPC Fuga synchronizes charge and bow pose");Screenshot.grab(mc.gameDirectory,"npc-fuga-charge.png",mc.getMainRenderTarget(),c->{});}
        if(ticks==120){Screenshot.grab(mc.gameDirectory,"npc-fuga-blast.png",mc.getMainRenderTarget(),c->{});server(mc,p->{for(int i=0;i<3;i++){require(victims.get(i).getHealth()<1000,"NPC Fuga deals real damage "+i);fighters.get(i).setNoAi(true);}require(p.getHealth()==20,"NPC Fuga protects nearby survival player");});}
        if(ticks==130)server(mc,p->{for(var n:fighters)require(!FugaService.casting(n.getUUID()),"NoAI cancels NPC Fuga");clear(p);p.setGameMode(GameType.CREATIVE);});
        if(ticks==140)server(mc,p->{fighters.clear();for(int i=0;i<9;i++){var n=npc(p,DomainEntities.SUKUNA_16.get(),i*2,0,false);fighters.add(n);require(FugaService.request(n)==(i<8),"shared Fuga slot limit "+i);}FugaService.request(p);require(!FugaService.casting(p.getUUID()),"player cannot exceed NPC-filled Fuga capacity");for(var n:fighters)n.setNoAi(true);});
        if(ticks==150)server(mc,p->{FugaService.request(p);require(FugaService.casting(p.getUUID()),"player can reuse released Fuga slot");FugaService.cancel(p.getUUID());clear(p);});
        if(ticks==160)server(mc,p->{source=npc(p,DomainEntities.MEGUMI_SUKUNA.get(),0,0,false);victim=husk(p,0,2);require(!SukunaCombatService.summon(source,victim)&&summons==1,"summon hook is invoked without inventing an unfinished creature");require(!SukunaCombatService.summon(source,victim)&&summons==1,"summon hook respects cooldown");var tag=new CompoundTag();source.save(tag);tag.remove("UUID");var restored=(JujutsuNpc)EntityType.loadEntityRecursive(tag,p.serverLevel(),a->a);require(!restored.summonReady(),"summon cooldown persists in NBT");source.setNoAi(true);clear(p);});
        if(ticks==180)server(mc,p->{actors.clear();var types=List.of(DomainEntities.STUDENT_GOJO.get(),DomainEntities.AWAKENED_GOJO.get(),DomainEntities.SHINJUKU_GOJO.get(),DomainEntities.SUKUNA_16.get(),DomainEntities.MEGUMI_SUKUNA.get(),DomainEntities.TRUE_SUKUNA.get());var team=p.serverLevel().getScoreboard().addPlayerTeam("npc-domain-qa");for(int i=0;i<6;i++){var n=npc(p,types.get(i),i<3?-4-i*3:4+(i-3)*3,0,false);actors.add(n);p.serverLevel().getScoreboard().addPlayerToTeam(n.getScoreboardName(),team);}p.setGameMode(GameType.SURVIVAL);p.teleportTo(p.serverLevel(),0,-60,-6,0,0);});
        if(ticks==190)server(mc,p->{require(DomainService.expandNpc(actors.get(0),false),"student can open Unlimited Void");var states=DomainService.snapshot(p);require(states.size()==2&&states.get(0).age()==states.get(1).age(),"Sukuna immediately counters a Gojo opening");require(states.stream().anyMatch(s->s.owner().equals(actors.get(3).getUUID())&&s.kind()==DomainKind.SHRINE),"nearest eligible Sukuna owns counter domain");for(int i:new int[]{1,2,4,5})require(!DomainService.expandNpc(actors.get(i),true),"same-family duplicate domain rejected "+i);});
        if(ticks==205){require(DomainSeals.of(actors.get(0).getUUID(),0)!=null&&DomainSeals.of(actors.get(3).getUUID(),0)!=null,"both NPCs receive original block-hand domain seals");server(mc,p->{for(int i:new int[]{1,2,4,5})actors.get(i).setNoAi(true);require(DomainService.fugaContested(actors.get(3).getUUID(),new Vec3(0,-59,0)),"opposing domain reservation contests hits during the longer Void formation");});}
        if(ticks==445){mc.options.hideGui=true;mc.options.fov().set(95);server(mc,p->p.teleportTo(p.serverLevel(),-9,-60,-8,-58,-20));}
        if(ticks==450)server(mc,p->{var states=DomainService.snapshot(p);require(states.size()==2&&states.stream().allMatch(s->s.age()>=DomainTimeline.formed(s.kind())),"both domains fully form and coexist");require(ShrineStructures.get(actors.get(3).getUUID())!=null&&actors.get(3).getY()>-53,"NPC shrine rises physically and lifts its caster");require(p.getHealth()==20,"NPC domain clash protects observer");});
        if(ticks==455){mc.options.hideGui=true;mc.options.fov().set(85);Screenshot.grab(mc.gameDirectory,"npc-domain-clash.png",mc.getMainRenderTarget(),c->{});}
        if(ticks==460)server(mc,p->idle());
        if(ticks==515)server(mc,p->{require(DomainService.snapshot(p).isEmpty()&&DomainService.captiveCount()==0,"NoAI domains fade and release all gameplay state");require(ShrineStructures.get(actors.get(3).getUUID())==null,"closing removes physical shrine");});
        if(ticks==520)server(mc,p->pair(p,actors.get(5),actors.get(1)));
        if(ticks==530)server(mc,p->{actors.get(1).hurt(p.serverLevel().damageSources().genericKill(),Float.MAX_VALUE);idle();});
        if(ticks==580)server(mc,p->require(DomainService.snapshot(p).isEmpty(),"death removes NPC domain through fade"));
        if(ticks==590)server(mc,p->{pair(p,actors.get(2),actors.get(4));var states=DomainService.snapshot(p);require(states.stream().anyMatch(s->s.owner().equals(actors.get(4).getUUID())&&s.kind()==DomainKind.SHRINE),"Megumi also owns a real Shrine counter");});
        if(ticks==600)server(mc,p->idle());
        if(ticks==650)server(mc,p->require(DomainService.snapshot(p).isEmpty(),"all six domain-capable forms clean up"));
        if(ticks==660)server(mc,p->{source=npc(p,DomainEntities.AWAKENED_GOJO.get(),0,0,false);victim=husk(p,0,8);require(DomainService.expandNpc(source,true),"solo Void starts without any active opposing caster");});
        if(ticks==960)server(mc,p->{require(DomainService.isCaptive(victim.getUUID())&&victim.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(DomainService.STASIS_MOVEMENT)!=null,"NPC Void actually immobilizes enemy");require(victim.getHealth()<1000,"NPC Void deals real damage");require(p.getHealth()==20,"NPC Void protects player");source.setNoAi(true);});
        if(ticks==1020)server(mc,p->{require(!DomainService.isCaptive(victim.getUUID())&&victim.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(DomainService.STASIS_MOVEMENT)==null,"Void closing restores enemy movement");clear(p);});
        if(ticks==1030)server(mc,p->{source=npc(p,DomainEntities.TRUE_SUKUNA.get(),0,0,false);gojo=npc(p,DomainEntities.SHINJUKU_GOJO.get(),0,8,true);p.teleportTo(p.serverLevel(),0,-60,-6,0,0);require(DomainService.expandNpc(source,true),"solo Shrine starts against an inactive Gojo");});
        if(ticks==1200)server(mc,p->{require(!gojo.isAlive(),"uncontested NPC Shrine sure-hit damages Infinity-protected enemy");require(p.getHealth()==20&&!ClosedBarrier.contains(p.getUUID()),"open NPC Shrine neither damages nor encloses observer");source.setNoAi(true);});
        if(ticks==1250)server(mc,p->require(DomainService.snapshot(p).isEmpty()&&DomainService.activeCount()==0,"final domain cleanup leaves no stale family slots"));
        if(ticks==1255)server(mc,p->{p.serverLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);clear(p);fighters.clear();victims.clear();var types=List.of(DomainEntities.SUKUNA_16.get(),DomainEntities.MEGUMI_SUKUNA.get(),DomainEntities.TRUE_SUKUNA.get());for(int i=0;i<3;i++){fighters.add(npc(p,types.get(i),-8+i*8,0,false));victims.add(husk(p,-8+i*8,2));}p.teleportTo(p.serverLevel(),0,-60,-6,0,0);});
        if(ticks==1330)server(mc,p->{for(int i=0;i<3;i++){require(victims.get(i).getHealth()<950,"natural Sukuna AI pursues and attacks without service calls "+i);require(fighters.get(i).getTarget()!=p,"natural Sukuna AI protects player "+i);}fighters.get(1).setHealth(40);});
        if(ticks==1335)server(mc,p->require(fighters.get(1).isTrueSukuna()&&fighters.get(1).getHealth()==1000,"natural AI transforms a critically injured Megumi to full-health true form"));
        if(ticks==1355){if(Boolean.getBoolean("uv.shaderTest"))try{var manager=Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager").invoke(null);var pipeline=manager.getClass().getMethod("getPipelineNullable").invoke(manager);require(pipeline!=null&&pipeline.getClass().getSimpleName().equals("IrisRenderingPipeline"),"real shader pipeline active");}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}require(mc.getResourceManager().getResource(new net.minecraft.resources.ResourceLocation(UnlimitedVoid.ID,"textures/entity/megumi_sukuna.png")).isPresent(),"supplied Megumi skin present");LogUtils.getLogger().info("NPC_DOMAIN_TEST_COMPLETE");mc.stop();}
    }
}
