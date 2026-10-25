package dev.unlimitedvoid;

public enum DomainKind {
    VOID(16, "domain.unlimitedvoid.void"),
    SHRINE(200, "domain.unlimitedvoid.shrine");

    public final float radius;
    public final String translation;
    DomainKind(float radius, String translation) { this.radius = radius; this.translation = translation; }
}
