package com.example.muhammad.usd_converter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.widget.Button;

/** A compact drawn control inside a full 48 dp accessible touch target. */
public final class SwapButton extends Button {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public SwapButton(Context context){
        super(context);
        setBackgroundColor(android.graphics.Color.TRANSPARENT);
        setPadding(0,0,0,0);
        setMinimumWidth(0);setMinimumHeight(0);
    }
    @Override protected void onDraw(Canvas canvas){
        float d=getResources().getDisplayMetrics().density;
        canvas.save();canvas.translate(getWidth()/2f,getHeight()/2f);canvas.scale(d,d);
        paint.setAlpha(isEnabled()?255:100);
        paint.setStyle(Paint.Style.FILL);paint.setColor(isPressed()||isFocused()?0xffe2e9e9:0xfffaf7f0);
        canvas.drawCircle(0,0,15,paint);
        paint.setColor(0xff102c46);paint.setAlpha(isEnabled()?255:100);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.1f);canvas.drawCircle(0,0,15,paint);
        paint.setStrokeWidth(1.6f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        Path arrows=new Path();
        arrows.moveTo(-7,-3.5f);arrows.lineTo(7,-3.5f);arrows.lineTo(3.5f,-7);
        arrows.moveTo(7,-3.5f);arrows.lineTo(3.5f,0);
        arrows.moveTo(7,3.5f);arrows.lineTo(-7,3.5f);arrows.lineTo(-3.5f,0);
        arrows.moveTo(-7,3.5f);arrows.lineTo(-3.5f,7);
        canvas.drawPath(arrows,paint);canvas.restore();
    }
}
