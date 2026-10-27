package dev.unlimitedvoid.client;

import dev.unlimitedvoid.*;
import dev.unlimitedvoid.network.LimitlessNetwork;
import dev.unlimitedvoid.server.LimitlessService;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.*;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class LimitlessClient {
    private static final KeyMapping BLUE=key("blue",GLFW.GLFW_KEY_Z),RED=key("red",GLFW.GLFW_KEY_X),PURPLE=key("purple",GLFW.GLFW_KEY_C);
    private static final Map<UUID,Effect> EFFECTS=new LinkedHashMap<>();
    private static ClientLevel lastLevel;private static int[] cooldowns=new int[3];private static boolean blueDown;private static long lastSnapshot;
    public record Pose(LimitlessKind kind,float weight,float progress){}
    private static KeyMapping key(String name,int key){return new KeyMapping("key.unlimitedvoid."+name,InputConstants.Type.KEYSYM,key,"category.unlimitedvoid");}
    static Collection<Effect> effects(){return EFFECTS.values();}
    public static int effectCount(){return EFFECTS.size();}
    public static Pose pose(UUID owner){
        var mc=Minecraft.getInstance();if(mc.level==null)return null;
        for(var e:EFFECTS.values())if(e.state.owner().equals(owner)){
            var s=e.state;float age=mc.level.getGameTime()-s.start();float progress=LimitlessKind.smooth(age/s.kind().charge);
            float weight=s.phase()==0?LimitlessKind.smooth(age/6):s.phase()==1?1-LimitlessKind.smooth((mc.level.getGameTime()-s.phaseStart())/8f):0;
            if(weight>0)return new Pose(s.kind(),weight,progress);
        }return null;
    }
    public static void receive(List<LimitlessService.State> states,int blue,int red,int purple){
        var mc=Minecraft.getInstance();if(mc.level==null)return;if(mc.level!=lastLevel)clear(mc.level);
        lastSnapshot=mc.level.getGameTime();cooldowns=new int[]{blue,red,purple};var live=new HashSet<UUID>();
        for(var s:states){
            live.add(s.id());var e=EFFECTS.get(s.id());boolean fresh=e==null;
            if(fresh){e=new Effect(s,lastSnapshot);EFFECTS.put(s.id(),e);}else{e.previous=e.position(0);e.received=lastSnapshot;e.state=s;}
            int age=(int)(lastSnapshot-s.start()),phaseAge=(int)(lastSnapshot-s.phaseStart());
            if(fresh&&s.phase()==0&&age<8)e.sound("limitless_charge",.6f);
            if(!e.launched&&s.phase()>=1){e.launched=true;e.stop();if(s.phase()==1&&phaseAge<6)e.sound(s.kind().name().toLowerCase(Locale.ROOT)+"_cast",1);}
        }
        var it=EFFECTS.entrySet().iterator();while(it.hasNext()){var e=it.next();if(!live.contains(e.getKey())){e.getValue().stop();it.remove();}}
    }
    private static void clear(ClientLevel level){EFFECTS.values().forEach(Effect::stop);EFFECTS.clear();cooldowns=new int[3];blueDown=false;lastLevel=level;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.level!=lastLevel)clear(mc.level);if(mc.level==null||mc.player==null)return;
        boolean usable=mc.screen==null&&!mc.isPaused();
        while(BLUE.consumeClick())if(usable){LimitlessNetwork.request(LimitlessKind.BLUE);blueDown=true;}
        while(RED.consumeClick())if(usable)LimitlessNetwork.request(LimitlessKind.RED);
        while(PURPLE.consumeClick())if(usable)LimitlessNetwork.request(LimitlessKind.PURPLE);
        if(blueDown&&(!BLUE.isDown()||!usable)){LimitlessNetwork.release();blueDown=false;}
        if(mc.isPaused())return;for(int i=0;i<3;i++)if(cooldowns[i]>0)cooldowns[i]--;
        if(mc.level.getGameTime()-lastSnapshot>30){EFFECTS.values().forEach(Effect::stop);EFFECTS.clear();}
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){if(e.getStage()==RenderLevelStageEvent.Stage.AFTER_PARTICLES&&!EFFECTS.isEmpty())LimitlessRenderer.render(e,EFFECTS.values());}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){var mc=Minecraft.getInstance();if(mc.player==null||mc.options.hideGui)return;
        var keys=new KeyMapping[]{BLUE,RED,PURPLE};int[] colors={0x75CFFF,0xFF7878,0xD596FF};
        for(var kind:LimitlessKind.values()){int i=kind.ordinal();Component state=Component.translatable(cooldowns[i]>0?"hud.unlimitedvoid.fuga_cooldown":"hud.unlimitedvoid.fuga_ready",(cooldowns[i]+19)/20);
            e.getGuiGraphics().drawString(mc.font,Component.translatable("hud.unlimitedvoid.limitless",Component.translatable("technique.unlimitedvoid."+kind.name().toLowerCase(Locale.ROOT)),keys[i].getTranslatedKeyMessage(),state),8,96+i*12,colors[i],true);
        }
    }
    static final class Effect {
        LimitlessService.State state;Vec3 previous;long received;boolean launched;final List<SoundInstance> sounds=new ArrayList<>();
        Effect(LimitlessService.State state,long now){this.state=state;previous=state.position();received=now;}
        Vec3 position(float partial){var mc=Minecraft.getInstance();return previous.lerp(state.position(),Math.max(0,Math.min(1,(mc.level.getGameTime()+partial-received)/2.)));}
        void sound(String name,float volume){var mc=Minecraft.getInstance();var p=state.position();var sound=new SimpleSoundInstance(new ResourceLocation(UnlimitedVoid.ID,name),SoundSource.PLAYERS,DomainSounds.gain(volume),1,RandomSource.create(),false,0,SoundInstance.Attenuation.LINEAR,p.x,p.y,p.z,false);sounds.add(sound);mc.getSoundManager().play(sound);}
        void stop(){for(var s:sounds)Minecraft.getInstance().getSoundManager().stop(s);sounds.clear();}
    }
    @Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {@SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(BLUE);e.register(RED);e.register(PURPLE);}}
}
