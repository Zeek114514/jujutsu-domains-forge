package dev.unlimitedvoid.client;

import dev.unlimitedvoid.*;
import java.util.UUID;

public final class DomainSeals {
    public record Seal(DomainKind kind,float weight){}
    private static boolean firstPerson;
    public static boolean renderingHands(){return firstPerson;}
    static void renderingHands(boolean value){firstPerson=value;}
    public static Seal of(UUID caster,float partial){
        for(var domain:VoidClient.renderStates())if(domain.owner().equals(caster)){
            float weight=DomainSealTimeline.weight(domain.kind(),domain.age()+partial,domain.closing());
            if(weight>0)return new Seal(domain.kind(),weight);
        }
        return null;
    }
}
