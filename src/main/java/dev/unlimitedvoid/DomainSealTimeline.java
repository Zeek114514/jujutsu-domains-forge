package dev.unlimitedvoid;

public final class DomainSealTimeline {
    private DomainSealTimeline(){}
    public static float weight(DomainKind kind,float age,int closing){
        if(age<0||closing>=0)return 0;
        return DomainTimeline.smooth(age/10)*(1-DomainTimeline.smooth((age-DomainTimeline.chant(kind))/12));
    }
}
