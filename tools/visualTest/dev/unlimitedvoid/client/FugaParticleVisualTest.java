package dev.unlimitedvoid.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.UnlimitedVoid;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Capture only: no original world, no release or combat claims. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class FugaParticleVisualTest {
    static boolean started;static int ticks,warmup;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("uv.fugaParticleTest")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(!started&&mc.screen instanceof TitleScreen){started=true;mc.createWorldOpenFlows().createFreshLevel("Fuga-Particle-Preview-"+System.currentTimeMillis(),new LevelSettings("Fuga Particle Preview",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());}
        if(mc.player==null||mc.level==null||mc.screen!=null)return;
        if(warmup++<400){if(warmup==1){mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.renderDistance().set(24);}return;}ticks++;
        if(ticks==20){mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var level=p.serverLevel();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);level.setDayTime(14000);p.teleportTo(level,0,-60,0,0,0);
            for(int x=-5;x<=5;x++)for(int y=-61;y<=-53;y++)level.setBlockAndUpdate(new BlockPos(x,y,54),Blocks.STONE.defaultBlockState());
            // Disposable skyline provides a real block-scale reference without using a player's save.
            for(int bx:new int[]{-95,-55,45,85})for(int bz:new int[]{-15,80}){
                int height=22+Math.floorMod(bx+bz,24);
                for(int x=0;x<15;x++)for(int z=0;z<15;z++)for(int y=0;y<height;y++){
                    if(x!=0&&z!=0&&x!=14&&z!=14&&y!=height-1)continue;
                    var block=y%5==2&&(x%4==2||z%4==2)?Blocks.SEA_LANTERN:Blocks.GRAY_CONCRETE;
                    level.setBlock(new BlockPos(bx+x,-60+y,bz+z),block.defaultBlockState(),2);
                }
            }
            for(int z=-70;z<150;z++)for(int x=-9;x<=9;x++)level.setBlock(new BlockPos(x,-61,z),(Math.abs(x)==9?Blocks.SMOOTH_STONE:Blocks.BLACK_CONCRETE).defaultBlockState(),2);
        });}
        if(ticks==35){mc.options.fov().set(70);camera(mc,7,-57,-8,35,0);}
        if(ticks==40)KeyMapping.click(InputConstants.getKey(GLFW.GLFW_KEY_H,0));
        if(ticks==79)capture(mc,"charge.png");
        if(ticks==82){mc.options.fov().set(90);mc.setCameraEntity(mc.player);}
        if(ticks==89)capture(mc,"charge-first-person.png");
        if(ticks==94){mc.options.fov().set(100);camera(mc,150,85,-180,32.7f,0);}
        if(ticks==102)capture(mc,"arrow.png");
        if(ticks==143)capture(mc,"burst.png");
        if(ticks==180)capture(mc,"smoke.png");
        if(ticks>=40&&ticks<=255)capture(mc,String.format("frame-%04d.png",ticks-40));
        // A second real server cast proves cancellation while owned particles are still alive.
        if(ticks==257)KeyMapping.click(InputConstants.getKey(GLFW.GLFW_KEY_O,0));
        if(ticks==260)KeyMapping.click(InputConstants.getKey(GLFW.GLFW_KEY_H,0));
        if(ticks==280){
            var atlas=(net.minecraft.client.renderer.texture.TextureAtlas)mc.getTextureManager().getTexture(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_PARTICLES);
            for(String sprite: new String[]{"fuga_particle","fuga_smoke"})if(atlas.getSprite(new net.minecraft.resources.ResourceLocation("unlimitedvoid",sprite)).contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()))throw new AssertionError("missing sprite "+sprite);
            if(Boolean.getBoolean("uv.shaderTest"))try{var manager=Class.forName("net.irisshaders.iris.Iris").getMethod("getPipelineManager").invoke(null);var pipeline=manager.getClass().getMethod("getPipelineNullable").invoke(manager);if(pipeline==null||!pipeline.getClass().getSimpleName().equals("IrisRenderingPipeline"))throw new AssertionError("shader pipeline inactive");}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
            if(FugaParticles.peak<=0||FugaParticles.peak>4500||FugaParticles.alive()==0)throw new AssertionError("particle preview budget or active cancel setup");LogUtils.getLogger().info("FUGA_PARTICLE_PREVIEW_COMPLETE peak={} alive={} captureOnly=true",FugaParticles.peak,FugaParticles.alive());FugaParticles.stop(mc.player.getUUID());if(FugaParticles.alive()!=0)throw new AssertionError("cancel left particles alive");LogUtils.getLogger().info("FUGA_PARTICLE_CANCEL_OK");mc.stop();}
    }
    static void camera(Minecraft mc,double x,double y,double z,float yaw,float pitch){var observer=new ArmorStand(mc.level,x,y,z);observer.moveTo(x,y,z,yaw,pitch);observer.setOldPosAndRot();observer.setYHeadRot(yaw);observer.yHeadRotO=yaw;mc.options.hideGui=true;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);}
    static void capture(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->{});}
}
