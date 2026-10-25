package dev.unlimitedvoid;

import net.minecraftforge.common.ForgeConfigSpec;

public final class DomainConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue SHRINE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue CASTER_ENERGY, LIVING_ENERGY;
    public static final ForgeConfigSpec.BooleanValue TERRAIN_DAMAGE;
    public static final ForgeConfigSpec.IntValue BLOCK_BUDGET;
    public static final ForgeConfigSpec.DoubleValue SOUND_VOLUME;
    public static final ForgeConfigSpec.BooleanValue DOMAIN_AMBIENCE;
    static {
        ForgeConfigSpec.Builder b=new ForgeConfigSpec.Builder();
        SHRINE_RADIUS=b.comment("Open domain radius in blocks; 1 block is treated as 1 metre. Loaded targets only.").defineInRange("shrineRadius",200,16,200);
        CASTER_ENERGY=b.comment("Default caster energy; entity persistent data uv_cursed_energy overrides it.").defineInRange("casterEnergy",100.0,1,1000000);
        LIVING_ENERGY=b.comment("Default living target energy; armor stands and nonliving entities default to zero.").defineInRange("livingEntityEnergy",100.0,0,1000000);
        TERRAIN_DAMAGE=b.comment("Real permanent block destruction by Dismantle. Off by default; no chunk generation.").define("shrineTerrainDamage",false);
        BLOCK_BUDGET=b.comment("Maximum blocks destroyed per tick per shrine; block checks are also bounded.").defineInRange("blocksDestroyedPerTick",96,1,512);
        SOUND_VOLUME=b.comment("Master gain for this mod's custom sounds, also controlled by vanilla sound category sliders.").defineInRange("domainSoundVolume",0.8,0.0,1.0);
        DOMAIN_AMBIENCE=b.comment("Enable the quiet local ambience loop while inside a domain.").define("domainAmbience",true);
        SPEC=b.build();
    }
}
