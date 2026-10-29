package com.example.muhammad.usd_converter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public final class Conversion {
    private Conversion() {}
    public static BigDecimal amount(String value) {
        String text = value.trim();
        if (!text.matches("[0-9]+(\\.[0-9]{1,2})?"))
            throw new IllegalArgumentException("Enter dollars and up to two cents digits.");
        BigDecimal amount = new BigDecimal(text);
        if (amount.compareTo(new BigDecimal("1000000000000")) > 0)
            throw new IllegalArgumentException("Amount is too large.");
        return amount;
    }
    public static BigDecimal assetAmount(String value) {
        String text=value.trim();
        if(text.length()>22 || !text.matches("[0-9]+(\\.[0-9]{1,8})?"))throw new IllegalArgumentException("Invalid amount");
        BigDecimal amount=new BigDecimal(text);
        if(amount.compareTo(new BigDecimal("1000000000000"))>0)throw new IllegalArgumentException("Amount is too large");
        return amount;
    }
    /** Both quotes are asset units per USD; round only the final result. */
    public static BigDecimal cross(BigDecimal amount,BigDecimal source,BigDecimal target,String code) {
        if(amount.signum()<0)throw new IllegalArgumentException("Invalid amount");
        validate(source);validate(target);
        return amount.multiply(target).divide(source,digits(code),RoundingMode.HALF_UP);
    }
    private static void validate(BigDecimal rate) {
        if(rate.signum()<=0 || rate.precision()>30 || Math.abs(rate.scale())>30)throw new IllegalArgumentException("Invalid rate");
    }
    public static int digits(String code) {
        if(code.equals("SAT") || code.equals("DROP")) return 0;
        if (code.equals("BTC") || code.equals("ETH") || code.equals("BCH") || code.equals("LTC")) return 8;
        if (code.equals("XRP")) return 6;
        if (code.equals("XAU") || code.equals("XAG") || code.equals("XPT") || code.equals("XPD")) return 8;
        try { return Math.max(0, Currency.getInstance(code).getDefaultFractionDigits()); }
        catch (IllegalArgumentException e) { return 2; } // New ISO codes on older Android releases.
    }
    public static BigDecimal convert(BigDecimal amount, BigDecimal rate, String code) {
        if (amount.signum() < 0 || rate.signum() <= 0 || rate.precision() > 30 || Math.abs(rate.scale()) > 30)
            throw new IllegalArgumentException("Invalid amount or exchange rate.");
        return amount.multiply(rate).setScale(digits(code), RoundingMode.HALF_UP);
    }
}
