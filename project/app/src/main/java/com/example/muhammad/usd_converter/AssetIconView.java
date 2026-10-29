package com.example.muhammad.usd_converter;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Local bullion and coin illustrations, without image downloads or special crypto fonts. */
public final class AssetIconView extends View {
    private final Asset asset;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    public AssetIconView(Context context,Asset value){super(context);asset=value;}
    private void polygon(Canvas c,int color,float...xy){p.setColor(color);path.reset();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);path.close();c.drawPath(path,p);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float size=Math.min(getWidth(),getHeight());c.save();c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/100,size/100);
        p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);
        if(asset.kind==Asset.Kind.WORLD){p.setTextSize(58);p.setTypeface(Typeface.DEFAULT);c.drawText(asset.icon,50,70,p);}
        else if(asset.kind==Asset.Kind.METAL){
            boolean gold=asset.code.equals("XAU");int top=gold?0xffedcb78:asset.code.equals("XPD")?0xffa9adaa:asset.code.equals("XPT")?0xffe0e5e4:0xffd2d6d6;
            p.setColor(0x18000000);c.drawOval(8,70,94,90,p);
            polygon(c,gold?0xff94702e:0xff717777,12,65,40,79,40,91,8,76);
            polygon(c,gold?0xffb38a3b:0xff949a98,40,79,86,40,91,53,40,91);
            polygon(c,top,12,65,56,21,86,35,40,79);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setColor(gold?0xffb28b43:0xff8d9593);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
            p.setColor(gold?0xff7b622e:0xff4f5958);p.setTextSize(24);p.setTypeface(Typeface.create("serif",Typeface.BOLD_ITALIC));
            String symbol=gold?"Au":asset.code.equals("XAG")?"Ag":asset.code.equals("XPT")?"Pt":"Pd";
            c.save();c.rotate(-35,50,50);c.drawText(symbol,50,58,p);c.restore();
        }else{
            boolean btc=asset.code.equals("BTC")||asset.code.equals("BCH");int base=btc?0xffe4b74f:asset.code.equals("ETH")?0xff8192b0:0xff729b9b;
            p.setColor(0x18000000);c.drawOval(10,78,91,94,p);p.setColor(base);c.drawCircle(50,49,39,p);
            p.setColor(0xfff5e4b0);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawCircle(50,49,33,p);p.setStyle(Paint.Style.FILL);
            p.setColor(0xff17324a);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(btc?44:22);c.drawText(btc?"B":asset.code,50,btc?65:57,p);
            if(btc){p.setStrokeWidth(2.5f);c.drawLine(45,21,45,30,p);c.drawLine(54,21,54,30,p);c.drawLine(45,65,45,75,p);c.drawLine(54,65,54,75,p);}
        }c.restore();
    }
}
