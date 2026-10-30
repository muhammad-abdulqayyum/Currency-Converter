package com.example.muhammad.usd_converter;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Local bullion and coin illustrations, without image downloads or special crypto fonts. */
public final class AssetIconView extends View {
    private final Asset asset;
    private final Bitmap cryptoIcon;
    private static final java.util.Map<String,Bitmap> ICONS=new java.util.HashMap<>();
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    public AssetIconView(Context context,Asset value){super(context);asset=value;
        String code=asset.rateCode();int id=code.equals("ETH")?R.drawable.crypto_eth:code.equals("LTC")?R.drawable.crypto_ltc:code.equals("XRP")?R.drawable.crypto_xrp:code.equals("BTC")?R.drawable.crypto_btc:code.equals("BCH")?R.drawable.crypto_bch:code.equals("XAU")?R.drawable.metal_xau:code.equals("XAG")?R.drawable.metal_xag:code.equals("XPT")?R.drawable.metal_xpt:code.equals("XPD")?R.drawable.metal_xpd:0;
        if(id!=0){if(!ICONS.containsKey(code))ICONS.put(code,BitmapFactory.decodeResource(getResources(),id));cryptoIcon=ICONS.get(code);}else cryptoIcon=null;
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float size=Math.min(getWidth(),getHeight());c.save();c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/100,size/100);
        p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);
        if(asset.kind==Asset.Kind.WORLD){p.setTextSize(58);p.setTypeface(Typeface.DEFAULT);c.drawText(asset.icon,50,70,p);}
        else if(cryptoIcon!=null){
            p.setFilterBitmap(true);c.drawBitmap(cryptoIcon,null,new RectF(2,2,98,98),p);
        }else{
            boolean btc=asset.rateCode().equals("BTC")||asset.code.equals("BCH");int base=btc?0xffe4b74f:asset.code.equals("ETH")?0xff8192b0:0xff729b9b;
            p.setColor(0x18000000);c.drawOval(10,78,91,94,p);p.setColor(base);c.drawCircle(50,49,39,p);
            p.setColor(0xfff5e4b0);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawCircle(50,49,33,p);p.setStyle(Paint.Style.FILL);
            p.setColor(0xff17324a);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(btc?44:22);c.drawText(btc?"B":asset.code,50,btc?65:57,p);
            if(btc){p.setStrokeWidth(2.5f);c.drawLine(45,21,45,30,p);c.drawLine(54,21,54,30,p);c.drawLine(45,65,45,75,p);c.drawLine(54,65,54,75,p);}
        }
        if(asset.isSubunit()){
            p.setStyle(Paint.Style.FILL);p.setColor(0xff102c46);c.drawRoundRect(45,70,98,94,7,7,p);
            p.setColor(Color.WHITE);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(14);c.drawText(asset.code.equals("SAT")?"sat":"drop",71,87,p);
        }
        c.restore();
    }
}
