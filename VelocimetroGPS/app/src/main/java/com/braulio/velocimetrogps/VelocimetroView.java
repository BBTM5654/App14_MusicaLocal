package com.braulio.velocimetrogps;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
public class VelocimetroView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float velocidad = 0;
    private static final float MAX = 240;
    public VelocimetroView(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setVelocidad(float nueva) { velocidad = Math.max(0, Math.min(MAX, nueva)); invalidate(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth()/2f, cy = getHeight()/2f, radio = Math.min(getWidth(),getHeight())*0.40f;
        p.setStyle(Paint.Style.FILL); p.setColor(Color.rgb(26,32,52)); canvas.drawCircle(cx,cy,radio+17,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(13); p.setColor(Color.rgb(151,125,240));
        RectF arco = new RectF(cx-radio,cy-radio,cx+radio,cy+radio);
        canvas.drawArc(arco,135,270,false,p);
        p.setStrokeWidth(14); p.setColor(Color.rgb(255,78,128)); canvas.drawArc(arco,337.5f,67.5f,false,p);
        for(int i=0;i<=240;i+=10){
            float a=(float)Math.toRadians(135+(i/MAX)*270);
            float ext=radio-6, in=radio-(i%20==0?23:14);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(i%20==0?3:1.5f); p.setColor(Color.WHITE);
            canvas.drawLine(cx+(float)Math.cos(a)*ext,cy+(float)Math.sin(a)*ext,cx+(float)Math.cos(a)*in,cy+(float)Math.sin(a)*in,p);
            if(i%20==0){
                float rt=radio-45, tx=cx+(float)Math.cos(a)*rt,ty=cy+(float)Math.sin(a)*rt;
                p.setStyle(Paint.Style.FILL);p.setTextSize(19);p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.rgb(202,187,255));
                canvas.drawText(String.valueOf(i),tx,ty+6,p);
            }
        }
        float a=(float)Math.toRadians(135+(velocidad/MAX)*270), largo=radio*0.70f;
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(Color.rgb(255,65,110));
        canvas.drawLine(cx,cy,cx+(float)Math.cos(a)*largo,cy+(float)Math.sin(a)*largo,p);
        p.setStyle(Paint.Style.FILL);canvas.drawCircle(cx,cy,11,p);
        p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(17);canvas.drawText("km/h",cx,cy+radio*0.55f,p);
    }
}
