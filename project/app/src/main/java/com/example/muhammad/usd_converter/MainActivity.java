package com.example.muhammad.usd_converter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;

/** Two-sided reference converter; category tabs filter each independent picker. */
public class MainActivity extends Activity {
    private static final int NAVY=0xff102c46,MUTED=0xff596873,GREEN=0xff32674e,CREAM=0xfffaf7f0;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final List<List<Asset>> groups=new ArrayList<>();
    private Asset from,to;
    private EditText amount;
    private TextView result,status,resultLabel,unit,note;
    private LinearLayout fromButton,toButton;
    private Button convert,swap;
    private ProgressBar progress;
    private RateRepository repository;
    private boolean destroyed,loading;
    private Dialog picker;
    private GlobeMotion pickerMotion;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        repository=new RateRepository(getSharedPreferences("reference_rates",MODE_PRIVATE));
        try{loadAssets();}catch(Exception e){setContentView(text(getString(R.string.catalog_error),18,NAVY));return;}
        from=find(state==null?"USD":state.getString("from","USD"));to=find(state==null?"GBP":state.getString("to","GBP"));
        buildScreen(state==null?"100":state.getString("amount","100"));
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);return v;}
    private TextView heading(String s,int size){TextView v=text(s,size,NAVY);v.setTypeface(Typeface.create("serif",Typeface.NORMAL));return v;}
    private GradientDrawable surface(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(16));d.setStroke(dp(1),0xffdedfd7);return d;}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private void add(LinearLayout parent,View v,int gap){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(gap);parent.addView(v,p);}
    private void buildScreen(String initial){
        ScrollView scroll=new ScrollView(this);scroll.setId(R.id.activity_main);scroll.setFillViewport(true);scroll.setBackgroundColor(CREAM);
        LinearLayout body=column();body.setPadding(dp(24),dp(20),dp(24),dp(32));body.setFocusableInTouchMode(true);scroll.addView(body);setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});scroll.requestApplyInsets();
        LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.universal_emblem);logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);brand.addView(logo,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView name=text(getString(R.string.brand),12,NAVY);name.setTypeface(null,Typeface.BOLD);name.setPadding(dp(12),0,0,0);brand.addView(name);add(body,brand,0);
        TextView version=text("VERSION 4 · FOR HUMANITY",11,0xff9b2637);version.setLetterSpacing(.08f);version.setTypeface(null,Typeface.BOLD);add(body,version,20);
        add(body,heading("A world within reach.",28),6);add(body,text("World currencies, metals and crypto.",14,MUTED),6);
        LinearLayout card=column();card.setPadding(dp(16),dp(12),dp(16),dp(12));card.setBackground(surface(0xfffffdfa));add(body,card,20);
        TextView label=text("Amount",13,MUTED);label.setLabelFor(R.id.editText);add(card,label,0);
        amount=new EditText(this);amount.setId(R.id.editText);amount.setInputType(8194);amount.setSingleLine(true);amount.setTextSize(28);amount.setTextColor(NAVY);amount.setMinHeight(dp(56));amount.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(22)});amount.setText(initial);add(card,amount,0);
        unit=text("",12,MUTED);add(card,unit,2);
        add(body,text("From",13,MUTED),18);fromButton=column();fromButton.setId(R.id.from_asset);add(body,fromButton,6);
        swap=new Button(this);swap.setId(R.id.swap);swap.setText("⇅  Swap");swap.setTextSize(14);swap.setTextColor(GREEN);swap.setAllCaps(false);swap.setBackground(surface(CREAM));swap.setContentDescription("Swap source and destination; keep the entered amount");LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(112),dp(48));sp.gravity=Gravity.CENTER_HORIZONTAL;sp.topMargin=dp(8);body.addView(swap,sp);
        add(body,text("To",13,MUTED),0);toButton=column();toButton.setId(R.id.to_asset);add(body,toButton,6);
        note=text("",12,MUTED);add(body,note,10);
        convert=new Button(this);convert.setId(R.id.convert);convert.setText("Convert");convert.setAllCaps(false);convert.setTextColor(0xffffffff);convert.setTextSize(17);convert.setMinHeight(dp(56));convert.setBackground(surface(NAVY));add(body,convert,16);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setVisibility(View.GONE);add(body,progress,4);
        LinearLayout resultCard=column();resultCard.setPadding(dp(18),dp(16),dp(18),dp(16));resultCard.setBackground(surface(0xffeef2e9));add(body,resultCard,18);
        resultLabel=text("YOUR CONVERSION",11,GREEN);resultLabel.setId(R.id.result_label);add(resultCard,resultLabel,0);
        result=heading("Ready to convert",25);result.setId(R.id.result);result.setTextIsSelectable(true);result.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);add(resultCard,result,8);
        status=text("",12,MUTED);status.setId(R.id.status);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);add(resultCard,status,10);
        LinearLayout footer=column();footer.setGravity(Gravity.CENTER_HORIZONTAL);footer.setPadding(0,dp(24),0,dp(12));add(body,footer,0);
        ImageView flag=new ImageView(this);flag.setImageResource(R.drawable.heritage_flag);flag.setContentDescription(getString(R.string.heritage_flag_description));footer.addView(flag,new LinearLayout.LayoutParams(dp(72),dp(48)));
        TextView credit=text(getString(R.string.footer),12,MUTED);credit.setGravity(Gravity.CENTER);credit.setLineSpacing(dp(4),1);add(footer,credit,10);
        Button about=new Button(this,null,android.R.attr.borderlessButtonStyle);about.setId(R.id.about);about.setText("About & sources");about.setAllCaps(false);about.setTextColor(GREEN);about.setMinHeight(dp(48));add(footer,about,4);about.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(R.string.app_name).setMessage(R.string.about_text).setPositiveButton(android.R.string.ok,null).show());
        fromButton.setOnClickListener(v->openPicker(true));toButton.setOnClickListener(v->openPicker(false));swap.setOnClickListener(v->{if(loading)return;Asset a=from;from=to;to=a;updatePair();});convert.setOnClickListener(v->convert());
        amount.addTextChangedListener(watcher(()->clearResult()));updatePair();body.requestFocus();
    }
    private TextWatcher watcher(Runnable changed){return new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){changed.run();}public void afterTextChanged(Editable e){}};}
    private Asset find(String code){for(List<Asset> group:groups)for(Asset a:group)if(a.code.equals(code))return a;return groups.get(0).get(0);}
    private String units(Asset a){return a.kind==Asset.Kind.METAL?"troy oz of "+a.name:a.code;}
    private String subtitle(Asset a){return a.code+(a.kind==Asset.Kind.METAL?" · troy oz":a.kind==Asset.Kind.CRYPTO?" · coin units":"");}
    private View assetRow(Asset a,boolean selected,boolean chevron){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(10),dp(12),dp(10));row.setMinimumHeight(dp(76));row.setBackground(surface(selected?0xffeaf0e3:0xfffffdfa));
        AssetIconView icon=new AssetIconView(this,a);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(icon,new LinearLayout.LayoutParams(dp(52),dp(52)));
        LinearLayout labels=column();labels.setPadding(dp(12),0,dp(6),0);row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));add(labels,heading(a.name,20),0);add(labels,text(subtitle(a),12,MUTED),3);
        row.addView(text(chevron?"⌄":selected?"✓":"",22,GREEN),new LinearLayout.LayoutParams(dp(24),-2));row.setContentDescription(a.name+", "+subtitle(a)+(selected?", selected":""));return row;
    }
    private void updatePair(){
        fromButton.removeAllViews();fromButton.addView(assetRow(from,false,true));fromButton.setFocusable(true);fromButton.setContentDescription("From: "+from.name+". Choose source asset");
        toButton.removeAllViews();toButton.addView(assetRow(to,false,true));toButton.setFocusable(true);toButton.setContentDescription("To: "+to.name+". Choose destination asset");
        unit.setText(from.kind==Asset.Kind.METAL?"Troy ounces of "+from.name+" · 1 troy oz = 31.1034768 g":from.kind==Asset.Kind.CRYPTO?from.code+" · units of "+from.name:from.code+" · "+from.name);
        String n="";if(from.kind==Asset.Kind.METAL||to.kind==Asset.Kind.METAL)n="Metals use troy ounces. Dealer premiums are excluded.";
        if(from.kind==Asset.Kind.CRYPTO||to.kind==Asset.Kind.CRYPTO)n+=(n.isEmpty()?"":"\n")+"Crypto uses dated Swiss reference values, not live prices.";
        note.setText(n);note.setVisibility(n.isEmpty()?View.GONE:View.VISIBLE);amount.setError(null);clearResult();
    }
    private void hideKeyboard(){((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(amount.getWindowToken(),0);}
    private void openPicker(boolean source){
        if(loading)return;hideKeyboard();final Asset chosen=source?from:to;final int[] category={chosen.kind.ordinal()};
        LinearLayout content=column();content.setPadding(dp(16),dp(12),dp(16),dp(12));content.setBackgroundColor(CREAM);add(content,heading(source?"Convert from":"Convert to",25),0);add(content,text("Choose any world currency, metal or crypto.",12,MUTED),5);
        LinearLayout tabs=new LinearLayout(this);add(content,tabs,12);String[] names={"World","Metals","Crypto"};View[] tabViews=new View[3];CategoryIconView worldIcon=null;
        for(int i=0;i<3;i++){
            LinearLayout tab=column();tab.setGravity(Gravity.CENTER);tab.setPadding(dp(4),dp(6),dp(4),dp(6));tab.setMinimumHeight(dp(76));tab.setId(new int[]{R.id.world_tab,R.id.metals_tab,R.id.crypto_tab}[i]);
            CategoryIconView icon=new CategoryIconView(this,null);icon.setKind(i);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);tab.addView(icon,new LinearLayout.LayoutParams(dp(36),dp(36)));if(i==0)worldIcon=icon;
            TextView label=text(names[i],13,NAVY);label.setGravity(Gravity.CENTER);tab.addView(label);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(2),0,dp(2),0);tabs.addView(tab,lp);tabViews[i]=tab;
        }
        EditText search=new EditText(this);search.setId(R.id.asset_search);search.setHint("Search name or code");search.setContentDescription("Search assets in selected category");search.setTextSize(15);search.setSingleLine(true);search.setMinHeight(dp(48));search.setInputType(1);add(content,search,10);
        ListView list=new ListView(this);list.setId(R.id.asset_list);list.setDividerHeight(dp(5));list.setPadding(0,dp(6),0,0);content.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        List<Asset> visible=new ArrayList<>();BaseAdapter adapter=new BaseAdapter(){public int getCount(){return visible.size();}public Object getItem(int p){return visible.get(p);}public long getItemId(int p){return p;}public View getView(int p,View recycled,ViewGroup parent){Asset a=visible.get(p);return assetRow(a,a.code.equals(chosen.code),false);}};list.setAdapter(adapter);
        TextView empty=text("No matches in this category.",14,MUTED);add(content,empty,8);list.setEmptyView(empty);
        Runnable filter=()->{visible.clear();String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);for(Asset a:groups.get(category[0]))if((a.name+" "+a.code).toLowerCase(Locale.ROOT).contains(q))visible.add(a);adapter.notifyDataSetChanged();for(int i=0;i<3;i++){tabViews[i].setBackground(surface(i==category[0]?0xffdfe9ed:CREAM));tabViews[i].setSelected(i==category[0]);tabViews[i].setContentDescription(names[i]+(i==category[0]?", selected":""));}};
        for(int i=0;i<3;i++){final int index=i;tabViews[i].setFocusable(true);tabViews[i].setOnClickListener(v->{category[0]=index;search.setText("");filter.run();if(pickerMotion!=null){pickerMotion.setSelected(index==0);if(index==0)pickerMotion.replay();}});}
        search.addTextChangedListener(watcher(filter));
        picker=new Dialog(this);picker.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Button cancel=new Button(this,null,android.R.attr.borderlessButtonStyle);cancel.setText("Cancel");cancel.setAllCaps(false);cancel.setTextColor(GREEN);cancel.setMinHeight(dp(48));add(content,cancel,4);picker.setContentView(content);cancel.setOnClickListener(v->picker.dismiss());picker.getWindow().setBackgroundDrawable(surface(CREAM));
        list.setOnItemClickListener((parent,v,position,id)->{if(source)from=visible.get(position);else to=visible.get(position);updatePair();picker.dismiss();});
        picker.setOnDismissListener(d->{if(pickerMotion!=null){pickerMotion.destroy();pickerMotion=null;}picker=null;});picker.show();picker.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(24),dp(520)),(int)(getResources().getDisplayMetrics().heightPixels*.86));picker.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        content.setFocusableInTouchMode(true);content.requestFocus();filter.run();pickerMotion=new GlobeMotion(worldIcon);pickerMotion.setSelected(category[0]==0);pickerMotion.setActive(true);final GlobeMotion motion=pickerMotion;worldIcon.post(motion::refreshVisibility);
    }
    private void clearResult(){if(result==null)return;resultLabel.setText("YOUR CONVERSION");result.setText("Ready to convert");status.setText("Choose your pair and tap Convert. Every reference rate has a date.");}
    private void busy(boolean value){loading=value;convert.setEnabled(!value);fromButton.setEnabled(!value);toButton.setEnabled(!value);swap.setEnabled(!value);amount.setEnabled(!value);progress.setVisibility(value?View.VISIBLE:View.GONE);}
    private String quoteDetails(Asset a,RateRepository.Quote q){if(a.code.equals("USD"))return "USD · common reference base";return a.code+" · "+q.date+(q.age>3?" · older reference ("+q.age+" days)":"")+"\n"+(q.offline?"Saved rate · connection unavailable":q.saved?"Saved rate · reused locally":"Reference rate retrieved")+"\n"+q.source;}
    private void convert(){
        if(loading)return;final BigDecimal input;
        try{input=Conversion.assetAmount(amount.getText().toString());amount.setError(null);}catch(IllegalArgumentException e){amount.setError("Enter 0 to 1 trillion, with up to 8 decimal places. Use a decimal point.");amount.requestFocus();return;}
        hideKeyboard();final Asset source=from,target=to;busy(true);result.setText("Getting your rate…");status.setText("Checking reference values…");
        executor.execute(()->{try{
            boolean same=source.code.equals(target.code);RateRepository.Quote first=same?null:repository.get(source),second=same?null:repository.get(target);
            BigDecimal output=same?input:Conversion.cross(input,first.rate,second.rate,target.code);
            NumberFormat format=NumberFormat.getNumberInstance();format.setMaximumFractionDigits(same?Math.max(Conversion.digits(target.code),input.scale()):Conversion.digits(target.code));format.setMinimumFractionDigits(target.kind==Asset.Kind.WORLD?Conversion.digits(target.code):0);
            String value=output.signum()==0&&input.signum()>0?"< "+format.format(BigDecimal.ONE.movePointLeft(Conversion.digits(target.code))):format.format(output);final String display=value+" "+units(target);
            String details=same?"Same asset · exact amount. No rate lookup needed.":quoteDetails(source,first)+"\n\n"+quoteDetails(target,second);
            if(!same&&!source.code.equals("USD")&&!target.code.equals("USD")&&!first.date.equals(second.date))details+="\n\nDifferent reference dates: this is an indicative comparison.";
            final String description=details;runOnUiThread(()->{if(destroyed)return;busy(false);resultLabel.setText(input.stripTrailingZeros().toPlainString()+" "+units(source)+" EQUALS");result.setText(display);status.setText(description);});
        }catch(Exception e){runOnUiThread(()->{if(destroyed)return;busy(false);result.setText(R.string.unavailable);status.setText(R.string.unavailable_detail);});}});
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(from!=null){out.putString("from",from.code);out.putString("to",to.code);out.putString("amount",amount.getText().toString());}}
    @Override protected void onResume(){super.onResume();if(pickerMotion!=null)pickerMotion.setActive(true);}
    @Override protected void onPause(){if(pickerMotion!=null)pickerMotion.setActive(false);super.onPause();}
    @Override protected void onDestroy(){destroyed=true;if(picker!=null)picker.dismiss();executor.shutdownNow();super.onDestroy();}
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
        world.add(new Asset("USD", "US Dollar", "🇺🇸", Asset.Kind.WORLD));
        java.util.Collections.sort(world, (a,b) -> a.name.compareToIgnoreCase(b.name));
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

}
