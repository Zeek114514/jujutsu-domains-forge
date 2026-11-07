package dev.unlimitedvoid.network;
import dev.unlimitedvoid.*;
import dev.unlimitedvoid.server.ProgressionService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
public final class ProgressionNetwork {
    private static final String VERSION="1";
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(UnlimitedVoid.ID,"progression"),()->VERSION,VERSION::equals,VERSION::equals);
    public record State(boolean enabled,boolean creative,int path,int xp,float energy,int burnout,boolean open){}
    public static void register(){
        CHANNEL.messageBuilder(Boolean.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->b.writeBoolean(m)).decoder(b->b.readBoolean()).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)ProgressionService.request(p,m);}).add();
        CHANNEL.messageBuilder(State.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeBoolean(m.enabled);b.writeBoolean(m.creative);b.writeVarInt(m.path);b.writeVarInt(m.xp);b.writeFloat(m.energy);b.writeVarInt(m.burnout);b.writeBoolean(m.open);})
            .decoder(b->{boolean enabled=b.readBoolean(),creative=b.readBoolean();int path=b.readVarInt(),xp=b.readVarInt();float energy=b.readFloat();int burnout=b.readVarInt();boolean open=b.readBoolean();if(path<0||path>2||xp<0||xp>100000||!Float.isFinite(energy)||energy<0||energy>195||burnout<0||burnout>800)throw new IllegalArgumentException("Invalid progression state");return new State(enabled,creative,path,xp,energy,burnout,open);})
            .consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->dev.unlimitedvoid.client.ProgressionClient.receive(m))).add();
    }
    public static void request(boolean open){CHANNEL.sendToServer(open);}
    public static void send(ServerPlayer p,State state){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),state);}
}
