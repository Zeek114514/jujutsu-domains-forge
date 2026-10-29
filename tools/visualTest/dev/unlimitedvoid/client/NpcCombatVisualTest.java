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
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class NpcCombatVisualTest {
    static int blueFists,redFists,beforeWallFists;static boolean clientPunch;static float beforePunch;static boolean started;static int ticks,warmup;static JujutsuNpc caster,duel;static Husk victim,far;
    static final Set<LimitlessKind> seen=EnumSet.noneOf(LimitlessKind.class);static boolean immediate,duelClose,blastAfterDeath;static float farHealth;static final java.util.List<JujutsuNpc> healers=new ArrayList<>();static final java.util.List<Float> wounded=new ArrayList<>();static float interruptedHealth,survivorAfterBlast;static JujutsuNpc survivor;
    static void require(boolean value,String message){if(!value)throw new AssertionError("NPC_COMBAT_FAIL "+message);LogUtils.getLogger().info("NPC_COMBAT_PASS {}",message);}
    static void server(Minecraft mc,Consumer<ServerPlayer> work){mc.getSingleplayerServer().execute(()->{try{work.accept(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0));}catch(Throwable error){LogUtils.getLogger().error("NPC_COMBAT_FAIL server task",error);mc.execute(mc::stop);}});}
    static JujutsuNpc npc(ServerPlayer p,EntityType<JujutsuNpc> type,double x,double z,boolean noAi){var n=type.create(p.serverLevel());n.moveTo(x,-60,z,180,0);n.setYBodyRot(180);n.setYHeadRot(180);n.setNoAi(noAi);p.serverLevel().addFreshEntity(n);return n;}
    static Husk husk(ServerPlayer p,double x,double z){var h=new Husk(EntityType.HUSK,p.serverLevel());h.moveTo(x,-60,z,180,0);h.setNoAi(true);h.setNoGravity(true);h.setPersistenceRequired();h.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);h.setHealth(1000);p.serverLevel().addFreshEntity(h);return h;}
    static void clear(ServerPlayer p){for(var n:p.serverLevel().getEntitiesOfClass(JujutsuNpc.class,new AABB(-80,-70,-80,80,-40,80))){NpcTechniqueService.cancel(n);n.discard();}for(var h:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(-80,-70,-80,80,-40,80),e->e instanceof net.minecraft.world.entity.monster.Enemy))h.discard();seen.clear();}
    static void forceGoal(LimitlessKind kind){long seed=0;while(true){var random=net.minecraft.util.RandomSource.create(seed);if(!NpcMeleeRules.preferPunch(random.nextInt(100))&&NpcCombatRules.choose(true,random.nextInt(100))==kind)break;seed++;}caster.getRandom().setSeed(seed);new GojoCombatGoal(caster).tick();}
    @SubscribeEvent public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent e){if(Boolean.getBoolean("uv.npcCombatTest")&&e.getSource().getEntity() instanceof JujutsuNpc n&&n.punchKind()>=0){if(n.punchKind()==0)blueFists++;else redFists++;}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("uv.npcCombatTest")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(!started&&mc.screen instanceof TitleScreen){started=true;mc.createWorldOpenFlows().createFreshLevel("Npc-Combat-QA-"+System.currentTimeMillis(),new LevelSettings("NPC Combat QA",GameType.CREATIVE,false,net.minecraft.world.Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());}
        if(mc.player==null||mc.level==null||mc.screen!=null)return;if(warmup++<200)return;ticks++;
        if(ticks==10)server(mc,p->{p.serverLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);p.serverLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);p.serverLevel().setDayTime(6000);clear(p);p.teleportTo(p.serverLevel(),0,-60,-8,0,0);caster=npc(p,DomainEntities.STUDENT_GOJO.get(),0,0,false);victim=husk(p,0,10);});
        if(ticks>=12&&ticks%2==0)server(mc,p->{
            if(caster==null)return;
            for(var state:LimitlessService.snapshot(p))if(state.owner().equals(caster.getUUID())){
                seen.add(state.kind());if(caster.isAscended()&&state.kind()==LimitlessKind.PURPLE&&state.phase()==1&&state.phaseStart()-state.start()==0)immediate=true;
                if(state.phase()==PurpleDetonation.BLAST&&!caster.isAlive())blastAfterDeath=true;
                if(duel!=null&&state.kind()==LimitlessKind.PURPLE&&state.phase()==1&&caster.distanceToSqr(duel)<=3*3)duelClose=true;
            }
            requireSilent(LimitlessService.snapshot(p).size()<=8,"shared cast cap");
        });
        if(ticks==55){mc.options.hideGui=true;mc.options.fov().set(65);server(mc,p->p.teleportTo(p.serverLevel(),5,-60,-6,18,0));}
        if(ticks<100)for(var actor:mc.level.entitiesForRendering())if(actor instanceof JujutsuNpc n&&n.punchKind()>=0)clientPunch=true;
        if(ticks==65)capture(mc,"npc-blue-fist.png");
        if(ticks==100)server(mc,p->{require(seen.isEmpty(),"first engagement favors fists instead of remote spells");require(blueFists>=3&&redFists==0,"student repeatedly lands Blue fists only");require(clientPunch,"client receives synchronized punch animation");require(caster.getZ()>5,"Gojo naturally pursues target across the ground");require(caster.getAttributeValue(Attributes.MOVEMENT_SPEED)==.34,"Gojo movement speed increased");require(victim.getHealth()<950,"Blue fists damage actual monster");require(p.getHealth()==20,"player remains unharmed");clear(p);});
        if(ticks==120)server(mc,p->{caster=npc(p,DomainEntities.AWAKENED_GOJO.get(),0,0,true);victim=husk(p,0,8);require(caster.isAscended()&&caster.isAwakened()&&!caster.isShinjuku(),"new egg type defaults to distinct awakened form");forceGoal(LimitlessKind.RED);});
        if(ticks==165)server(mc,p->{require(seen.contains(LimitlessKind.RED),"awakened goal can select Red");require(victim.getHealth()<1000,"Red damages actual monster");float hp=caster.getHealth();require(!caster.hurt(p.serverLevel().damageSources().playerAttack(p),5)&&caster.getHealth()==hp,"awakened Gojo inherits Infinity");clear(p);});
        if(ticks==180)server(mc,p->{caster=npc(p,DomainEntities.AWAKENED_GOJO.get(),0,0,true);victim=husk(p,0,8);forceGoal(LimitlessKind.PURPLE);});
        if(ticks==225)capture(mc,"npc-purple-charge.png");
        if(ticks==275)server(mc,p->{require(seen.contains(LimitlessKind.PURPLE),"awakened low-probability branch casts Purple");require(victim.getHealth()<900,"Purple sweeps and damages monster");clear(p);});
        if(ticks==280)server(mc,p->{DomainConfig.NPC_DANGER_TRANSFORM_CHANCE.set(1.0);caster=npc(p,DomainEntities.STUDENT_GOJO.get(),0,0,false);victim=husk(p,0,7);var id=caster.getUUID();caster.hurt(p.serverLevel().damageSources().indirectMagic(p,p),caster.getMaxHealth()*2);require(caster.isAscended()&&!caster.isShinjuku()&&caster.getHealth()==caster.getMaxHealth(),"lethal first hit awakens and fully heals student");require(caster.getUUID().equals(id)&&!caster.lastResortUsed(),"awakening preserves identity and does not trigger self detonation");p.teleportTo(p.serverLevel(),3,-60,4,143,6);});
        if(ticks==300){capture(mc,"awakened-gojo.png");server(mc,p->{require(immediate,"awakening immediately launches Purple without a normal charge");require(!caster.awakeningBurstPending(),"awakening burst launches once");var tag=new CompoundTag();caster.save(tag);tag.remove("UUID");var copy=(JujutsuNpc)EntityType.loadEntityRecursive(tag,p.serverLevel(),a->a);require(copy!=null&&copy.isAscended()&&!copy.isShinjuku()&&!copy.awakeningBurstPending(),"awakening state survives NBT reload distinctly");require(!caster.awakenFromDanger(),"already awakened Gojo cannot refill again");});}
        if(ticks==315)server(mc,p->{p.setGameMode(GameType.SURVIVAL);NpcTechniqueService.cancel(caster);far=husk(p,0,50);farHealth=far.getHealth();caster.setHealth(1);p.teleportTo(p.serverLevel(),12,-60,-14,40,-5);});
        if(ticks==330){capture(mc,"npc-self-purple-growing.png");server(mc,p->{require(caster.lastResortUsed(),"low-health awakened Gojo arms one self detonation");require(NpcTechniqueService.snapshot(p).stream().anyMatch(s->s.owner().equals(caster.getUUID())&&s.phase()==PurpleDetonation.GROWING),"self Purple visibly grows before detonation");var tag=new CompoundTag();caster.save(tag);tag.remove("UUID");var copy=(JujutsuNpc)EntityType.loadEntityRecursive(tag,p.serverLevel(),a->a);require(copy!=null&&copy.lastResortUsed(),"one-shot self detonation flag is persistent");});}
        if(ticks==375)capture(mc,"npc-self-purple-blast.png");
        if(ticks==405)server(mc,p->{require(blastAfterDeath,"blast continues after caster dies to backlash");require(far.getHealth()<farHealth,"self Purple reaches a distant monster after caster death");require(p.getHealth()==20,"NPC self detonation protects players");require(!caster.isAlive(),"low-health caster takes configured self backlash");require(!NpcTechniqueService.casting(caster),"self detonation expires instead of repeating");clear(p);p.setGameMode(GameType.CREATIVE);});
        if(ticks==420)server(mc,p->{caster=npc(p,DomainEntities.SHINJUKU_GOJO.get(),0,0,false);duel=npc(p,DomainEntities.TRUE_SUKUNA.get(),0,9,true);p.teleportTo(p.serverLevel(),0,-60,-8,0,0);});
        if(ticks==460)capture(mc,"shinjuku-sukuna-purple.png");
        if(ticks==490)server(mc,p->{require(seen.contains(LimitlessKind.PURPLE)&&!seen.contains(LimitlessKind.BLUE)&&!seen.contains(LimitlessKind.RED),"Shinjuku prioritizes Purple against true Sukuna seen="+seen+" distance="+caster.distanceTo(duel)+" pos="+caster.position()+" target="+duel.position());require(duelClose,"Shinjuku approaches to point-blank range before Purple");require(duel.isAlive()&&duel.getHealth()<duel.getMaxHealth(),"high-health true Sukuna survives a real point-blank Purple hit");clear(p);duel=null;});
        if(ticks==510)server(mc,p->{caster=npc(p,DomainEntities.STUDENT_GOJO.get(),0,0,false);npc(p,DomainEntities.SUKUNA_16.get(),0,6,true);});
        if(ticks==540)server(mc,p->{require(caster.getTarget() instanceof JujutsuNpc enemy&&!enemy.isGojo(),"Gojo now recognizes initial Sukuna as an opponent");require(p.getHealth()==20,"player still unharmed after all stages");});
        if(ticks==550)server(mc,p->{
            clear(p);victim=husk(p,0,20);var group=new ArrayList<JujutsuNpc>();
            for(int i=0;i<9;i++)group.add(npc(p,DomainEntities.AWAKENED_GOJO.get(),-10+i*2,6,true));
            for(int i=0;i<8;i++)require(NpcTechniqueService.start(group.get(i),LimitlessKind.BLUE,victim,false),"NPC shared capacity slot "+i);
            require(!NpcTechniqueService.start(group.get(8),LimitlessKind.BLUE,victim,false)&&LimitlessService.snapshot(p).size()==8,"ninth NPC cast is rejected");
            LimitlessService.reset(p);LimitlessService.request(p,LimitlessKind.RED);require(LimitlessService.cooldown(p,LimitlessKind.RED)==0&&LimitlessService.snapshot(p).size()==8,"player also respects full shared cast budget");
            NpcTechniqueService.cancel(group.get(0));LimitlessService.request(p,LimitlessKind.RED);require(LimitlessService.cooldown(p,LimitlessKind.RED)>0&&LimitlessService.snapshot(p).size()==8,"player can use released NPC capacity without exceeding protocol cap");p.setGameMode(GameType.SPECTATOR);
        });
        if(ticks==560)server(mc,p->{clear(p);LimitlessService.reset(p);p.setGameMode(GameType.CREATIVE);});
        if(ticks==570)server(mc,p->{
            var types=java.util.List.of(DomainEntities.STUDENT_GOJO.get(),DomainEntities.AWAKENED_GOJO.get(),DomainEntities.SHINJUKU_GOJO.get(),DomainEntities.SUKUNA_16.get(),DomainEntities.TRUE_SUKUNA.get());double[] hp={400,800,1000,800,1000};
            var board=p.serverLevel().getScoreboard();var team=board.addPlayerTeam("rct-qa");
            for(int i=0;i<types.size();i++){
                var n=npc(p,types.get(i),i*2,6,false);board.addPlayerToTeam(n.getScoreboardName(),team);healers.add(n);require(n.getMaxHealth()==hp[i]&&n.getHealth()==hp[i],"new per-form health profile "+i);
                var legacy=new CompoundTag();n.save(legacy);legacy.remove("UUID");legacy.remove("NpcVitalsRevision");legacy.remove("Attributes");legacy.putFloat("Health",20);
                var restored=(JujutsuNpc)EntityType.loadEntityRecursive(legacy,p.serverLevel(),a->a);require(restored!=null&&restored.getHealth()==restored.getMaxHealth()*.5f,"legacy 40HP save migration preserves half health "+i);
                n.setHealth(n.getMaxHealth()*.5f);n.hurt(p.serverLevel().damageSources().indirectMagic(p,p),10);wounded.add(n.getHealth());
            }
        });
        if(ticks==610)server(mc,p->{for(int i=0;i<healers.size();i++)require(healers.get(i).getHealth()==wounded.get(i),"RCT waits after real damage "+i);});
        if(ticks==640)server(mc,p->{
            require(healers.get(0).getHealth()==wounded.get(0)&&!healers.get(0).canReverse(),"student has no normal reversal healing");
            for(int i=1;i<healers.size();i++)require(healers.get(i).getHealth()>wounded.get(i)&&healers.get(i).canReverse(),"all advanced Gojo and Sukuna forms use RCT "+i);
        });
        if(ticks==645)server(mc,p->{var n=healers.get(1);n.invulnerableTime=0;n.hurt(p.serverLevel().damageSources().indirectMagic(p,p),10);interruptedHealth=n.getHealth();var tag=new CompoundTag();n.save(tag);tag.remove("UUID");var restored=(JujutsuNpc)EntityType.loadEntityRecursive(tag,p.serverLevel(),a->a);var savedAgain=new CompoundTag();restored.save(savedAgain);require(savedAgain.getInt("ReverseDelay")>0,"RCT damage delay survives NBT restore");});
        if(ticks==675)server(mc,p->{require(healers.get(1).getHealth()==interruptedHealth,"new damage restarts RCT delay");});
        if(ticks==700)server(mc,p->healers.get(1).setHealth(healers.get(1).getMaxHealth()-.1f));
        if(ticks==720)server(mc,p->{require(healers.get(1).getHealth()==healers.get(1).getMaxHealth(),"RCT clamps at full health");});
        if(ticks==740)server(mc,p->{survivor=npc(p,DomainEntities.AWAKENED_GOJO.get(),64,0,false);survivor.setHealth(35);p.teleportTo(p.serverLevel(),64,-60,-10,0,0);});
        if(ticks==795)server(mc,p->{require(survivor.isAlive()&&survivor.lastResortUsed()&&survivor.getHealth()<35,"last-resort backlash can leave a living critical-health caster");survivorAfterBlast=survivor.getHealth();});
        if(ticks==860)server(mc,p->{require(survivor.getHealth()>survivorAfterBlast,"surviving last-resort caster resumes RCT");require(!NpcTechniqueService.detonating(survivor),"survivor does not re-arm self Purple indefinitely");});
        if(ticks==885)server(mc,p->{clear(p);caster=npc(p,DomainEntities.AWAKENED_GOJO.get(),0,0,false);victim=husk(p,0,2);victim.getAttribute(Attributes.ARMOR).setBaseValue(0);p.teleportTo(p.serverLevel(),2.5,-60,-2.5,26,5);beforePunch=victim.getHealth();require(NpcMeleeService.start(caster,victim,LimitlessKind.RED),"advanced Gojo starts Red punch windup");require(victim.getHealth()==beforePunch,"punch does not deal damage before windup");});
        if(ticks==888)capture(mc,"npc-red-fist.png");
        if(ticks==889)server(mc,p->{require(victim.getHealth()==beforePunch-28,"Red punch lands once for 28 damage on zero armor actual="+victim.getHealth());require(victim.getDeltaMovement().z>0,"Red punch repels target");require(redFists>0,"Red fist damage is a real NPC melee hit");caster.setNoAi(true);});
        if(ticks==900)capture(mc,"npc-melee-scene.png");
        if(ticks==905)server(mc,p->{clear(p);caster=npc(p,DomainEntities.STUDENT_GOJO.get(),0,0,false);victim=husk(p,0,2);require(!NpcMeleeService.start(caster,victim,LimitlessKind.RED),"student cannot use Red fists");require(NpcMeleeService.start(caster,victim,LimitlessKind.BLUE),"student starts Blue punch");require(victim.getDeltaMovement().z<0,"Blue fist attracts target during windup");victim.setPos(0,-60,10);beforePunch=victim.getHealth();});
        if(ticks==910)server(mc,p->{require(victim.getHealth()==beforePunch,"escaping melee reach avoids the pending punch");caster.setNoAi(true);require(!NpcMeleeService.start(caster,victim,LimitlessKind.BLUE),"NoAI disables active punches");});
        if(ticks==915)server(mc,p->{clear(p);caster=npc(p,DomainEntities.AWAKENED_GOJO.get(),0,0,false);victim=husk(p,0,2);require(!NpcMeleeService.start(caster,p,LimitlessKind.BLUE),"fists protect players");require(NpcMeleeService.start(caster,victim,LimitlessKind.BLUE),"start punch before emergency");caster.cancelPunch();require(caster.punchKind()==-1,"emergency cancellation clears punch animation");caster.setNoAi(true);});
        if(ticks==925)server(mc,p->{clear(p);caster=npc(p,DomainEntities.STUDENT_GOJO.get(),0,0,false);victim=husk(p,0,2);require(NpcMeleeService.start(caster,victim,LimitlessKind.BLUE),"start punch before target moves behind wall");beforeWallFists=blueFists;victim.setPos(0,-60,2.7);for(int x=-2;x<=2;x++)for(int y=-60;y<=-57;y++)p.serverLevel().setBlockAndUpdate(new net.minecraft.core.BlockPos(x,y,1),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());});
        if(ticks==930)server(mc,p->{require(blueFists==beforeWallFists&&!caster.hasLineOfSight(victim),"wall blocks a pending punch at impact time");caster.setNoAi(true);for(int x=-2;x<=2;x++)for(int y=-60;y<=-57;y++)p.serverLevel().setBlockAndUpdate(new net.minecraft.core.BlockPos(x,y,1),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());});
        if(ticks==935){
            if(Boolean.getBoolean("uv.shaderTest"))try{var manager=Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager").invoke(null);var pipeline=manager.getClass().getMethod("getPipelineNullable").invoke(manager);require(pipeline!=null&&pipeline.getClass().getSimpleName().equals("IrisRenderingPipeline"),"real shader pipeline active");}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
            require(mc.getResourceManager().getResource(new net.minecraft.resources.ResourceLocation(UnlimitedVoid.ID,"textures/entity/awakened_gojo.png")).isPresent(),"supplied awakened skin present");
            LogUtils.getLogger().info("NPC_COMBAT_TEST_COMPLETE");mc.stop();
        }
    }
    static void requireSilent(boolean value,String message){if(!value)throw new AssertionError("NPC_COMBAT_FAIL "+message);}
    static void capture(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->{});}
}
