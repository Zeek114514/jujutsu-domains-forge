package dev.unlimitedvoid.server;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import dev.unlimitedvoid.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=UnlimitedVoid.ID)
public final class DomainCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("domain").requires(s->s.hasPermission(2))
            .then(Commands.literal("clash").executes(c->{
                int n=DomainService.generateClash(c.getSource().getPlayerOrException());
                c.getSource().sendSuccess(()->Component.literal(n==2?"已生成无量空处 × 伏魔御厨子对峙；台词后展开。/domain clear 清理生成的领域。":"领域数量已达上限或区块尚未加载。"),false);return n;
            }))
            .then(Commands.literal("clear").executes(c->{
                int n=DomainService.clearGenerated(c.getSource().getPlayerOrException());
                c.getSource().sendSuccess(()->Component.literal("生成的领域开始退场："+n),false);return n;
            }))
            .then(Commands.literal("spawn")
                .then(spawn("void",DomainKind.VOID))
                .then(spawn("shrine",DomainKind.SHRINE)))
            .then(Commands.literal("energy").then(Commands.argument("targets",EntityArgument.entities())
                .then(Commands.argument("value",DoubleArgumentType.doubleArg(0,1000000)).executes(c->{
                    double value=DoubleArgumentType.getDouble(c,"value");
                    var targets=EntityArgument.getEntities(c,"targets");
                    for(var entity:targets) entity.getPersistentData().putDouble("uv_cursed_energy",value);
                    c.getSource().sendSuccess(()->Component.translatable("message.unlimitedvoid.energy",targets.size(),value),true);
                    return targets.size();
                }))))
            .then(Commands.literal("terrain").then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c->{
                boolean enabled=BoolArgumentType.getBool(c,"enabled");
                DomainConfig.TERRAIN_DAMAGE.set(enabled); DomainConfig.SPEC.save();
                c.getSource().sendSuccess(()->Component.translatable("message.unlimitedvoid.terrain",enabled),true);
                return 1;
            }))));
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> spawn(String name,DomainKind kind){
        return Commands.literal(name).executes(c->{var p=c.getSource().getPlayerOrException();return DomainService.generate(p,kind,p.position().add(0,1,0),p.getLookAngle());})
            .then(Commands.argument("position",Vec3Argument.vec3()).executes(c->{var p=c.getSource().getPlayerOrException();return DomainService.generate(p,kind,Vec3Argument.getVec3(c,"position").add(0,1,0),p.getLookAngle());}));
    }
}
