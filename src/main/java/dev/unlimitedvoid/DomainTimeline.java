package dev.unlimitedvoid;

/** Tick-based choreography shared by gameplay, entity motion and client rendering. */
public final class DomainTimeline {
    public static final int FADE = 40;
    public static final int ACTIVE = 400;
    public static final int RISE = 78;
    public static int chant(DomainKind kind) { return kind==DomainKind.VOID?147:156; }
    public static int formationStart(DomainKind kind) { return kind==DomainKind.SHRINE?78:chant(kind); }
    public static int formed(DomainKind kind) { return kind==DomainKind.SHRINE?chant(kind):chant(kind)+100; }
    public static int casterRelease(DomainKind kind) { return formed(kind)+(kind==DomainKind.SHRINE?10:0); }
    public static float smooth(float x) { x=Math.max(0,Math.min(1,x));return x*x*(3-2*x); }
    public static float growth(DomainKind kind,float age) { return smooth((age-formationStart(kind))/(kind==DomainKind.VOID?45f:RISE)); }
    public static float rise(float age) { return smooth((age-formationStart(DomainKind.SHRINE))/RISE); }
    public static float sky(DomainKind kind,float age) { return kind==DomainKind.SHRINE?smooth((age-108)/48f):smooth((age-chant(kind))/45f); }
    public static float fade(int closing) { return closing<0?1:1-smooth(closing/(float)FADE); }
    private DomainTimeline() {}
}
