package dev.unlimitedvoid.client;

import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.JujutsuNpc;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class NpcVisualTest {
    static boolean started;static int ticks,warmup;static JujutsuNpc gojo,sukuna;
    static Vec3 gojoStart,sukunaStart;static net.minecraft.world.entity.projectile.Arrow infinityArrow;
    static void require(boolean ok,String message){if(!ok)throw new AssertionError("NPC_FAIL "+message);LogUtils.getLogger().info("NPC_PASS {}",message);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("uv.npcTest")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(!started&&mc.screen instanceof TitleScreen){started=true;mc.createWorldOpenFlows().createFreshLevel("Npc-QA-"+System.currentTimeMillis(),new LevelSettings("NPC QA",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());}
        if(mc.player==null||mc.level==null||(mc.screen!=null&&ticks<210))return;if(warmup++<200)return;ticks++;
        if(ticks==5){require(mc.getEntityRenderDispatcher().getRenderer(DomainEntities.STUDENT_GOJO.get().create(mc.level))!=null,"Gojo renderer registered");require(mc.getEntityRenderDispatcher().getRenderer(DomainEntities.SUKUNA_16.get().create(mc.level))!=null,"Sukuna renderer registered");}
        if(ticks==6){
            var four=java.util.Set.of(DomainItems.STUDENT_GOJO.get(),DomainItems.SUKUNA_16.get(),DomainItems.BLINDFOLD.get(),DomainItems.SUKUNA_FINGER.get(),DomainItems.SHINJUKU_GOJO.get(),DomainItems.TRUE_SUKUNA.get());
            net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(),true,mc.level.registryAccess());
            var tab=DomainItems.JUJUTSU_TAB.get();require(tab.getDisplayItems().size()==6&&tab.getDisplayItems().stream().allMatch(st->four.contains(st.getItem())),"dedicated creative tab contains exactly all six mod items");
            for(var key:new net.minecraft.resources.ResourceKey[]{net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS,net.minecraft.world.item.CreativeModeTabs.INGREDIENTS}){
                var vanilla=net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.get((net.minecraft.resources.ResourceKey<net.minecraft.world.item.CreativeModeTab>)key);
                require(vanilla.getDisplayItems().stream().noneMatch(st->four.contains(st.getItem())),"mod items removed from vanilla category");
            }
        }
        if(ticks==10)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var level=p.serverLevel();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);level.setDayTime(6000);p.teleportTo(level,0,-60,-6,0,0);
            var eggs=new Item[]{DomainItems.STUDENT_GOJO.get(),DomainItems.SUKUNA_16.get()};
            int x=-2;
            for(var egg:eggs){
                var pos=new BlockPos(x,-61,0);level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(egg));
                require(p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(x+.5,-60,.5),Direction.UP,pos,false))).consumesAction(),"real spawn egg interaction "+egg);
                x+=3;
            }
            var actors=level.getEntitiesOfClass(JujutsuNpc.class,new AABB(-10,-65,-5,10,-50,5));
            require(actors.size()==2,"two NPCs spawned from eggs");
            gojo=actors.stream().filter(JujutsuNpc::isGojo).findFirst().orElseThrow();sukuna=actors.stream().filter(a->!a.isGojo()).findFirst().orElseThrow();
            for(var actor:actors){actor.setYRot(180);actor.setYBodyRot(180);actor.setYHeadRot(180);require(actor.isPersistenceRequired(),"persistent display actor");require(actor.getTarget()==null,"no combat target");}
            gojoStart=gojo.position();sukunaStart=sukuna.position();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        });
        if(ticks==25)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var level=p.serverLevel();
            var eggs=new Item[]{DomainItems.SHINJUKU_GOJO.get(),DomainItems.TRUE_SUKUNA.get()};int x=20;
            for(var egg:eggs){
                var pos=new BlockPos(x,-61,0);level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(egg));
                require(p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(x+.5,-60,.5),Direction.UP,pos,false))).consumesAction(),"advanced egg real use "+egg);
                var npc=level.getEntitiesOfClass(JujutsuNpc.class,new AABB(x,-61,0,x+1,-56,1)).stream().findFirst().orElseThrow();
                require(npc.isAwakened()&&npc.isGojo()==(x==20),"advanced egg directly spawns correct final form");
                require(net.minecraftforge.common.ForgeSpawnEggItem.fromEntityType(npc.getType())==egg,"advanced type has correct pick-block egg");
                var tag=new CompoundTag();npc.save(tag);tag.remove("UUID");var restored=(JujutsuNpc)EntityType.loadEntityRecursive(tag,level,a->a);require(restored!=null&&restored.getType()==npc.getType()&&restored.isAwakened(),"advanced identity and form persist in NBT");
                tag.remove("Awakened");var defaulted=(JujutsuNpc)EntityType.loadEntityRecursive(tag,level,a->a);require(defaulted!=null&&defaulted.isAwakened(),"advanced type default does not require form NBT");
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(npc.isGojo()?DomainItems.BLINDFOLD.get():DomainItems.SUKUNA_FINGER.get()));npc.interact(p,InteractionHand.MAIN_HAND);require(p.getMainHandItem().getCount()==1,"advanced NPC repeat offering does not consume");
                if(npc.isGojo()){float health=npc.getHealth();require(!npc.hurt(level.damageSources().mobAttack(new net.minecraft.world.entity.monster.Zombie(level)),4)&&npc.getHealth()==health,"directly spawned Shinjuku inherits Infinity");}
                x+=3;
            }
            require(net.minecraftforge.common.ForgeSpawnEggItem.fromEntityType(DomainEntities.STUDENT_GOJO.get())==DomainItems.STUDENT_GOJO.get()&&net.minecraftforge.common.ForgeSpawnEggItem.fromEntityType(DomainEntities.SUKUNA_16.get())==DomainItems.SUKUNA_16.get(),"original pick-block eggs remain intact");p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        });
        if(ticks==35){mc.options.hideGui=true;mc.options.fov().set(45);camera(mc,0,-60,-3.8,0,8);}
        if(ticks==45)mc.getSingleplayerServer().execute(()->{
            infinityArrow=new net.minecraft.world.entity.projectile.Arrow(gojo.level(),gojo.getX(),gojo.getY()+1.25,gojo.getZ()-3);
            infinityArrow.setDeltaMovement(0,0,1.4);gojo.level().addFreshEntity(infinityArrow);
        });
        if(ticks==60)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var level=p.serverLevel();
            var attacker=new net.minecraft.world.entity.monster.Zombie(level);
            float health=gojo.getHealth();require(!gojo.hurt(level.damageSources().mobAttack(attacker),8)&&gojo.getHealth()==health,"Gojo Infinity blocks ordinary melee");
            require(!gojo.hurt(level.damageSources().arrow(infinityArrow,p),8)&&gojo.getHealth()==health,"Gojo Infinity blocks arrow damage");
            require(sukuna.hurt(level.damageSources().mobAttack(attacker),3)&&sukuna.getHealth()<40,"Sukuna does not receive Gojo Infinity");sukuna.setHealth(40);
            gojo.invulnerableTime=0;require(gojo.hurt(level.damageSources().indirectMagic(p,p),2),"domain-style indirect magic is not blanket blocked");gojo.setHealth(health);gojo.invulnerableTime=0;
            DomainConfig.GOJO_INFINITY.set(false);
            try{require(gojo.hurt(level.damageSources().mobAttack(attacker),2),"Infinity config can disable defence");}finally{DomainConfig.GOJO_INFINITY.set(true);gojo.setHealth(health);gojo.invulnerableTime=0;}
            require(infinityArrow.isAlive()&&infinityArrow.getZ()<gojo.getZ()-.4&&infinityArrow.getDeltaMovement().length()<.6,"incoming arrow slows before contacting Gojo");
            require(!infinityArrow.isNoGravity(),"Infinity does not permanently modify projectile gravity");
            var dummy=DomainEntities.STUDENT_GOJO.get().create(level);dummy.moveTo(12,-60,4,0,0);level.addFreshEntity(dummy);dummy.hurt(level.damageSources().genericKill(),Float.MAX_VALUE);require(!dummy.isAlive(),"kill bypass remains functional");dummy.discard();
        });
        if(ticks==75)Screenshot.grab(mc.gameDirectory,"npc-front.png",mc.getMainRenderTarget(),c->{});
        if(ticks==90)mc.getSingleplayerServer().execute(()->{
            require(gojo.position().distanceToSqr(gojoStart)<.02&&sukuna.position().distanceToSqr(sukunaStart)<.02,"no wandering");
            require(gojo.getTarget()==null&&sukuna.getTarget()==null,"no automatic combat");
            var tag=new CompoundTag();require(gojo.save(tag),"NPC supports world save");tag.remove("UUID");
            var reloaded=EntityType.loadEntityRecursive(tag,gojo.level(),a->a);
            require(reloaded instanceof JujutsuNpc&&((JujutsuNpc)reloaded).isGojo(),"entity type restored from NBT");
            require(((JujutsuNpc)reloaded).isPersistenceRequired(),"persistence restored from NBT");
            require(reloaded.getX()==gojo.getX()&&reloaded.getY()==gojo.getY(),"saved position restored");
            var level=gojo.level();reloaded.setPos(5,-60,0);require(level.addFreshEntity(reloaded),"saved NPC can be added to world");
        });
        if(ticks==80)camera(mc,5,-60,-5,45,6);
        if(ticks==85)Screenshot.grab(mc.gameDirectory,"npc-side.png",mc.getMainRenderTarget(),c->{});
        if(ticks==100)mc.getSingleplayerServer().execute(()->{
            infinityArrow.discard();
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.setGameMode(GameType.SURVIVAL);
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(DomainItems.SUKUNA_FINGER.get(),3));gojo.interact(p,InteractionHand.MAIN_HAND);
            require(!gojo.isAwakened()&&p.getMainHandItem().getCount()==3,"wrong item does not change Gojo or consume");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(DomainItems.BLINDFOLD.get(),3));sukuna.interact(p,InteractionHand.MAIN_HAND);
            require(!sukuna.isAwakened()&&p.getMainHandItem().getCount()==3,"wrong item does not change Sukuna or consume");
            var gojoId=gojo.getUUID();var sukunaId=sukuna.getUUID();float health=gojo.getHealth();
            gojo.interact(p,InteractionHand.MAIN_HAND);require(gojo.isAwakened()&&p.getMainHandItem().getCount()==2,"blindfold transforms Gojo and consumes exactly one");
            gojo.interact(p,InteractionHand.MAIN_HAND);require(p.getMainHandItem().getCount()==2,"repeated Gojo interaction does not consume");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(DomainItems.SUKUNA_FINGER.get(),3));sukuna.interact(p,InteractionHand.MAIN_HAND);
            require(sukuna.isAwakened()&&p.getMainHandItem().getCount()==2,"finger transforms Sukuna and consumes exactly one");
            sukuna.interact(p,InteractionHand.MAIN_HAND);require(p.getMainHandItem().getCount()==2,"repeated Sukuna interaction does not consume");
            require(gojo.getUUID().equals(gojoId)&&sukuna.getUUID().equals(sukunaId)&&gojo.getHealth()==health,"transform preserves identity and health");
            for(var actor:new JujutsuNpc[]{gojo,sukuna}){
                var tag=new CompoundTag();actor.save(tag);tag.remove("UUID");var restored=EntityType.loadEntityRecursive(tag,actor.level(),a->a);
                require(restored instanceof JujutsuNpc&&((JujutsuNpc)restored).isAwakened(),"transformed form survives NBT reload");
                tag.remove("Awakened");var legacy=EntityType.loadEntityRecursive(tag,actor.level(),a->a);
                require(legacy instanceof JujutsuNpc&&!((JujutsuNpc)legacy).isAwakened(),"legacy NPC NBT defaults to first form");
            }
            p.setGameMode(GameType.CREATIVE);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(DomainItems.BLINDFOLD.get()));
            var extra=p.serverLevel().getEntitiesOfClass(JujutsuNpc.class,new AABB(4,-62,-1,6,-57,1)).stream().filter(a->a!=gojo&&a!=sukuna).findFirst().orElseThrow();
            extra.interact(p,InteractionHand.MAIN_HAND);require(extra.isAwakened()&&p.getMainHandItem().getCount()==1,"creative transforms without consuming");
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        });
        if(ticks==115){mc.options.fov().set(45);camera(mc,0,-60,-3.8,0,8);}
        if(ticks==135)Screenshot.grab(mc.gameDirectory,"npc-transformed.png",mc.getMainRenderTarget(),c->{});
        if(ticks==145){mc.options.fov().set(35);camera(mc,gojoStart.x,-60,-2.1,0,-3);}
        if(ticks==165)Screenshot.grab(mc.gameDirectory,"npc-gojo-close.png",mc.getMainRenderTarget(),c->{});
        if(ticks==175)camera(mc,sukunaStart.x,-60,-2.1,0,-3);
        if(ticks==195)Screenshot.grab(mc.gameDirectory,"npc-sukuna-close.png",mc.getMainRenderTarget(),c->{});
        if(ticks==210){
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player,mc.player.connection.enabledFeatures(),true));
        }
        if(ticks==214){
            var screen=mc.screen;
            if(screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen creative){
                try{var field=screen.getClass().getDeclaredField("pages");field.setAccessible(true);for(var value:(java.util.List<?>)field.get(screen)){var page=(net.minecraftforge.client.gui.CreativeTabsScreenPage)value;if(page.getVisibleTabs().contains(DomainItems.JUJUTSU_TAB.get())){creative.setCurrentPage(page);break;}}require(creative.getCurrentPage().getVisibleTabs().contains(DomainItems.JUJUTSU_TAB.get()),"custom tab visible on real Forge creative page");}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
                try{for(var method:screen.getClass().getDeclaredMethods())if(method.getParameterCount()==1&&method.getParameterTypes()[0]==net.minecraft.world.item.CreativeModeTab.class&&method.getReturnType()==void.class){method.setAccessible(true);method.invoke(screen,DomainItems.JUJUTSU_TAB.get());break;}}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
            }
        }
        if(ticks==217)Screenshot.grab(mc.gameDirectory,"creative-tab.png",mc.getMainRenderTarget(),c->{});
        if(ticks==220)mc.setScreen(null);
        if(ticks==225){
            var advanced=mc.level.getEntitiesOfClass(JujutsuNpc.class,new AABB(19,-62,-1,25,-56,2));require(advanced.size()==2&&advanced.stream().allMatch(JujutsuNpc::isAwakened),"advanced egg forms synchronize to client");
            var clientActors=mc.level.getEntitiesOfClass(JujutsuNpc.class,new AABB(-10,-65,-10,10,-50,10));
            require(clientActors.stream().allMatch(JujutsuNpc::isAwakened),"transformed form synchronized to all client actors");
            require(mc.level.getEntitiesOfClass(JujutsuNpc.class,new AABB(-10,-65,-10,10,-50,10)).size()==3,"all actors synchronized to client");
            for(var name:new String[]{"student_gojo","shinjuku_gojo","sukuna_16","true_sukuna"}){
                var texture=new net.minecraft.resources.ResourceLocation(UnlimitedVoid.ID,"textures/entity/"+name+".png");
                require(mc.getResourceManager().getResource(texture).isPresent(),"skin resource present "+name);
            }
            if(Boolean.getBoolean("uv.shaderTest"))try{var manager=Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager").invoke(null);var pipe=manager.getClass().getMethod("getPipelineNullable").invoke(manager);require(pipe!=null&&pipe.getClass().getSimpleName().equals("IrisRenderingPipeline"),"real shader pipeline active");}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
            LogUtils.getLogger().info("NPC_TEST_COMPLETE shader={}",Boolean.getBoolean("uv.shaderTest"));mc.stop();
        }
    }
    static void camera(Minecraft mc,double x,double y,double z,float yaw,float pitch){
        mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(mc.player);
        mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.teleportTo(p.serverLevel(),x,y,z,yaw,pitch);});
    }
}
