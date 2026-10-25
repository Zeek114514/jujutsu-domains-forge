package dev.unlimitedvoid.server;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Pure geometry regression checks; no game client, world, server or window. */
public final class ShrineGroundCheck15 {
    static int checks;
    static final AABB FULL=new AABB(0,0,0,1,1,1);
    static void expect(String label,Vec3 feet,Map<Integer,List<AABB>> blocks,double expected){
        var result=ShrinePlacement.groundBelow(feet,-64,320,y->blocks.getOrDefault(y,List.of()));
        if(result.isEmpty() || Math.abs(result.getAsDouble()-expected)>1e-8)throw new AssertionError(label+": "+result+" != "+expected);
        checks++;
    }
    public static void main(String[] args){
        expect("standing",new Vec3(.5,64,.5),Map.of(63,List.of(FULL)),64);
        expect("airborne",new Vec3(.5,140,.5),Map.of(63,List.of(FULL)),64);
        expect("roof below caster",new Vec3(.5,140,.5),Map.of(63,List.of(FULL),79,List.of(FULL)),80);
        expect("ignore ceiling above caster",new Vec3(.5,70,.5),Map.of(63,List.of(FULL),90,List.of(FULL)),64);
        expect("lower slab",new Vec3(.5,100,.5),Map.of(63,List.of(new AABB(0,0,0,1,.5,1))),63.5);
        expect("upper slab",new Vec3(.5,100,.5),Map.of(63,List.of(new AABB(0,.5,0,1,1,1))),64);
        var stair=Map.of(63,List.of(new AABB(0,0,0,1,.5,1),new AABB(.5,.5,0,1,1,1)));
        expect("stair low tread",new Vec3(.25,100,.5),stair,63.5);
        expect("stair high tread",new Vec3(.75,100,.5),stair,64);
        expect("negative coordinates",new Vec3(-.25,100,-.5),stair,64);
        expect("ignore nearby non-supporting shape",new Vec3(.8,100,.8),Map.of(63,List.of(FULL),80,List.of(new AABB(0,0,0,.2,1,.2))),64);
        expect("above build height",new Vec3(.5,500,.5),Map.of(319,List.of(FULL)),320);
        expect("world bottom",new Vec3(.5,0,.5),Map.of(-64,List.of(FULL)),-63);
        if(ShrinePlacement.groundBelow(new Vec3(.5,100,.5),-64,320,y->List.of()).isPresent())throw new AssertionError("void must reject");checks++;
        AtomicInteger reads=new AtomicInteger();
        if(ShrinePlacement.groundBelow(new Vec3(.5,-80,.5),-64,320,y->{reads.incrementAndGet();return List.of(FULL);}).isPresent() || reads.get()!=0)throw new AssertionError("below world bounds");checks++;
        ShrinePlacement.groundBelow(new Vec3(.5,500,.5),-64,320,y->{if(y< -64 || y>=320)throw new AssertionError("out of bounds");reads.incrementAndGet();return List.of();});
        if(reads.get()!=384)throw new AssertionError("bounded column read budget");checks++;
        System.out.println("SHRINE_GROUND_OK checks="+checks+" maxColumnReads="+reads.get()+" gameLaunched=false");
    }
}
