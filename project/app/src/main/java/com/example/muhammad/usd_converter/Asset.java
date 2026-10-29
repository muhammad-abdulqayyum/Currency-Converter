package com.example.muhammad.usd_converter;

/** Display metadata; national currencies and special asset units stay separate. */
public final class Asset {
    public enum Kind { WORLD, METAL, CRYPTO }
    public final String code, name, icon;
    public final Kind kind;
    public Asset(String code, String name, String icon, Kind kind) {
        this.code = code; this.name = name; this.icon = icon; this.kind = kind;
    }
    public boolean isSubunit() { return code.equals("SAT") || code.equals("DROP"); }
    public String rateCode() { return code.equals("SAT") ? "BTC" : code.equals("DROP") ? "XRP" : code; }
    public java.math.BigDecimal unitsPerCoin() { return new java.math.BigDecimal(code.equals("SAT") ? "100000000" : code.equals("DROP") ? "1000000" : "1"); }
    public String unit() { return kind == Kind.METAL ? "troy oz" : code; }
    @Override public String toString() {
        return icon + "  " + code + " — " + name + (kind == Kind.METAL ? " · troy oz" : "");
    }
}
