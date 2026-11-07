package dev.unlimitedvoid.network;

import dev.unlimitedvoid.UnlimitedVoid;
import dev.unlimitedvoid.server.NpcControlService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;

public final class NpcControlNetwork {
    private static final String VERSION="1";
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(UnlimitedVoid.ID,"npc_control"),()->VERSION,VERSION::equals,VERSION::equals);
    public record Request(int entity,int operation){}
    public record State(int entity,String name,int mode,int team,boolean arena,boolean open,String status){}
    public static void register(){
        CHANNEL.messageBuilder(Request.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeVarInt(m.entity);b.writeVarInt(m.operation);})
            .decoder(b->{int id=b.readVarInt(),op=b.readVarInt();if(id<0||op<0||op>10)throw new IllegalArgumentException("Invalid NPC request");return new Request(id,op);})
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)NpcControlService.request(p,m.entity,m.operation);}).add();
        CHANNEL.messageBuilder(State.class,1,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{b.writeVarInt(m.entity);b.writeUtf(m.name,128);b.writeVarInt(m.mode);b.writeVarInt(m.team);b.writeBoolean(m.arena);b.writeBoolean(m.open);b.writeUtf(m.status,512);})
            .decoder(b->new State(b.readVarInt(),b.readUtf(128),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readUtf(512)))
            .consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->dev.unlimitedvoid.client.NpcManagementScreen.receive(m))).add();
    }
    public static void request(int entity,int op){CHANNEL.sendToServer(new Request(entity,op));}
    public static void send(ServerPlayer p,State state){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),state);}
}
