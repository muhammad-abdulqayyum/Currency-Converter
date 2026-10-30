package com.example.muhammad.usd_converter;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

/** Small, bundled canvas illustrations. No network or third-party runtime is needed. */
public class CategoryIconView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF globeBounds = new RectF(7, 7, 93, 93);
    private static final int SIZE = 112;
    private final int[] pixels = new int[SIZE * SIZE];
    private final double[] longitude = new double[SIZE * SIZE], latitude = new double[SIZE * SIZE];
    private final boolean[] visible = new boolean[SIZE * SIZE];
    private Bitmap globe;
    private Bitmap categoryArtwork;
    private int[] land;
    private int landWidth, landHeight, kind;
    private double centerLongitude = -80;

    public CategoryIconView(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setKind(int value) { kind = value; if (kind == 0) prepareGlobe(); else categoryArtwork = BitmapFactory.decodeResource(getResources(), kind == 1 ? R.drawable.premium_extras : R.drawable.premium_assets); invalidate(); }

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
        } else if (categoryArtwork != null) {
            paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.setFilterBitmap(true);
            AssetIconView.drawCell(canvas, categoryArtwork, kind == 1 ? 0 : 4, kind == 1 ? 2 : 4, kind == 1 ? 1 : 2, paint);
        }
        canvas.restore();
    }
}
