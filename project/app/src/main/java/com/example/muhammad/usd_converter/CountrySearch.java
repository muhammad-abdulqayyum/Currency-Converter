package com.example.muhammad.usd_converter;

import android.content.res.AssetManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class CountrySearch {
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private final JSONObject countries;

    CountrySearch(AssetManager assets) {
        JSONObject loaded = new JSONObject();
        try (InputStream input = assets.open("country-currencies.json");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            loaded = new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
        } catch (IOException | JSONException error) {
            android.util.Log.w("CountrySearch", "Could not load country dictionary", error);
        }
        countries = loaded;
    }

    static String normalize(String value) {
        String decomposed = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return MARKS.matcher(decomposed).replaceAll("");
    }

    Set<String> currencies(String query, String excludedCountry) {
        Set<String> matches = new HashSet<>();
        if (query.isEmpty()) return matches;
        for (Iterator<String> keys = countries.keys(); keys.hasNext();) {
            String code = keys.next();
            if (code.equals(excludedCountry)) continue;
            JSONObject country = countries.optJSONObject(code);
            if (country == null) continue;
            boolean exact = code.equalsIgnoreCase(query) || country.optString("alpha3").equalsIgnoreCase(query);
            JSONArray exactAliases = country.optJSONArray("exactAliases");
            for (int i = 0; !exact && exactAliases != null && i < exactAliases.length(); i++) {
                exact = normalize(exactAliases.optString(i)).equals(query);
            }
            boolean matched = exact || (query.length() >= 3 && normalize(country.optString("name")).contains(query));
            JSONArray aliases = country.optJSONArray("aliases");
            for (int i = 0; !matched && aliases != null && i < aliases.length(); i++) {
                String alias = normalize(aliases.optString(i));
                matched = alias.equals(query) || (query.length() >= 3 && alias.contains(query));
            }
            JSONArray currencies = country.optJSONArray("currencies");
            if (exact) matches.clear();
            if (matched && currencies != null) {
                for (int i = 0; i < currencies.length(); i++) matches.add(currencies.optString(i));
            }
            if (exact) return matches;
        }
        return matches;
    }
}
