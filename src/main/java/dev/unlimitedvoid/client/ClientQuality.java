package dev.unlimitedvoid.client;
import net.minecraftforge.common.ForgeConfigSpec;
public final class ClientQuality {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.IntValue QUALITY;
    static{var b=new ForgeConfigSpec.Builder();QUALITY=b.comment("0 full, 1 balanced, 2 economy. Cosmetic only: fire particle budgets and distant particles.").defineInRange("effectQuality",0,0,2);SPEC=b.build();}
    public static int quality(){return QUALITY.get();}
    public static int particlesPerTick(){return quality()==0?300:quality()==1?150:60;}
    public static int aliveParticles(){return quality()==0?4500:quality()==1?2250:900;}
    public static int distance(){return quality()==0?768:quality()==1?256:128;}
    public static String label(){return new String[]{"完整","均衡","省电"}[quality()];}
    public static void cycle(){QUALITY.set((quality()+1)%3);SPEC.save();}
}
