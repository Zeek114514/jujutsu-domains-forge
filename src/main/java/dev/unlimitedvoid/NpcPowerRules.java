package dev.unlimitedvoid;
/** Current-form balance; transformations and rare techniques can still reverse a particular fight. */
public final class NpcPowerRules {
    public enum Form { STUDENT,AWAKENED,YUJI,SHINJUKU,MEGUMI,TRUE }
    public static float attack(Form form){return switch(form){case STUDENT->.85f;case AWAKENED->1f;case YUJI->1.25f;case SHINJUKU->1.35f;case MEGUMI->1.5f;case TRUE->1.6f;};}
    public static float taken(Form form){return switch(form){case STUDENT->1.1f;case AWAKENED->1;case YUJI,SHINJUKU,MEGUMI->.9f;case TRUE->.80f;};}
    public static double speed(Form form){return switch(form){case STUDENT,AWAKENED,YUJI->.34;case SHINJUKU,TRUE->.36;case MEGUMI->.35;};}
    public static float domainHit(Form form){return switch(form){case STUDENT->32;case AWAKENED,YUJI->28;case SHINJUKU,MEGUMI->12;case TRUE->10;};}
    private NpcPowerRules(){}
}
