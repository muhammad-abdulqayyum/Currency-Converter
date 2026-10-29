package com.example.muhammad.usd_converter;
import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;
public class ExampleUnitTest {
    @Test public void decimalAmountAndPoundRounding() { assertEquals(new BigDecimal("7.51"), Conversion.convert(Conversion.amount("10"), new BigDecimal("0.75103"), "GBP")); }
    @Test public void yenHasNoFractionalDigits() { assertEquals(new BigDecimal("1579"), Conversion.convert(Conversion.amount("10"), new BigDecimal("157.88"), "JPY")); }
    @Test public void zeroAllowed() { assertEquals(new BigDecimal("0.00"), Conversion.convert(Conversion.amount("0"), BigDecimal.ONE, "EUR")); }
    @Test(expected=IllegalArgumentException.class) public void negativeRejected() { Conversion.amount("-1"); }
    @Test(expected=IllegalArgumentException.class) public void emptyRejected() { Conversion.amount(""); }
    @Test(expected=IllegalArgumentException.class) public void oversizedRejected() { Conversion.amount("1000000000001"); }
    @Test(expected=IllegalArgumentException.class) public void nonNumericRejected() { Conversion.amount("NaN"); }
    @Test(expected=IllegalArgumentException.class) public void invalidRateRejected() { Conversion.convert(BigDecimal.ONE, BigDecimal.ZERO, "GBP"); }
}
