package com.example.muhammad.usd_converter;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Daily reference data: allow weekends, but never silently use very old values. */
public final class RatePolicy {
    public static final long HOUR = 3600000L, DAY = 24 * HOUR;
    public static final long CACHE_TTL = 6 * HOUR;
    private RatePolicy() {}
    public static long ageDays(String date, long now) {
        if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException("Invalid rate date");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setLenient(false); format.setTimeZone(TimeZone.getTimeZone("UTC"));
        ParsePosition pos = new ParsePosition(0);
        Date parsed = format.parse(date, pos);
        if (parsed == null || pos.getIndex() != date.length()) throw new IllegalArgumentException("Invalid rate date");
        long age = now / DAY - parsed.getTime() / DAY;
        if (age < 0 || age > 7) throw new IllegalArgumentException("Rate is outside the seven-day freshness window");
        return age;
    }
    public static boolean reusable(long fetchedAt, long now) {
        return fetchedAt > 0 && now >= fetchedAt && now - fetchedAt < CACHE_TTL;
    }
}
