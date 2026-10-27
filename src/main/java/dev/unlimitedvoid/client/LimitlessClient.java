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
    private record Tail(UUID owner,long expires){}
    private static final Map<SoundInstance,Tail> TAILS=new LinkedHashMap<>();
    private static ClientLevel lastLevel;private static int[] cooldowns=new int[3];private static boolean blueDown;private static long lastSnapshot;
    public record Pose(LimitlessKind kind,float weight,float progress){}
    private static KeyMapping key(String name,int key){return new KeyMapping("key.unlimitedvoid."+name,InputConstants.Type.KEYSYM,key,"category.unlimitedvoid");}
    static Collection<Effect> effects(){return EFFECTS.values();}
    public static int effectCount(){return EFFECTS.size();}
    static int tailCount(){return TAILS.size();}
    private static net.minecraft.world.entity.player.Player owner(UUID id){var mc=Minecraft.getInstance();return mc.player!=null&&mc.player.getUUID().equals(id)?mc.player:mc.level.getPlayerByUUID(id);}
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
            if(fresh&&s.phase()==0&&age<8)e.charge=e.sound(e.name("charge"),.4f);
            if(fresh&&s.phase()==PurpleDetonation.GROWING&&phaseAge<8)e.charge=e.sound("purple_charge",.6f);
            e.voiceIfDue();
            if(!e.launched&&s.phase()>=1&&s.phase()<PurpleDetonation.GROWING){e.launched=true;e.stopCharge();if(s.phase()==1&&phaseAge<6)e.sound(e.name("cast"),.8f);}
            if(!e.impacted&&(s.phase()==2||s.phase()==PurpleDetonation.BLAST)){e.impacted=true;e.stopCharge();if(phaseAge<6)e.sound(e.name("impact"),1);}
        }
        var it=EFFECTS.entrySet().iterator();while(it.hasNext()){var e=it.next();if(!live.contains(e.getKey())){e.getValue().retire();it.remove();}}
    }
    private static void clear(ClientLevel level){EFFECTS.values().forEach(Effect::stop);EFFECTS.clear();for(var sound:TAILS.keySet())stopSound(sound);TAILS.clear();LimitlessRenderer.clear();cooldowns=new int[3];blueDown=false;lastLevel=level;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.level!=lastLevel)clear(mc.level);if(mc.level==null||mc.player==null)return;
        boolean usable=mc.screen==null&&!mc.isPaused();
        while(BLUE.consumeClick())if(usable){LimitlessNetwork.request(LimitlessKind.BLUE);blueDown=true;}
        while(RED.consumeClick())if(usable)LimitlessNetwork.request(LimitlessKind.RED);
        while(PURPLE.consumeClick())if(usable)LimitlessNetwork.request(LimitlessKind.PURPLE);
        if(blueDown&&(!BLUE.isDown()||!usable)){LimitlessNetwork.release();blueDown=false;}
        if(mc.isPaused())return;for(int i=0;i<3;i++)if(cooldowns[i]>0)cooldowns[i]--;
        for(var effect:EFFECTS.values())effect.voiceIfDue();
        var tails=TAILS.entrySet().iterator();while(tails.hasNext()){
            var tail=tails.next();var owner=owner(tail.getValue().owner());
            if(owner==null||!owner.isAlive()||owner.isSpectator()||mc.level.getGameTime()>=tail.getValue().expires()){stopSound(tail.getKey());tails.remove();}
        }
        if(mc.level.getGameTime()-lastSnapshot>30){EFFECTS.values().forEach(Effect::stop);EFFECTS.clear();}
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){if(e.getStage()==RenderLevelStageEvent.Stage.AFTER_PARTICLES)LimitlessRenderer.prepare(e,EFFECTS.values());}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){var mc=Minecraft.getInstance();if(mc.player==null||mc.options.hideGui)return;
        var keys=new KeyMapping[]{BLUE,RED,PURPLE};int[] colors={0x75CFFF,0xFF7878,0xD596FF};
        for(var kind:LimitlessKind.values()){int i=kind.ordinal();Component state=Component.translatable(cooldowns[i]>0?"hud.unlimitedvoid.fuga_cooldown":"hud.unlimitedvoid.fuga_ready",(cooldowns[i]+19)/20);
            e.getGuiGraphics().drawString(mc.font,Component.translatable("hud.unlimitedvoid.limitless",Component.translatable("technique.unlimitedvoid."+kind.name().toLowerCase(Locale.ROOT)),keys[i].getTranslatedKeyMessage(),state),8,96+i*12,colors[i],true);
        }
        for(var effect:EFFECTS.values())if(effect.state.owner().equals(mc.player.getUUID())&&effect.state.phase()>=PurpleDetonation.GROWING){
            e.getGuiGraphics().drawString(mc.font,Component.translatable(effect.state.phase()==PurpleDetonation.GROWING?"hud.unlimitedvoid.purple_growing":"hud.unlimitedvoid.purple_blast"),8,136,0xE9B4FF,true);break;
        }
    }
    @SubscribeEvent public static void shake(ViewportEvent.ComputeCameraAngles e){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.isPaused())return;double strength=0;
        for(var effect:EFFECTS.values())if(effect.state.phase()==PurpleDetonation.BLAST){
            double age=mc.level.getGameTime()+mc.getFrameTime()-effect.state.phaseStart();
            double distance=effect.state.position().distanceTo(mc.gameRenderer.getMainCamera().getPosition());
            strength=Math.max(strength,Math.max(0,1-age/24)*Math.max(0,1-distance/(effect.state.blastRadius()*1.6)));
        }
        float time=mc.level.getGameTime()+mc.getFrameTime(),weight=(float)(strength*mc.options.screenEffectScale().get());
        e.setPitch(e.getPitch()+(float)Math.sin(time*2.3)*weight);e.setRoll(e.getRoll()+(float)Math.sin(time*1.8)*weight*.6f);
    }
    static final class Effect {
        LimitlessService.State state;Vec3 previous;long received;boolean launched,voiced,impacted;SoundInstance charge;final List<SoundInstance> sounds=new ArrayList<>();
        Effect(LimitlessService.State state,long now){this.state=state;previous=state.position();received=now;}
        Vec3 position(float partial){var mc=Minecraft.getInstance();return previous.lerp(state.position(),Math.max(0,Math.min(1,(mc.level.getGameTime()+partial-received)/2.)));}
        String name(String suffix){return state.kind().name().toLowerCase(Locale.ROOT)+"_"+suffix;}
        void voiceIfDue(){
            if(voiced)return;var mc=Minecraft.getInstance();long age=mc.level.getGameTime()-state.start();int offset=state.kind()==LimitlessKind.PURPLE?26:0;
            if(age<offset)return;voiced=true;if(state.phase()==0&&age<=offset+6)sound(name("voice"),.95f);
        }
        SoundInstance sound(String name,float volume){var mc=Minecraft.getInstance();var sound=new Cue(state.id(),name,state.position(),volume);sounds.add(sound);mc.getSoundManager().play(sound);return sound;}
        void stopCharge(){if(charge!=null){stopSound(charge);sounds.remove(charge);charge=null;}}
        void retire(){
            var mc=Minecraft.getInstance();var owner=owner(state.owner());long now=mc.level.getGameTime();
            boolean natural=(state.phase()==2&&now-state.phaseStart()>=(state.kind()==LimitlessKind.BLUE?78:15)||state.phase()==PurpleDetonation.BLAST&&now-state.phaseStart()>=PurpleDetonation.BLAST_TICKS-2)&&owner!=null&&owner.isAlive()&&!owner.isSpectator();
            for(var sound:sounds){
                if(natural){
                    if(TAILS.size()>=32){var oldest=TAILS.keySet().iterator().next();stopSound(oldest);TAILS.remove(oldest);}
                    TAILS.put(sound,new Tail(state.owner(),now+100));
                }else stopSound(sound);
            }sounds.clear();
        }
        void stop(){for(var s:sounds)stopSound(s);sounds.clear();}
    }
    /** Keep cue ownership explicit across asynchronous audio callbacks and tail cleanup. */
    private static void stopSound(SoundInstance sound){if(sound instanceof Cue cue)cue.stopped=true;Minecraft.getInstance().getSoundManager().stop(sound);}
    static final class Cue extends SimpleSoundInstance implements TickableSoundInstance {
        private volatile boolean stopped;
        @Override public void tick(){}
        @Override public boolean isStopped(){return stopped;}
        @Override public boolean canPlaySound(){return !stopped;}
        final UUID cast;
        Cue(UUID cast,String name,Vec3 p,float volume){super(new ResourceLocation(UnlimitedVoid.ID,name),name.endsWith("_voice")?SoundSource.VOICE:SoundSource.PLAYERS,DomainSounds.gain(volume),1,RandomSource.create(),false,0,SoundInstance.Attenuation.LINEAR,p.x,p.y,p.z,false);this.cast=cast;}
    }
    @Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {@SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(BLUE);e.register(RED);e.register(PURPLE);}}
}
