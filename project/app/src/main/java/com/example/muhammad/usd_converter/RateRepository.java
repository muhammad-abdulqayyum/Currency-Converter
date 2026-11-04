package com.example.muhammad.usd_converter;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import javax.net.ssl.HttpsURLConnection;

/** Only rate snapshots are persisted. Amounts and conversion history are not stored. */
public final class RateRepository {
    public static final class Quote {
        public final BigDecimal rate;
        public final String date, source;
        public final boolean saved, offline;
        public final long age;
        Quote(BigDecimal rate, String date, String source, boolean saved, boolean offline, long age) {
            this.rate = rate; this.date = date; this.source = source;
            this.saved = saved; this.offline = offline; this.age = age;
        }
    }
    private final SharedPreferences cache;
    public RateRepository(SharedPreferences cache) { this.cache = cache; }
    public Quote get(Asset asset) throws Exception {
        if(asset.isSubunit()) {
            Quote parent=get(new Asset(asset.rateCode(),asset.rateCode(),asset.icon,Asset.Kind.CRYPTO));
            return new Quote(parent.rate.multiply(asset.unitsPerCoin()),parent.date,parent.source+"\nDerived from "+asset.rateCode()+" denomination",parent.saved,parent.offline,parent.age);
        }
        if(asset.code.equals("USD"))return new Quote(BigDecimal.ONE,"base","USD reference base",false,false,0);
        long now = System.currentTimeMillis();
        String key = "v3." + asset.code;
        String stored = cache.getString(key, null);
        long fetchedAt = cache.getLong(key + ".time", 0);
        if (stored != null && RatePolicy.reusable(fetchedAt, now)) {
            try { return parse(new JSONObject(stored), asset, true, false, now); }
            catch (Exception ignored) { /* A refresh may establish a better reference date. */ }
        }
        try {
            Response latest = download(asset, "");
            long referenceTime = latest.serverTime > 0 ? latest.serverTime : now;
            JSONObject body = new JSONObject(latest.body);
            Quote quote;
            try {
                quote = parse(body, asset, false, false, referenceTime);
            } catch (Exception invalidLatest) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                String range = "&from=" + format.format(new Date(referenceTime - 7 * RatePolicy.DAY))
                        + "&to=" + format.format(new Date(referenceTime));
                JSONArray history = new JSONArray(download(asset, range).body);
                quote = null;
                for (int i = 0; i < history.length(); i++) {
                    JSONObject candidate = history.optJSONObject(i);
                    if (candidate == null) continue;
                    try {
                        Quote historical = parse(candidate, asset, false, false, referenceTime);
                        if (quote == null || historical.date.compareTo(quote.date) > 0) {
                            quote = historical;
                            body = candidate;
                        }
                    } catch (Exception ignored) { /* Skip invalid history entries. */ }
                }
                if (quote == null) throw invalidLatest;
            }
            if (stored != null) {
                Quote saved = null;
                try { saved = parse(new JSONObject(stored), asset, true, false, referenceTime); }
                catch (Exception ignored) { /* Ignore invalid saved data. */ }
                if (saved != null && saved.date.compareTo(quote.date) > 0) return saved;
            }
            cache.edit().putString(key, compact(body)).putLong(key + ".time", referenceTime).apply();
            return quote;
        } catch (Exception networkError) {
            if (stored != null) {
                try { return parse(new JSONObject(stored), asset, true, true, Math.max(now, fetchedAt)); }
                catch (Exception ignored) { /* Invalid cache is never displayed as a price. */ }
            }
            throw new IOException("No verified reference rate available", networkError);
        }
    }
    private String compact(JSONObject body) throws Exception {
        JSONObject small = new JSONObject();
        for (String field : new String[]{"base", "quote", "rate", "date"}) small.put(field, body.get(field));
        JSONArray providers = body.optJSONArray("providers"), keys = new JSONArray();
        if (providers != null) for (int i = 0; i < providers.length(); i++) {
            JSONObject p = providers.getJSONObject(i);
            if (!p.optBoolean("excluded", false)) keys.put(new JSONObject().put("key", p.optString("key")));
        }
        small.put("providers", keys);
        return small.toString();
    }
    private Quote parse(JSONObject body, Asset asset, boolean saved, boolean offline, long now) throws Exception {
        if (!"USD".equals(body.getString("base")) || !asset.code.equals(body.getString("quote")))
            throw new IOException("Unexpected currency pair");
        BigDecimal rate = new BigDecimal(body.getString("rate"));
        Conversion.convert(BigDecimal.ONE, rate, asset.code);
        String date = body.getString("date");
        long age = RatePolicy.ageDays(date, now);
        JSONArray providers = body.optJSONArray("providers");
        StringBuilder names = new StringBuilder();
        boolean swiss = false;
        int providerCount = 0;
        if (providers != null) for (int i = 0; i < providers.length(); i++) {
            JSONObject p = providers.getJSONObject(i);
            if (p.optBoolean("excluded", false)) continue;
            String provider = p.optString("key");
            if (!provider.matches("[A-Z0-9_]{1,20}")) continue;
            if (provider.equals("ESTV")) swiss = true;
            providerCount++;
            if (providerCount <= 3) {
                if (names.length() > 0) names.append(", ");
                names.append(provider);
            }
        }
        if (asset.kind == Asset.Kind.CRYPTO && !swiss) throw new IOException("Missing Swiss source");
        String source = asset.kind == Asset.Kind.CRYPTO ? "Swiss Federal Tax Administration (ESTV) via Frankfurter"
                : "Frankfurter · official-source reference rates" + (names.length() > 0 ? "\nProviders: " + names + (providerCount > 3 ? " + " + (providerCount - 3) + " more" : "") : "");
        return new Quote(rate, date, source, saved, offline, age);
    }
    private static final class Response {
        final String body;
        final long serverTime;

        Response(String body, long serverTime) {
            this.body = body;
            this.serverTime = serverTime;
        }
    }
    private Response download(Asset asset, String range) throws Exception {
        String endpoint = range.isEmpty()
                ? "https://api.frankfurter.dev/v2/rate/USD/" + asset.code + "?expand=providers"
                : "https://api.frankfurter.dev/v2/rates?base=USD&quotes=" + asset.code + "&expand=providers" + range;
        if (asset.kind == Asset.Kind.CRYPTO) endpoint += "&providers=ESTV";
        HttpsURLConnection connection = (HttpsURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/json");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Rate service unavailable");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder body = new StringBuilder();
                char[] buffer = new char[2048]; int count;
                while ((count = reader.read(buffer)) != -1) {
                    body.append(buffer, 0, count);
                    if (body.length() > 65536) throw new IOException("Unexpected response size");
                }
                return new Response(body.toString(), connection.getHeaderFieldDate("Date", 0));
            }
        } finally { connection.disconnect(); }
    }
}
