package com.example.muhammad.usd_converter;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Bundled, shared artwork atlas; no downloads or special symbol fonts. */
public final class AssetIconView extends View {
    private final Asset asset;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private static Bitmap artwork, extras;
    public AssetIconView(Context context, Asset value) {
        super(context); asset = value;
        if (artwork == null) artwork = BitmapFactory.decodeResource(getResources(), R.drawable.premium_assets);
        if (extras == null) extras = BitmapFactory.decodeResource(getResources(), R.drawable.premium_extras);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        canvas.save(); canvas.translate((getWidth()-size)/2, (getHeight()-size)/2); canvas.scale(size/100, size/100);
        paint.setStyle(Paint.Style.FILL); paint.setTextAlign(Paint.Align.CENTER);
        if (asset.kind == Asset.Kind.WORLD) {
            paint.setColor(0xff102c46); paint.setTextSize(58); paint.setTypeface(Typeface.DEFAULT);
            canvas.drawText(asset.icon,50,70,paint);
        } else {
            String code = asset.rateCode();
            int cell = code.equals("XAU")?0:code.equals("XAG")?1:code.equals("XPT")?2:code.equals("XPD")?3:code.equals("BTC")?4:code.equals("ETH")?5:code.equals("XRP")?6:7;
            if (code.equals("BCH")) drawCell(canvas, extras, 1, 2, 1, paint);
            else drawCell(canvas, artwork, cell, 4, 2, paint);
        }
        if (asset.isSubunit()) {
            paint.setColor(0xff102c46); canvas.drawRoundRect(45,70,98,94,7,7,paint);
            paint.setColor(Color.WHITE); paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD)); paint.setTextSize(14);
            canvas.drawText(asset.code.equals("SAT")?"sat":"drop",71,87,paint);
        }
        canvas.restore();
    }
    static void drawCell(Canvas canvas, Bitmap bitmap, int index, int columns, int rows, Paint paint) {
        int x=index%columns, y=index/columns;
        Rect source=new Rect(x*bitmap.getWidth()/columns,y*bitmap.getHeight()/rows,(x+1)*bitmap.getWidth()/columns,(y+1)*bitmap.getHeight()/rows);
        canvas.drawBitmap(bitmap,source,new RectF(0,0,100,100),paint);
    }
}
