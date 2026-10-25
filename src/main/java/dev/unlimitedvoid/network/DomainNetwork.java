package dev.unlimitedvoid.network;

import dev.unlimitedvoid.UnlimitedVoid;
import dev.unlimitedvoid.DomainKind;
import dev.unlimitedvoid.client.VoidClient;
import dev.unlimitedvoid.server.DomainService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The client requests activation only. Position, membership, damage and timing are server-owned. */
public final class DomainNetwork {
    private static final String VERSION = "7";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UnlimitedVoid.ID, "domain"), () -> VERSION, VERSION::equals, VERSION::equals);

    public record DomainState(UUID owner, Vec3 center, Vec3 axis, float radius, int age, int remaining, DomainKind kind, float maxRadius, float integrity, int closing) {}
    private record Toggle() {}
    private record Cycle() {}
    private record Reset() {}
    private record Expansion(UUID caster,DomainKind kind,Vec3 center,float radius) {}
    private record Snapshot(List<DomainState> states, boolean captive, DomainKind selected, int cooldown) {}

    public static void register() {
        CHANNEL.messageBuilder(Toggle.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> {})
                .decoder(buffer -> new Toggle())
                .consumerMainThread((message, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) DomainService.toggle(player);
                }).add();
        CHANNEL.messageBuilder(Snapshot.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DomainNetwork::encode)
                .decoder(DomainNetwork::decode)
                .consumerMainThread((message, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> VoidClient.receive(message.states(), message.captive(), message.selected(), message.cooldown()))).add();
        CHANNEL.messageBuilder(Cycle.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> {}).decoder(buffer -> new Cycle())
                .consumerMainThread((message, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) DomainService.cycle(player);
                }).add();
        CHANNEL.messageBuilder(Reset.class,3,NetworkDirection.PLAY_TO_SERVER)
                .encoder((message,buffer)->{}).decoder(buffer->new Reset())
                .consumerMainThread((message,context)->{
                    ServerPlayer player=context.get().getSender();
                    if(player!=null) DomainService.resetBurnout(player);
                }).add();
        CHANNEL.messageBuilder(Expansion.class,4,NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message,buffer)->{buffer.writeUUID(message.caster());buffer.writeEnum(message.kind());vector(buffer,message.center());buffer.writeFloat(message.radius());})
                .decoder(buffer->new Expansion(buffer.readUUID(),buffer.readEnum(DomainKind.class),vector(buffer),buffer.readFloat()))
                .consumerMainThread((message,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        ()->()->dev.unlimitedvoid.client.ClientDomainAudio.playExpansion(message.caster(),message.kind(),message.center(),message.radius()))).add();
    }

    public static void announceExpansion(ServerPlayer caster,DomainKind kind,Vec3 center,float radius){
        announceExpansion(caster.serverLevel(),caster.getUUID(),kind,center,radius);
    }
    public static void announceExpansion(net.minecraft.server.level.ServerLevel level,UUID owner,DomainKind kind,Vec3 center,float radius){
        Expansion message=new Expansion(owner,kind,center,radius);
        for(ServerPlayer listener:level.getServer().getPlayerList().getPlayers())
            if(listener.serverLevel()==level && (listener.getUUID().equals(owner) || listener.position().distanceToSqr(center)<(radius+32)*(radius+32)))
                CHANNEL.send(PacketDistributor.PLAYER.with(()->listener),message);
    }

    public static void requestToggle() { CHANNEL.sendToServer(new Toggle()); }
    public static void requestCycle() { CHANNEL.sendToServer(new Cycle()); }
    public static void requestReset() { CHANNEL.sendToServer(new Reset()); }

    public static void sync(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new Snapshot(DomainService.snapshot(player), DomainService.isCaptive(player.getUUID()), DomainService.selection(player), DomainService.cooldown(player)));
    }

    private static void encode(Snapshot message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.captive());
        buffer.writeEnum(message.selected());
        buffer.writeVarInt(message.cooldown());
        buffer.writeVarInt(message.states().size());
        for (DomainState state : message.states()) {
            buffer.writeUUID(state.owner());
            vector(buffer, state.center());
            vector(buffer, state.axis());
            buffer.writeFloat(state.radius());
            buffer.writeVarInt(state.age());
            buffer.writeVarInt(state.remaining());
            buffer.writeEnum(state.kind());
            buffer.writeFloat(state.maxRadius());
            buffer.writeFloat(state.integrity());
            buffer.writeInt(state.closing());
        }
    }

    private static Snapshot decode(FriendlyByteBuf buffer) {
        boolean captive = buffer.readBoolean();
        DomainKind selected = buffer.readEnum(DomainKind.class);
        int cooldown=buffer.readVarInt();
        if(cooldown<0 || cooldown>100000) throw new IllegalArgumentException("Invalid cooldown");
        int count = buffer.readVarInt();
        if (count < 0 || count > 8) throw new IllegalArgumentException("Invalid domain snapshot count");
        List<DomainState> states = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID owner = buffer.readUUID();
            Vec3 center = vector(buffer), axis = vector(buffer);
            float radius = buffer.readFloat();
            int age = buffer.readVarInt(), remaining = buffer.readVarInt();
            DomainKind kind = buffer.readEnum(DomainKind.class);
            float maxRadius=buffer.readFloat(),integrity=buffer.readFloat();
            int closing=buffer.readInt();
            if (!Double.isFinite(center.lengthSqr()) || !Double.isFinite(axis.lengthSqr())
                    || !Float.isFinite(radius) || radius < 0 || radius > 200 || age < 0 || remaining < 0
                    || !Float.isFinite(maxRadius) || maxRadius<1 || maxRadius>200
                    || !Float.isFinite(integrity) || integrity<0 || integrity>1 || closing < -1 || closing > 40)
                throw new IllegalArgumentException("Invalid domain snapshot");
            states.add(new DomainState(owner, center, axis, radius, age, remaining, kind, maxRadius, integrity, closing));
        }
        return new Snapshot(List.copyOf(states), captive, selected, cooldown);
    }

    private static void vector(FriendlyByteBuf buffer, Vec3 vector) {
        buffer.writeDouble(vector.x); buffer.writeDouble(vector.y); buffer.writeDouble(vector.z);
    }
    private static Vec3 vector(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
