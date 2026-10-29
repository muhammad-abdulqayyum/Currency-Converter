package com.example.muhammad.usd_converter;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<List<Asset>> groups = new ArrayList<>();
    private final int[] selections = new int[3];
    private EditText amount;
    private Spinner currency;
    private TextView result, status, note, count, resultLabel;
    private Button convert;
    private final View[] tabs = new View[3];
    private ProgressBar progress;
    private RateRepository repository;
    private GlobeMotion globeMotion;
    private int section;
    private boolean destroyed, loading;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.activity_main);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();
        amount = findViewById(R.id.editText); currency = findViewById(R.id.currency);
        result = findViewById(R.id.result); status = findViewById(R.id.status);
        note = findViewById(R.id.section_note); count = findViewById(R.id.asset_count);
        resultLabel = findViewById(R.id.result_label);
        convert = findViewById(R.id.convert); progress = findViewById(R.id.progress);
        tabs[0] = findViewById(R.id.world_tab); tabs[1] = findViewById(R.id.metals_tab);
        tabs[2] = findViewById(R.id.crypto_tab);
        CategoryIconView worldIcon = findViewById(R.id.world_icon);
        worldIcon.setKind(0);
        globeMotion = new GlobeMotion(worldIcon);
        root.getViewTreeObserver().addOnScrollChangedListener(() -> globeMotion.refreshVisibility());
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> globeMotion.refreshVisibility());
        worldIcon.post(() -> globeMotion.refreshVisibility());
        ((CategoryIconView)findViewById(R.id.metals_icon)).setKind(1);
        ((CategoryIconView)findViewById(R.id.crypto_icon)).setKind(2);
        repository = new RateRepository(getSharedPreferences("reference_rates", MODE_PRIVATE));
        try { loadAssets(); }
        catch (Exception e) {
            convert.setEnabled(false); status.setText(R.string.catalog_error); return;
        }
        for (int i = 0; i < groups.get(0).size(); i++) if (groups.get(0).get(i).code.equals("GBP")) selections[0] = i;
        if (state != null) {
            int[] restored = state.getIntArray("selections");
            if (restored != null && restored.length == 3) System.arraycopy(restored, 0, selections, 0, 3);
        }
        for (int i = 0; i < 3; i++) { final int target = i; tabs[i].setOnClickListener(v -> { selectSection(target); if (target == 0 && !loading) globeMotion.replay(); }); }
        selectSection(state == null ? 0 : state.getInt("section", 0));
        convert.setOnClickListener(v -> convert());
        findViewById(R.id.about).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.app_name).setMessage(R.string.about_text)
                .setPositiveButton(android.R.string.ok, null).show());
    }
    private void loadAssets() throws Exception {
        List<Asset> world = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("currencies.tsv"), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\t");
                if (fields.length != 3 || !fields[0].matches("[A-Z]{3}")) throw new IllegalArgumentException("Invalid catalog");
                world.add(new Asset(fields[0], fields[1], fields[2], Asset.Kind.WORLD));
            }
        }
        if (world.isEmpty()) throw new IllegalArgumentException("Empty catalog");
        groups.add(world);
        List<Asset> metals = new ArrayList<>();
        metals.add(new Asset("XAU", "Gold", "☀", Asset.Kind.METAL));
        metals.add(new Asset("XAG", "Silver", "☾", Asset.Kind.METAL));
        metals.add(new Asset("XPT", "Platinum", "✦", Asset.Kind.METAL));
        metals.add(new Asset("XPD", "Palladium", "◇", Asset.Kind.METAL)); groups.add(metals);
        List<Asset> crypto = new ArrayList<>();
        crypto.add(new Asset("BTC", "Bitcoin", "₿", Asset.Kind.CRYPTO));
        crypto.add(new Asset("ETH", "Ethereum", "Ξ", Asset.Kind.CRYPTO));
        crypto.add(new Asset("XRP", "XRP", "◎", Asset.Kind.CRYPTO));
        crypto.add(new Asset("LTC", "Litecoin", "Ł", Asset.Kind.CRYPTO));
        crypto.add(new Asset("BCH", "Bitcoin Cash", "₿", Asset.Kind.CRYPTO)); groups.add(crypto);
    }
    private void selectSection(int target) {
        if (loading) return;
        section = Math.max(0, Math.min(2, target));
        for (int i = 0; i < 3; i++) { tabs[i].setSelected(i == section); tabs[i].setContentDescription(
                getString(new int[]{R.string.world_tab, R.string.metals_tab, R.string.crypto_tab}[i]) + (i == section ? ", selected" : "")); }
        ArrayAdapter<Asset> adapter = new ArrayAdapter<>(this, R.layout.asset_selected, groups.get(section));
        adapter.setDropDownViewResource(R.layout.asset_row);
        currency.setAdapter(adapter);
        currency.setSelection(Math.max(0, Math.min(groups.get(section).size() - 1, selections[section])));
        count.setText(getString(R.string.asset_count, groups.get(section).size()));
        note.setText(section == 0 ? R.string.world_note : section == 1 ? R.string.metals_note : R.string.crypto_note);
        currency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selections[section] = position; clearResult();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        globeMotion.setSelected(section == 0);
        clearResult();
    }
    private void clearResult() {
        resultLabel.setText(R.string.result_label);
        result.setText(R.string.result_placeholder);
        status.setText(R.string.ready_status);
    }
    private void busy(boolean value) {
        loading = value; convert.setEnabled(!value); currency.setEnabled(!value); amount.setEnabled(!value);
        for (View tab : tabs) tab.setEnabled(!value);
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
    }
    private void convert() {
        if (loading) return;
        final BigDecimal input;
        try { input = Conversion.amount(amount.getText().toString()); amount.setError(null); }
        catch (IllegalArgumentException e) { amount.setError(getString(R.string.amount_error)); amount.requestFocus(); return; }
        final Asset asset = groups.get(section).get(currency.getSelectedItemPosition());
        busy(true); result.setText(R.string.fetching); status.setText(R.string.connecting);
        executor.execute(() -> {
            try {
                RateRepository.Quote quote = repository.get(asset);
                BigDecimal output = Conversion.convert(input, quote.rate, asset.code);
                NumberFormat format = NumberFormat.getNumberInstance();
                format.setMinimumFractionDigits(asset.kind == Asset.Kind.WORLD ? Conversion.digits(asset.code) : 0);
                format.setMaximumFractionDigits(Conversion.digits(asset.code));
                String value = output.signum() == 0 && input.signum() > 0
                        ? "< " + format.format(BigDecimal.ONE.movePointLeft(Conversion.digits(asset.code)))
                        : format.format(output);
                final String display = value + " " + asset.unit();
                DecimalFormat rateFormat = new DecimalFormat("#,##0.###############");
                String details = (quote.offline ? "Saved rate · connection unavailable" : quote.saved ? "Saved rate · reused locally" : "Reference rate retrieved")
                        + "\nRate date: " + quote.date + (quote.age > 3 ? " · Older reference (" + quote.age + " days)" : "")
                        + "\n1 USD = " + rateFormat.format(quote.rate) + " " + asset.unit()
                        + "\n" + quote.source;
                runOnUiThread(() -> {
                    if (destroyed) return;
                    busy(false); resultLabel.setText(input.toPlainString() + " USD EQUALS");
                    result.setText(display); status.setText(details);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    busy(false); resultLabel.setText(R.string.result_label);
                    result.setText(R.string.unavailable); status.setText(R.string.unavailable_detail);
                });
            }
        });
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putInt("section", section); out.putIntArray("selections", selections);
    }
    @Override protected void onResume() { super.onResume(); if (globeMotion != null) globeMotion.setActive(true); }
    @Override protected void onPause() { if (globeMotion != null) globeMotion.setActive(false); super.onPause(); }
    @Override protected void onDestroy() { destroyed = true; if (globeMotion != null) globeMotion.destroy(); executor.shutdownNow(); super.onDestroy(); }
}
