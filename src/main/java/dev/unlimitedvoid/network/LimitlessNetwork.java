package dev.unlimitedvoid.network;

import dev.unlimitedvoid.*;
import dev.unlimitedvoid.server.LimitlessService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import java.util.*;

public final class LimitlessNetwork {
    private static final String VERSION="8";
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(UnlimitedVoid.ID,"limitless"),()->VERSION,VERSION::equals,VERSION::equals);
    private record Request(LimitlessKind kind){}
    private record Release(){}
    private record Snapshot(List<LimitlessService.State> states,int blue,int red,int purple){}
    public static void register(){
        CHANNEL.messageBuilder(Request.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->b.writeEnum(m.kind())).decoder(b->new Request(b.readEnum(LimitlessKind.class))).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)LimitlessService.request(p,m.kind());}).add();
        CHANNEL.messageBuilder(Release.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{}).decoder(b->new Release()).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)LimitlessService.releaseBlue(p);}).add();
        CHANNEL.messageBuilder(Snapshot.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder(LimitlessNetwork::encode).decoder(LimitlessNetwork::decode).consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->dev.unlimitedvoid.client.LimitlessClient.receive(m.states(),m.blue(),m.red(),m.purple()))).add();
    }
    public static void request(LimitlessKind kind){CHANNEL.sendToServer(new Request(kind));}
    public static void release(){CHANNEL.sendToServer(new Release());}
    public static void sync(ServerPlayer p,List<LimitlessService.State> states,int blue,int red,int purple){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Snapshot(states,blue,red,purple));}
    private static void vector(FriendlyByteBuf b,Vec3 p){b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);}
    private static Vec3 vector(FriendlyByteBuf b){var p=new Vec3(b.readDouble(),b.readDouble(),b.readDouble());if(!Double.isFinite(p.lengthSqr())||Math.abs(p.x)>30000001||Math.abs(p.z)>30000001||Math.abs(p.y)>100000)throw new IllegalArgumentException("Invalid technique position");return p;}
    private static void encode(Snapshot m,FriendlyByteBuf b){b.writeVarInt(m.blue);b.writeVarInt(m.red);b.writeVarInt(m.purple);b.writeVarInt(m.states.size());for(var s:m.states){b.writeUUID(s.id());b.writeUUID(s.owner());b.writeEnum(s.kind());vector(b,s.position());vector(b,s.direction());b.writeVarInt(s.phase());b.writeVarInt(s.charge());b.writeLong(s.start());b.writeLong(s.phaseStart());b.writeFloat(s.blastRadius());}}
    private static Snapshot decode(FriendlyByteBuf b){
        int blue=b.readVarInt(),red=b.readVarInt(),purple=b.readVarInt(),count=b.readVarInt();if(blue<0||blue>100||red<0||red>120||purple<0||purple>300||count<0||count>8)throw new IllegalArgumentException("Invalid technique snapshot");
        var states=new ArrayList<LimitlessService.State>();for(int i=0;i<count;i++){
            UUID id=b.readUUID(),owner=b.readUUID();var kind=b.readEnum(LimitlessKind.class);var position=vector(b);var direction=vector(b);int phase=b.readVarInt(),charge=b.readVarInt();long start=b.readLong(),phaseStart=b.readLong();float radius=b.readFloat();
            if(phase<0||phase>PurpleDetonation.BLAST||charge<16||charge>70||start<0||phaseStart<start||direction.lengthSqr()<.9||direction.lengthSqr()>1.1||!Float.isFinite(radius)||radius<0||radius>128||
                    phase>=PurpleDetonation.GROWING&&(kind!=LimitlessKind.PURPLE||radius<8)||phase==PurpleDetonation.AIR&&kind==LimitlessKind.PURPLE||phase<PurpleDetonation.GROWING&&radius!=0)throw new IllegalArgumentException("Invalid technique state");
            states.add(new LimitlessService.State(id,owner,kind,position,direction,phase,charge,start,phaseStart,radius));
        }return new Snapshot(List.copyOf(states),blue,red,purple);
    }
}
