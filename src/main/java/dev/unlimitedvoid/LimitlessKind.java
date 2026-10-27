package dev.unlimitedvoid;

/** Server-owned balance; health points, blocks and ticks. */
public enum LimitlessKind {
    BLUE(16,100,40,2,8,4), RED(20,120,64,3,6,72), PURPLE(70,300,96,2.8,4.5,160);
    public final int charge,cooldown;
    public final double range,speed,radius,damage;
    LimitlessKind(int charge,int cooldown,double range,double speed,double radius,double damage){this.charge=charge;this.cooldown=cooldown;this.range=range;this.speed=speed;this.radius=radius;this.damage=damage;}
    public double radius(int held){return this==BLUE?8+4*power(held):radius;}
    public static float smooth(float v){v=Math.max(0,Math.min(1,v));return v*v*(3-2*v);}
    public static double power(int held){return Math.max(0,Math.min(1,(held-16)/24.));}
}
