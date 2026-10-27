package dev.unlimitedvoid;

/** Hard total geometry limit plus density scaling for simultaneous and distant blasts. */
public final class FugaRenderBudget {
    public static final int MAX_QUADS=6000;
    private FugaRenderBudget(){}
    public static int stride(int casts,double distanceSquared,int particles){
        int concurrency=casts>=6?4:casts>=3?2:1;
        int distance=distanceSquared>400*400?4:distanceSquared>220*220?2:1;
        return Math.min(16,concurrency*Math.max(distance,particles>=2?4:particles==1?2:1));
    }
}
