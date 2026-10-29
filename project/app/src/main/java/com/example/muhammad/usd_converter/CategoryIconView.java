package com.example.muhammad.usd_converter;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

/** Small, bundled canvas illustrations. No network or third-party runtime is needed. */
public class CategoryIconView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path path = new Path();
    private final RectF globeBounds = new RectF(7, 7, 93, 93);
    private final Shader coinGradient = new LinearGradient(20,12,80,85,new int[]{0xffffe29a,0xffdb9f34,0xfff5ca68},null,Shader.TileMode.CLAMP);
    private static final int SIZE = 112;
    private final int[] pixels = new int[SIZE * SIZE];
    private final double[] longitude = new double[SIZE * SIZE], latitude = new double[SIZE * SIZE];
    private final boolean[] visible = new boolean[SIZE * SIZE];
    private Bitmap globe;
    private int[] land;
    private int landWidth, landHeight, kind;
    private double centerLongitude = -80;

    public CategoryIconView(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setKind(int value) { kind = value; if (kind == 0) prepareGlobe(); invalidate(); }

    private void prepareGlobe() {
        Bitmap mask = BitmapFactory.decodeResource(getResources(), R.drawable.earth_land);
        landWidth = mask.getWidth(); landHeight = mask.getHeight(); land = new int[landWidth * landHeight];
        mask.getPixels(land, 0, landWidth, 0, 0, landWidth, landHeight); mask.recycle();
        globe = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        double tilt = Math.toRadians(12);
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            int i = y * SIZE + x;
            double xx = (x + .5 - SIZE / 2.0) / (SIZE / 2.0 - 2);
            double yy = -(y + .5 - SIZE / 2.0) / (SIZE / 2.0 - 2);
            double rho = Math.sqrt(xx * xx + yy * yy);
            if (rho > 1) continue;
            visible[i] = true;
            double z = Math.sqrt(1 - rho * rho);
            latitude[i] = Math.asin(z * Math.sin(tilt) + yy * Math.cos(tilt));
            longitude[i] = Math.atan2(xx, z * Math.cos(tilt) - yy * Math.sin(tilt));
        }
        renderGlobe();
    }
    protected final void setGlobeLongitude(double degrees) {
        centerLongitude = degrees;
        if (globe != null) { renderGlobe(); invalidate(); }
    }
    private void renderGlobe() {
        double center = Math.toRadians(centerLongitude);
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            int i = y * SIZE + x;
            if (!visible[i]) continue;
            double lon = longitude[i] + center;
            int mx = ((int)Math.floor((lon / (2 * Math.PI) + .5) * landWidth) % landWidth + landWidth) % landWidth;
            int my = Math.max(0, Math.min(landHeight - 1, (int)((.5 - latitude[i] / Math.PI) * landHeight)));
            boolean isLand = Color.red(land[my * landWidth + mx]) > 127;
            double xx = (x + .5 - SIZE / 2.0) / (SIZE / 2.0 - 2);
            double yy = (y + .5 - SIZE / 2.0) / (SIZE / 2.0 - 2);
            double z = Math.sqrt(Math.max(0, 1 - xx * xx - yy * yy));
            double light = .62 + .38 * Math.max(0, -.35 * xx - .35 * yy + .87 * z);
            int r = isLand ? 178 : 69, g = isLand ? 191 : 135, b = isLand ? 115 : 175;
            pixels[i] = Color.rgb((int)(r * light), (int)(g * light), (int)(b * light));
        }
        globe.setPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        canvas.save(); canvas.translate((getWidth() - size) / 2, (getHeight() - size) / 2); canvas.scale(size / 100f, size / 100f);
        if (kind == 0 && globe != null) {
            paint.setShader(null); paint.setColor(Color.WHITE);
            canvas.drawBitmap(globe, null, globeBounds, paint);
            paint.setStyle(Paint.Style.STROKE); paint.setColor(0x6688c4db); paint.setStrokeWidth(1);
            canvas.drawCircle(50, 50, 41, paint); paint.setStyle(Paint.Style.FILL);
        } else if (kind == 1) drawBars(canvas); else drawCoin(canvas);
        canvas.restore();
    }
    private void polygon(Canvas c, int color, float... xy) {
        paint.setShader(null); paint.setColor(color); path.reset(); path.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) path.lineTo(xy[i], xy[i + 1]); path.close(); c.drawPath(path, paint);
    }
    private void bar(Canvas c, float x, float y, boolean silver) {
        polygon(c, silver ? 0xffb6c2ca : 0xffe2ac43, x+7,y, x+38,y, x+45,y+14, x,y+14);
        polygon(c, silver ? 0xff83929e : 0xffa36b21, x,y+14,x+45,y+14,x+45,y+24,x,y+24);
        polygon(c, silver ? 0xffe0e7eb : 0xffffd577, x+7,y,x+38,y,x+40,y+5,x+5,y+5);
        polygon(c, silver ? 0xff687d8c : 0xff80551e, x+38,y,x+45,y+14,x+45,y+24,x+40,y+16);
    }
    private void drawBars(Canvas c) {
        paint.setColor(0x24000000); paint.setShader(null); c.drawOval(8,80,94,91,paint);
        bar(c,9,58,false); bar(c,49,54,true); bar(c,27,33,false); bar(c,45,10,false);
    }
    private void drawCoin(Canvas c) {
        paint.setShader(null); paint.setColor(0xff97651f); c.drawCircle(51,53,40,paint);
        paint.setShader(coinGradient);
        c.drawCircle(48,48,39,paint); paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2); paint.setColor(0xffa87527); c.drawCircle(48,48,33,paint);
        paint.setColor(0xffffe5a1); paint.setStrokeWidth(1); c.drawCircle(48,48,36,paint);
        // Draw the Bitcoin mark as paths, so Android 6 needs no Bitcoin font glyph.
        paint.setColor(0xff80541e); paint.setStrokeWidth(5); paint.setStrokeCap(Paint.Cap.SQUARE);
        path.reset(); path.moveTo(36,29); path.lineTo(36,68); path.moveTo(32,30); path.lineTo(48,30);
        path.cubicTo(68,30,68,47,48,47); path.lineTo(36,47); path.moveTo(48,47);
        path.cubicTo(71,47,71,67,48,67); path.lineTo(32,67); c.drawPath(path,paint);
        paint.setStrokeWidth(3); c.drawLine(42,23,42,30,paint); c.drawLine(51,23,51,30,paint);
        c.drawLine(42,68,42,75,paint); c.drawLine(51,68,51,75,paint);
        paint.setStyle(Paint.Style.FILL); paint.setStrokeCap(Paint.Cap.BUTT);
    }
}
