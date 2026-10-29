package com.example.muhammad.usd_converter;

/** Display metadata; national currencies and special asset units stay separate. */
public final class Asset {
    public enum Kind { WORLD, METAL, CRYPTO }
    public final String code, name, icon;
    public final Kind kind;
    public Asset(String code, String name, String icon, Kind kind) {
        this.code = code; this.name = name; this.icon = icon; this.kind = kind;
    }
    public String unit() { return kind == Kind.METAL ? "troy oz" : code; }
    @Override public String toString() {
        return icon + "  " + code + " — " + name + (kind == Kind.METAL ? " · troy oz" : "");
    }
}
