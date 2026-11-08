package dev.unlimitedvoid.item;
import dev.unlimitedvoid.server.CursedToolCombat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import dev.unlimitedvoid.CursedToolEffects;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import com.google.common.collect.*;
/** Normal melee pipeline; effects only on landed hits. */
public final class CursedToolItem extends SwordItem {
    public enum Kind { SPEAR, SOUL, CLOUD }
    public final Kind kind;
    public static int cloudForm(ItemStack stack){return stack.getTag()==null?0:Math.max(0,Math.min(2,stack.getTag().getInt("uv_cloud_form")));}
    public static boolean dual(LivingEntity entity){return entity.getMainHandItem().getItem() instanceof CursedToolItem tool&&tool.kind==Kind.CLOUD&&cloudForm(entity.getMainHandItem())==2&&entity.getOffhandItem().isEmpty();}
    @Override public Multimap<Attribute,AttributeModifier> getAttributeModifiers(EquipmentSlot slot,ItemStack stack){
        if(kind!=Kind.CLOUD||slot!=EquipmentSlot.MAINHAND||cloudForm(stack)==0)return super.getAttributeModifiers(slot,stack);
        boolean staff=cloudForm(stack)==1;
        var b=ImmutableMultimap.<Attribute,AttributeModifier>builder();
        b.put(Attributes.ATTACK_DAMAGE,new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,"Cloud physical form",staff?23:15,AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_SPEED,new AttributeModifier(BASE_ATTACK_SPEED_UUID,"Cloud physical form",staff?-2.7:-1.4,AttributeModifier.Operation.ADDITION));
        if(staff)b.put(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get(),new AttributeModifier(java.util.UUID.fromString("7b28333e-24ad-4401-b95c-ecf111881212"),"Cloud staff reach",1,AttributeModifier.Operation.ADDITION));
        return b.build();
    }
    public CursedToolItem(Kind kind){
        super(Tiers.NETHERITE,kind==Kind.SOUL?21:kind==Kind.CLOUD?17:13,kind==Kind.SPEAR?-2.2f:kind==Kind.SOUL?-2.5f:-2.4f,new Item.Properties().rarity(Rarity.EPIC).fireResistant());this.kind=kind;
    }
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){
        boolean result=super.hurtEnemy(stack,target,attacker);
        if(!attacker.level().isClientSide)CursedToolCombat.landed(this,stack,attacker,target);return result;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(kind==Kind.CLOUD){
            ItemStack stack=player.getItemInHand(hand);int form=cloudForm(stack),next=player.isShiftKeyDown()?(form==2?0:2):(form==0?1:0);
            if(hand!=InteractionHand.MAIN_HAND)return InteractionResultHolder.pass(stack);
            if(next==2&&!player.getOffhandItem().isEmpty()){
                if(!level.isClientSide)player.displayClientMessage(Component.translatable("message.unlimitedvoid.cloud_offhand"),true);
                return InteractionResultHolder.fail(stack);
            }
            if(!level.isClientSide){stack.getOrCreateTag().putInt("uv_cloud_form",next);player.getCooldowns().addCooldown(this,12);player.displayClientMessage(Component.translatable("tooltip.unlimitedvoid.cloud_form."+next),true);}
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
        if(kind!=Kind.SOUL||!player.isShiftKeyDown())return InteractionResultHolder.pass(player.getItemInHand(hand));
        if(!level.isClientSide){player.addEffect(new MobEffectInstance(CursedToolEffects.SOUL_SIGHT.get(),600));player.getCooldowns().addCooldown(this,40);}
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
        if(!level.isClientSide&&selected&&kind==Kind.CLOUD&&cloudForm(stack)==2&&entity instanceof Player p&&!p.getOffhandItem().isEmpty())stack.getOrCreateTag().putInt("uv_cloud_form",0);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,level,lines,flag);
        lines.add(Component.translatable("tooltip.unlimitedvoid.special_grade").withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("tooltip.unlimitedvoid.cursed_tool."+kind.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
        if(kind==Kind.CLOUD){lines.add(Component.translatable("tooltip.unlimitedvoid.cloud_form."+cloudForm(stack)).withStyle(ChatFormatting.GOLD));lines.add(Component.translatable("tooltip.unlimitedvoid.cloud_controls").withStyle(ChatFormatting.GRAY));}
    }
}
