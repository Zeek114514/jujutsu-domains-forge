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
    public static final ForgeConfigSpec.BooleanValue FUGA_COMBAT;
    public static final ForgeConfigSpec.DoubleValue FUGA_RADIUS,FUGA_DAMAGE,FUGA_DUST_DAMAGE;
    public static final ForgeConfigSpec.IntValue FUGA_BALANCE_REVISION;
    public static final ForgeConfigSpec.BooleanValue LIMITLESS_TERRAIN;
    public static final ForgeConfigSpec.DoubleValue PURPLE_BLAST_RADIUS,PURPLE_BLAST_DAMAGE,PURPLE_BACKLASH;
    static {
        ForgeConfigSpec.Builder b=new ForgeConfigSpec.Builder();
        SHRINE_RADIUS=b.comment("Open domain radius in blocks; 1 block is treated as 1 metre. Loaded targets only.").defineInRange("shrineRadius",200,16,200);
        CASTER_ENERGY=b.comment("Default caster energy; entity persistent data uv_cursed_energy overrides it.").defineInRange("casterEnergy",100.0,1,1000000);
        LIVING_ENERGY=b.comment("Default living target energy; armor stands and nonliving entities default to zero.").defineInRange("livingEntityEnergy",100.0,0,1000000);
        TERRAIN_DAMAGE=b.comment("Real permanent block destruction by Dismantle. Off by default; no chunk generation.").define("shrineTerrainDamage",false);
        BLOCK_BUDGET=b.comment("Maximum blocks destroyed per tick per shrine; block checks are also bounded.").defineInRange("blocksDestroyedPerTick",96,1,512);
        SOUND_VOLUME=b.comment("Master gain for this mod's custom sounds, also controlled by vanilla sound category sliders.").defineInRange("domainSoundVolume",0.8,0.0,1.0);
        DOMAIN_AMBIENCE=b.comment("Enable the quiet local ambience loop while inside a domain.").define("domainAmbience",true);
        FUGA_COMBAT=b.comment("Enable server-authoritative Fuga entity damage. Does not enable block destruction.").define("fugaCombatEnabled",true);
        FUGA_RADIUS=b.comment("Fuga damage radius matches the main firestorm footprint by default.").defineInRange("fugaDamageRadius",200.0,1,200);
        FUGA_DAMAGE=b.defineInRange("fugaBaseDamage",240.0,0,10000);
        FUGA_DUST_DAMAGE=b.comment("Maximum damage after consuming a fully charged own Shrine dust field.").defineInRange("fugaDustDamage",480.0,0,10000);
        FUGA_BALANCE_REVISION=b.comment("One-time migration of unmodified 1.1.0 Fuga defaults; custom values are preserved.").defineInRange("fugaBalanceRevision",0,0,1);
        LIMITLESS_TERRAIN=b.comment("Allow Purple to break blocks, with Forge break events and per-tick limits. Independent of Shrine; off by default.").define("limitlessTerrainDamage",false);
        PURPLE_BLAST_RADIUS=b.comment("Airborne Blue/Red detonation damage and visible shockwave radius. Loaded targets only.").defineInRange("purpleDetonationRadius",64.0,8,128);
        PURPLE_BLAST_DAMAGE=b.comment("Maximum center damage of airborne Blue/Red detonation; edge receives 25 percent.").defineInRange("purpleDetonationDamage",480.0,0,10000);
        PURPLE_BACKLASH=b.comment("Caster damage multiplier for airborne detonation. 0 protects the caster; creative/spectator remain protected.").defineInRange("purpleDetonationBacklash",.05,0,1);
        SPEC=b.build();
    }
}
