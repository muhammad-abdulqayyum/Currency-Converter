package com.example.muhammad.usd_converter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
public final class Conversion {
    private Conversion() {}
    public static BigDecimal amount(String value) {
        BigDecimal amount = new BigDecimal(value.trim());
        if (amount.signum() < 0 || amount.compareTo(new BigDecimal("1000000000000")) > 0)
            throw new IllegalArgumentException("Enter an amount from 0 to 1 trillion USD.");
        return amount;
    }
    public static BigDecimal convert(BigDecimal amount, BigDecimal rate, String currency) {
        if (rate.signum() <= 0) throw new IllegalArgumentException("Invalid exchange rate");
        return amount.multiply(rate).setScale(Currency.getInstance(currency).getDefaultFractionDigits(), RoundingMode.HALF_UP);
    }
}
