package com.example.muhammad.usd_converter;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final String[] codes = {"GBP", "EUR", "JPY", "BRL"};
    private EditText amount;
    private Spinner currency;
    private TextView result, status;
    private Button convert;
    private ProgressBar progress;
    private boolean destroyed;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.activity_main);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();
        amount = findViewById(R.id.editText);
        currency = findViewById(R.id.currency);
        result = findViewById(R.id.result);
        status = findViewById(R.id.status);
        convert = findViewById(R.id.convert);
        progress = findViewById(R.id.progress);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"GBP — British pound", "EUR — Euro", "JPY — Japanese yen", "BRL — Brazilian real"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currency.setAdapter(adapter);
        if (state != null) {
            currency.setSelection(state.getInt("currency"));
            result.setText(state.getString("result", "Your conversion appears here"));
            status.setText(state.getString("status", "Tap Convert to fetch rates."));
        }
        convert.setOnClickListener(v -> convert());
    }
    private void busy(boolean loading) {
        convert.setEnabled(!loading);
        currency.setEnabled(!loading);
        amount.setEnabled(!loading);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
    private void convert() {
        final BigDecimal input;
        try { input = Conversion.amount(amount.getText().toString()); }
        catch (IllegalArgumentException e) { amount.setError("Enter a valid USD amount from 0 to 1 trillion."); return; }
        final String code = codes[currency.getSelectedItemPosition()];
        busy(true);
        result.setText("Fetching rate…");
        status.setText("Connecting to Frankfurter…");
        executor.execute(() -> {
            try {
                JSONObject response = fetch(code);
                BigDecimal rate = new BigDecimal(response.getString("rate"));
                if (!"USD".equals(response.getString("base")) || !code.equals(response.getString("quote")))
                    throw new IOException("Unexpected currency pair");
                BigDecimal output = Conversion.convert(input, rate, code);
                String date = response.getString("date");
                if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) throw new IOException("Missing rate date");
                NumberFormat money = NumberFormat.getCurrencyInstance();
                money.setCurrency(Currency.getInstance(code));
                money.setMinimumFractionDigits(Currency.getInstance(code).getDefaultFractionDigits());
                money.setMaximumFractionDigits(Currency.getInstance(code).getDefaultFractionDigits());
                String text = input.toPlainString() + " USD = " + money.format(output) + " " + code;
                runOnUiThread(() -> {
                    if (destroyed) return;
                    busy(false);
                    result.setText(text);
                    status.setText("1 USD = " + rate.toPlainString() + " " + code + "\nRate date: " + date + " · Frankfurter\nDaily reference rate; bank fees and spreads are not included.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    busy(false);
                    result.setText("Conversion unavailable");
                    status.setText("Could not retrieve a valid rate. Check your connection and tap Convert to retry. No estimated or old rate was used.");
                });
            }
        });
    }
    private JSONObject fetch(String code) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) new URL("https://api.frankfurter.dev/v2/rate/USD/" + code).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("Accept", "application/json");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Rate service unavailable");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                    if (body.length() > 65536) throw new IOException("Unexpected response size");
                }
                return new JSONObject(body.toString());
            }
        } finally { connection.disconnect(); }
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("currency", currency.getSelectedItemPosition());
        out.putString("result", convert.isEnabled() ? result.getText().toString() : "Tap Convert to try again.");
        out.putString("status", convert.isEnabled() ? status.getText().toString() : "Conversion interrupted by screen change.");
    }
    @Override protected void onDestroy() { destroyed = true; executor.shutdownNow(); super.onDestroy(); }
}
