package dev.unlimitedvoid.client;

import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.InnocentSunDog;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class SunDogPrankVisualTest {
    static boolean started;static int warmup,ticks;static InnocentSunDog actor,partner;
    static volatile boolean follow,landed,finished;static double firstDistance;static long kickStart=-1;
    static boolean windupShot,contactShot;
    static void check(boolean ok,String msg){if(!ok)throw new AssertionError("PRANK_FAIL "+msg);LogUtils.getLogger().info("PRANK_PASS {}",msg);}
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("uv.sunDogPrankTest")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(!started&&mc.screen instanceof TitleScreen){started=true;mc.createWorldOpenFlows().createFreshLevel("Prank-QA-"+System.currentTimeMillis(),new LevelSettings("Prank QA",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());}
        if(mc.player==null||mc.level==null||mc.screen!=null)return;if(warmup++<140)return;ticks++;
        if(ticks==1){
            check(InnocentSunDog.shouldPrank(0)&&InnocentSunDog.shouldPrank(.1999f)&&!InnocentSunDog.shouldPrank(.2f)&&!InnocentSunDog.shouldPrank(.99f),"20 percent threshold");
            CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(),true,mc.level.registryAccess());
            check(DomainItems.JUJUTSU_TAB.get().getDisplayItems().size()==11,"eleven items including both photo eggs");
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var level=p.serverLevel();
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);level.setDayTime(6000);
                for(int i=0;i<2;i++){
                    var pos=new BlockPos(i*2,-61,0);level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
                    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(i==0?DomainItems.INNOCENT_SUN_DOG.get():DomainItems.SUN_DOG_SERVANT.get()));
                    check(p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false))).consumesAction(),"real egg use "+i);
                }
                var all=level.getEntitiesOfClass(InnocentSunDog.class,new AABB(-5,-62,-5,6,-55,5));
                actor=all.stream().filter(InnocentSunDog::isSunDog).findFirst().orElseThrow();partner=all.stream().filter(e->!e.isSunDog()).findFirst().orElseThrow();
                // Fail rolls during initial binding, so default follow is tested before any kick.
                actor.getRandom().setSeed(0);partner.setYRot(0);partner.setYBodyRot(0);partner.setYHeadRot(0);
                p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setGameMode(GameType.SPECTATOR);p.teleportTo(level,5,-60,-5,40,2);
            });
        }
        if(mc.level!=null){
            var a=mc.level.getEntitiesOfClass(InnocentSunDog.class,new AABB(-15,-64,-15,15,-50,15)).stream().filter(InnocentSunDog::isSunDog).findFirst().orElse(null);
            if(a!=null){float age=a.kickAge(0);
                if(age>=5&&age<10&&!windupShot){windupShot=true;mc.options.hideGui=true;Screenshot.grab(mc.gameDirectory,"prank-windup.png",mc.getMainRenderTarget(),c->{});}
                if(age>=12&&age<18&&!contactShot){contactShot=true;Screenshot.grab(mc.gameDirectory,"prank-contact.png",mc.getMainRenderTarget(),c->{});}
            }
        }
        if(finished){check(windupShot&&contactShot,"synchronized windup and contact captured");LogUtils.getLogger().info("PRANK_TEST_COMPLETE");mc.stop();}
        if(ticks>700)throw new AssertionError("PRANK_FAIL timeout");
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent event){
        if(!Boolean.getBoolean("uv.sunDogPrankTest")||actor==null||event.phase!=TickEvent.Phase.END||finished)return;
        if(ticks%40==0){var tag=new net.minecraft.nbt.CompoundTag();actor.save(tag);LogUtils.getLogger().info("PRANK_DEBUG tick={} actor={} partner={} yaw={} leash={} noAI={} delay={} age={}",ticks,actor.position(),partner.position(),partner.yBodyRot,partner.getLeashHolder()==actor,actor.isNoAi(),tag.getInt("PrankDelay"),actor.kickAge(0));try{var f=InnocentSunDog.class.getDeclaredField("prankActive");f.setAccessible(true);LogUtils.getLogger().info("PRANK_DEBUG active={} ground={}/{} LOS={} partnerNoAI={} navDone={}",f.getBoolean(actor),actor.onGround(),partner.onGround(),actor.hasLineOfSight(partner),partner.isNoAi(),actor.getNavigation().isDone());}catch(Exception ex){throw new RuntimeException(ex);}}
        if(ticks==30){check(partner.getLeashHolder()==actor,"automatic vanilla leash holder");actor.setNoAi(true);actor.setPos(-2.5,-60,.5);firstDistance=partner.distanceTo(actor);}
        if(ticks==75){
            check(partner.distanceTo(actor)<firstDistance-.3,"partner follows holder by navigation");follow=true;
            actor.setPos(.5,-60,-1.3);partner.setPos(.5,-60,.5);partner.setYRot(0);partner.setYBodyRot(0);partner.setYHeadRot(0);
            // Keep the existing saved cooldown until the scheduled roll, then bias RNG for a reproducible demo.
            actor.setNoAi(false);
        }
        if(follow&&kickStart<0){
            // QA only: advance eligible polls rapidly; production still polls once per 160 ticks.
            actor.getRandom().setSeed(actor.level().getGameTime()*17);
            try{var field=InnocentSunDog.class.getDeclaredField("nextPrankAt");field.setAccessible(true);field.setLong(actor,0);}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
        }
        float age=actor.kickAge(0);
        if(follow&&age>=0&&age<InnocentSunDog.KICK_DURATION){
            if(kickStart<0){kickStart=actor.level().getGameTime();check(partner.getLeashHolder()==actor,"leash retained during kick");}
            if(age<InnocentSunDog.KICK_CONTACT)check(partner.getHealth()==100&&partner.recoilAge(0)>16,"no early contact");
            if(partner.recoilAge(0)>=0&&partner.recoilAge(0)<3&&!landed){landed=true;check(age>=InnocentSunDog.KICK_CONTACT&&age<=InnocentSunDog.KICK_CONTACT+2,"recoil begins at contact frame");check(partner.getDeltaMovement().horizontalDistanceSqr()>.001,"real knockback");}
        }
        if(kickStart>=0&&actor.level().getGameTime()-kickStart>35){
            check(landed,"kick made actual contact");check(actor.getHealth()==100&&partner.getHealth()==100,"no health damage");check(partner.getLeashHolder()==actor,"follow binding retained after recovery");
            var tag=new net.minecraft.nbt.CompoundTag();actor.save(tag);check(tag.getInt("PrankDelay")>0,"cooldown persists in NBT");finished=true;
        }
    }
}
