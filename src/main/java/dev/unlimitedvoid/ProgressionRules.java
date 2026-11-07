package dev.unlimitedvoid;
/** Small, bounded progression policy shared by UI, persistence validation and the server gate. */
public final class ProgressionRules {
    private ProgressionRules(){}
    public static int level(int xp){return 1+Math.max(0,Math.min(760,xp))/40;}
    public static int maximum(int xp){return 100+(level(xp)-1)*5;}
    public static int cost(int ability){return switch(ability){case 0->12;case 1->24;case 2->60;case 3->45;case 4,5->75;default->Integer.MAX_VALUE;};}
    public static int required(int ability){return switch(ability){case 0,3->1;case 1->4;case 2,5->8;case 4->12;default->100;};}
    public static boolean unlocked(int path,int xp,int ability){return ability>=0&&ability<=5&&path==(ability<3||ability==4?1:2)&&level(xp)>=required(ability);}
}
