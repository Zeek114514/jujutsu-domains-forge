package dev.unlimitedvoid.client;

import com.mojang.logging.LogUtils;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.entity.JujutsuNpc;
import dev.unlimitedvoid.server.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.function.Consumer;

/** Real Player.attack calls against server entities, including production damage-source mixin. */
@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID,value=Dist.CLIENT)
public final class CursedToolsVisualTest {
    static boolean started;static int warmup,ticks,checks;static volatile boolean done;
    static JujutsuNpc gojo;static Husk armored,cloud,dummy;static float before;static ItemStack cloudStack;
    static void check(boolean ok,String message){if(!ok)throw new AssertionError("CURSED_TOOLS_FAIL "+message);checks++;LogUtils.getLogger().info("CURSED_TOOLS_PASS {}",message);}
    static void server(Minecraft mc,Consumer<ServerPlayer> action){mc.getSingleplayerServer().execute(()->{try{action.accept(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0));}catch(Throwable error){LogUtils.getLogger().error("CURSED_TOOLS_FAIL",error);mc.execute(mc::stop);}});}
    static void equip(ServerPlayer p,Item item){p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(item));}
    static Husk husk(ServerPlayer p,double x,double z){var h=new Husk(EntityType.HUSK,p.serverLevel());h.moveTo(x,-60,z,180,0);h.setNoAi(true);h.setNoGravity(true);h.setPersistenceRequired();h.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);h.getAttribute(Attributes.ARMOR).setBaseValue(0);h.setHealth(1000);p.serverLevel().addFreshEntity(h);return h;}
    static float strike(ServerPlayer p,LivingEntity target){target.invulnerableTime=0;target.moveTo(0,-60,2,180,0);p.teleportTo(p.serverLevel(),0,-60,0,0,0);p.setSprinting(false);float hp=target.getHealth();p.attack(target);return hp-target.getHealth();}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("uv.cursedToolsTest")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(!started&&mc.screen instanceof TitleScreen){started=true;mc.createWorldOpenFlows().createFreshLevel("Cursed-Tools-QA-"+System.currentTimeMillis(),new LevelSettings("Cursed Tools QA",GameType.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(12345,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());}
        if(mc.player==null||mc.level==null)return;if(mc.screen!=null&&!(mc.screen instanceof Preview))return;
        if(warmup++<200){if(warmup==1)mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);return;}ticks++;
        if(ticks==10)server(mc,p->{
            p.serverLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);p.serverLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);p.serverLevel().setDayTime(6000);
            p.setGameMode(GameType.SURVIVAL);p.teleportTo(p.serverLevel(),0,-60,0,0,0);p.setNoGravity(true);
            gojo=DomainEntities.STUDENT_GOJO.get().create(p.serverLevel());gojo.moveTo(0,-60,2,180,0);gojo.setNoAi(true);p.serverLevel().addFreshEntity(gojo);dummy=husk(p,6,6);equip(p,Items.DIAMOND_SWORD);
            for(var item:new Item[]{DomainItems.INVERTED_SPEAR.get(),DomainItems.SPLIT_SOUL_KATANA.get(),DomainItems.PLAYFUL_CLOUD.get()})check(item instanceof dev.unlimitedvoid.item.CursedToolItem,"registered tool "+item);
        });
        if(ticks==35)server(mc,p->{check(strike(p,gojo)==0,"ordinary sword blocked by Infinity");equip(p,DomainItems.INVERTED_SPEAR.get());});
        if(ticks==55)server(mc,p->{
            gojo.setNoAi(false);check(DomainService.expandNpc(gojo,true),"active domain setup");gojo.setNoAi(true);
            check(NpcTechniqueService.start(gojo,LimitlessKind.BLUE,dummy,false),"Gojo charge setup");
            check(strike(p,gojo)>0,"spear pierces Infinity");check(CursedToolCombat.nullified(gojo),"spear nullification active");
            check(!NpcTechniqueService.charging(gojo),"spear interrupts Gojo charge");check(!NpcTechniqueService.start(gojo,LimitlessKind.RED,dummy,false),"suppression prevents immediate recast");
            check(DomainService.snapshot(p).stream().anyMatch(d->d.owner().equals(gojo.getUUID())&&d.closing()>=0),"contact cancels domain through closing transition");
            check(p.getMainHandItem().getDamageValue()==1,"successful spear strike consumes durability");equip(p,Items.DIAMOND_SWORD);
        });
        if(ticks==75)server(mc,p->{check(strike(p,gojo)>0,"ordinary strike lands while Infinity suppressed");equip(p,DomainItems.SPLIT_SOUL_KATANA.get());});
        if(ticks==105)server(mc,p->{
            check(!CursedToolCombat.nullified(gojo),"nullification expires");check(strike(p,gojo)==0,"soul katana does not bypass recovered Infinity");
            gojo.discard();dummy.discard();armored=husk(p,0,2);armored.getAttribute(Attributes.ARMOR).setBaseValue(20);armored.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(8);
            armored.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));armored.getItemBySlot(EquipmentSlot.CHEST).enchant(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION,4);
            armored.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,600,3));
        });
        if(ticks==115)server(mc,p->{float damage=strike(p,armored);check(damage>0&&damage<26,"soul perception required for full penetration");p.setShiftKeyDown(true);p.getMainHandItem().getItem().use(p.serverLevel(),p,InteractionHand.MAIN_HAND);p.setShiftKeyDown(false);check(p.hasEffect(CursedToolEffects.SOUL_SIGHT.get()),"sneak-use activates Soul Sight");});
        if(ticks==120){check(mc.player.hasEffect(CursedToolEffects.SOUL_SIGHT.get()),"Soul Sight synchronizes to viewing client");Screenshot.grab(mc.gameDirectory,"soul-sight.png",mc.getMainRenderTarget(),c->{});}
        if(ticks==135)server(mc,p->{float damage=strike(p,armored);check(Math.abs(damage-26)<.05,"perceived soul bypasses armor, resistance and protection: "+damage);check(p.getMainHandItem().getDamageValue()==2,"soul source preserves vanilla durability");float hp=armored.getHealth();armored.heal(20);check(armored.getHealth()>hp,"no invented blanket healing prohibition");armored.moveTo(8,-60,2,180,0);cloud=husk(p,0,2);equip(p,DomainItems.PLAYFUL_CLOUD.get());cloudStack=p.getMainHandItem();});
        if(ticks==160||ticks==180||ticks==200||ticks==220){int hit=ticks;server(mc,p->{
            float damage=strike(p,cloud),expected=hit==200?56:22;check(Math.abs(damage-expected)<.05,"cloud physical damage at hit "+hit+": "+damage);
            if(hit==200)check(cloud.getDeltaMovement().horizontalDistanceSqr()>.01,"cloud finisher knockback");
            if(hit==180)p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,400,1));
            if(hit==200)p.removeEffect(MobEffects.DAMAGE_BOOST);
            if(hit==220){float hp=cloud.getHealth();p.attack(cloud);check(cloud.getHealth()==hp,"spam hit respects vanilla hurt cooldown");}
        });}
        if(ticks==240)server(mc,p->{p.resetAttackStrengthTicker();float damage=strike(p,cloud);check(damage>0&&damage<22,"weak cloud swing is weaker than a charged physical strike");p.removeEffect(CursedToolEffects.SOUL_SIGHT.get());equip(p,DomainItems.SPLIT_SOUL_KATANA.get());var ordinary=p.damageSources().playerAttack(p);check(CursedToolCombat.meleeSource(p,ordinary)==ordinary,"ending Soul Sight removes soul bypass");});
        if(ticks==270)server(mc,p->{check(!p.hasEffect(MobEffects.DAMAGE_BOOST),"physical strength removed cleanly");cloud.moveTo(8,-60,5,180,0);equip(p,DomainItems.INVERTED_SPEAR.get());done=true;});
        if(ticks==275)Screenshot.grab(mc.gameDirectory,"inverted-spear-first-person.png",mc.getMainRenderTarget(),c->{});
        if(ticks==276)server(mc,p->equip(p,DomainItems.SPLIT_SOUL_KATANA.get()));
        if(ticks==282)Screenshot.grab(mc.gameDirectory,"split-soul-first-person.png",mc.getMainRenderTarget(),c->{});
        if(ticks==283)server(mc,p->equip(p,DomainItems.PLAYFUL_CLOUD.get()));
        if(ticks==290)Screenshot.grab(mc.gameDirectory,"playful-cloud-first-person.png",mc.getMainRenderTarget(),c->{});
        if(ticks==295)server(mc,p->{p.getMainHandItem().getItem().use(p.serverLevel(),p,InteractionHand.MAIN_HAND);check(dev.unlimitedvoid.item.CursedToolItem.cloudForm(p.getMainHandItem())==1,"right-click selects whole staff");});
        if(ticks==315)server(mc,p->{check(Math.abs(strike(p,cloud)-24)<.05,"whole staff physical damage");check(Math.abs(p.getAttributeValue(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get())-4)<.05,"whole staff reach");p.setShiftKeyDown(true);p.getMainHandItem().getItem().use(p.serverLevel(),p,InteractionHand.MAIN_HAND);p.setShiftKeyDown(false);check(dev.unlimitedvoid.item.CursedToolItem.dual(p),"active sharpening selects dual blades with empty offhand");});
        if(ticks==330)server(mc,p->{check(Math.abs(strike(p,cloud)-16)<.05,"dual blades physical damage");check(Math.abs(p.getAttributeValue(Attributes.ATTACK_SPEED)-2.6)<.05,"dual blades attack faster");});
        if(ticks==335)Screenshot.grab(mc.gameDirectory,"cloud-dual-first-person.png",mc.getMainRenderTarget(),c->{});
        if(ticks==336)mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if(ticks==342)Screenshot.grab(mc.gameDirectory,"cloud-dual-third-person.png",mc.getMainRenderTarget(),c->{});
        if(ticks==345)mc.options.setCameraType(CameraType.FIRST_PERSON);
        if(ticks==350)server(mc,p->{p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));var stack=p.getMainHandItem();stack.getItem().inventoryTick(stack,p.serverLevel(),p,0,true);check(dev.unlimitedvoid.item.CursedToolItem.cloudForm(stack)==0&&p.getOffhandItem().is(Items.SHIELD),"occupied offhand safely retracts dual mode without replacing items");p.setShiftKeyDown(true);stack.getItem().use(p.serverLevel(),p,InteractionHand.MAIN_HAND);p.setShiftKeyDown(false);check(dev.unlimitedvoid.item.CursedToolItem.cloudForm(stack)==0,"sharpening rejected when offhand occupied");p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);stack.getItem().use(p.serverLevel(),p,InteractionHand.MAIN_HAND);check(dev.unlimitedvoid.item.CursedToolItem.cloudForm(ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag())))==1,"staff form survives item NBT save/load");});
        if(ticks==365&&done){mc.getToasts().clear();mc.setScreen(new Preview());}
        if(ticks==370)Screenshot.grab(mc.gameDirectory,"cursed-tools-preview.png",mc.getMainRenderTarget(),c->{});
        if(ticks==375)mc.setScreen(new CloudFormsPreview());
        if(ticks==380)Screenshot.grab(mc.gameDirectory,"playful-cloud-forms.png",mc.getMainRenderTarget(),c->{});
        if(ticks==385&&done){
            for(var item:new Item[]{DomainItems.INVERTED_SPEAR.get(),DomainItems.SPLIT_SOUL_KATANA.get(),DomainItems.PLAYFUL_CLOUD.get()}){
                var model=mc.getItemRenderer().getModel(new ItemStack(item),mc.level,mc.player,0);
                check(model!=mc.getModelManager().getMissingModel()&&!model.getQuads(null,null,net.minecraft.util.RandomSource.create()).isEmpty(),"nonempty baked item model "+item);
            }
            var prior=mc.getItemRenderer().getModel(new ItemStack(DomainItems.PLAYFUL_CLOUD.get()),mc.level,mc.player,0);
            for(int form=1;form<=2;form++){
                var stack=new ItemStack(DomainItems.PLAYFUL_CLOUD.get());stack.getOrCreateTag().putInt("uv_cloud_form",form);
                var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);check(model!=prior&&!model.getQuads(null,null,net.minecraft.util.RandomSource.create()).isEmpty(),"distinct nonempty cloud form model "+form);prior=model;
            }
            LogUtils.getLogger().info("CURSED_TOOLS_TEST_COMPLETE checks={}",checks);mc.stop();
        }
    }
    static final class CloudFormsPreview extends Preview {
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            g.fill(0,0,width,height,0xff11151e);g.drawCenteredString(font,Component.literal("游云 · 三种物理形态"),width/2,26,0xf5dc9c);
            String[] labels={"三节棍","整棍","主动削尖 · 双刀"};String[] hints={"右键切换整棍","右键收回三节","潜行右键削尖 / 收回"};
            for(int i=0;i<3;i++){
                int x=width*(i*2+1)/6,y=height/2;g.fill(x-width/6+5,60,x+width/6-5,height-35,0xff202838);
                var stack=new ItemStack(DomainItems.PLAYFUL_CLOUD.get());stack.getOrCreateTag().putInt("uv_cloud_form",i);
                int copies=i==2?2:1;
                for(int j=0;j<copies;j++){g.pose().pushPose();g.pose().translate(x-(i==2?52:40)+(i==2?j*42:0),y-48,0);g.pose().scale(i==2?3.8f:5,i==2?3.8f:5,i==2?3.8f:5);g.renderItem(stack,0,0);g.pose().popPose();}
            }
            g.flush();g.pose().pushPose();g.pose().translate(0,0,500);
            for(int i=0;i<3;i++){int x=width*(i*2+1)/6,y=height/2;g.drawCenteredString(font,labels[i],x,y+58,0xffdd96);g.drawCenteredString(font,hints[i],x,y+78,0xaebccd);}g.pose().popPose();
        }
    }
    static class Preview extends Screen {
        Preview(){super(Component.literal("特级咒具"));}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            g.fill(0,0,width,height,0xff11151e);g.drawCenteredString(font,Component.literal("特级咒具 · 第一批"),width/2,26,0xf5dc9c);
            Item[] items={DomainItems.INVERTED_SPEAR.get(),DomainItems.SPLIT_SOUL_KATANA.get(),DomainItems.PLAYFUL_CLOUD.get()};
            String[] captions={"接触强制解除术式","感知魂魄 · 无视硬度","纯物理 · 随力量增强"};
            for(int i=0;i<3;i++){
                int x=width*(i*2+1)/6,y=height/2;g.fill(x-width/6+5,60,x+width/6-5,height-35,0xff202838);
                g.pose().pushPose();g.pose().translate(x-40,y-48,0);g.pose().scale(5,5,5);g.renderItem(new ItemStack(items[i]),0,0);g.pose().popPose();
            }
            g.flush();g.pose().pushPose();g.pose().translate(0,0,500);
            for(int i=0;i<3;i++){
                int x=width*(i*2+1)/6,y=height/2;
                g.drawCenteredString(font,items[i].getDescription(),x,y+58,0xffdd96);g.drawCenteredString(font,captions[i],x,y+78,0xaebccd);
            }
            g.pose().popPose();
        }
    }
}
