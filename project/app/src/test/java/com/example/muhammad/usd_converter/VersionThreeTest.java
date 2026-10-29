package com.example.muhammad.usd_converter;

import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;

public class VersionThreeTest {
    private final long now = 1793232000000L; // 2026-10-29 UTC
    @Test public void bitcoinKeepsFractionalCoins() {
        assertEquals(new BigDecimal("0.00124250"), Conversion.convert(new BigDecimal("100"), new BigDecimal("0.000012425"), "BTC"));
    }
    @Test public void goldIsAWeightNotACurrency() {
        assertEquals(new BigDecimal("0.02481400"), Conversion.convert(new BigDecimal("100"), new BigDecimal("0.00024814"), "XAU"));
    }
    @Test public void xrpUsesSixDecimals() { assertEquals(6, Conversion.digits("XRP")); }
    @Test public void newerCurrencyDoesNotCrashOlderRuntime() { assertEquals(2, Conversion.digits("ZWG")); }
    @Test public void dinarKeepsThreeDecimals() { assertEquals(3, Conversion.digits("KWD")); }
    @Test public void freshRateAndWeekendRateAccepted() {
        assertEquals(0, RatePolicy.ageDays("2026-10-29", now));
        assertEquals(3, RatePolicy.ageDays("2026-10-26", now));
        assertEquals(7, RatePolicy.ageDays("2026-10-22", now));
    }
    @Test(expected=IllegalArgumentException.class) public void staleRateRejected() { RatePolicy.ageDays("2026-10-21", now); }
    @Test(expected=IllegalArgumentException.class) public void futureRateRejected() { RatePolicy.ageDays("2026-10-30", now); }
    @Test(expected=IllegalArgumentException.class) public void impossibleDateRejected() { RatePolicy.ageDays("2026-02-30", now); }
    @Test public void cacheExpiresAndRejectsClockRollback() {
        assertTrue(RatePolicy.reusable(now - RatePolicy.HOUR, now));
        assertFalse(RatePolicy.reusable(now - RatePolicy.CACHE_TTL, now));
        assertFalse(RatePolicy.reusable(now + 1, now));
    }
    @Test(expected=IllegalArgumentException.class) public void exponentNotationRejected() { Conversion.amount("1e12"); }
    @Test(expected=IllegalArgumentException.class) public void excessiveCentsRejected() { Conversion.amount("1.001"); }
    @Test public void trillionAllowed() { assertEquals(new BigDecimal("1000000000000"), Conversion.amount("1000000000000")); }
}
