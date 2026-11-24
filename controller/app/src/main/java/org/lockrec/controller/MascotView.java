package org.lockrec.controller;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;

public final class MascotView extends View {
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    float phase;
    ValueAnimator anim;
    public MascotView(Context c){super(c);setContentDescription("Animated anime-style moon guardian");}
    void color(int c){p.setColor(c);p.setStyle(Paint.Style.FILL);p.setShader(null);}
    void oval(Canvas c,float l,float t,float r,float b,int col){color(col);c.drawOval(l,t,r,b,p);}
    void path(Canvas c,int col,float... v){Path a=new Path();a.moveTo(v[0],v[1]);for(int i=2;i<v.length;i+=2)a.lineTo(v[i],v[i+1]);a.close();color(col);c.drawPath(a,p);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();anim=ValueAnimator.ofFloat(0,1);anim.setDuration(6500);anim.setRepeatCount(ValueAnimator.INFINITE);anim.setInterpolator(new LinearInterpolator());anim.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});anim.start();}
    @Override protected void onDetachedFromWindow(){if(anim!=null){anim.cancel();anim=null;}super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float scale=Math.min(getWidth()/340f,getHeight()/180f);
        c.save();c.translate((getWidth()-340*scale)/2,(getHeight()-180*scale)/2);c.scale(scale,scale);
        p.setShader(new LinearGradient(0,0,340,180,new int[]{0xff10172b,0xff21182e,0xff0d111d},null,Shader.TileMode.CLAMP));
        c.drawRoundRect(0,0,340,180,24,24,p);p.setShader(null);
        for(int i=0;i<27;i++){float x=(i*67+13)%330+5,y=(i*31+19)%165+6;int a=(int)(95+75*Math.sin(phase*6.283+i));color((a<<24)|0xadcaff);c.drawCircle(x,y,i%4==0?1.8f:.7f,p);}
        oval(c,214,12,313,111,0xff374264);oval(c,221,19,306,104,0xffcad8fd);oval(c,248,12,315,83,0xff211c31);
        c.save();c.translate(0,(float)Math.sin(phase*6.283)*2.5f);
        // Original cel-shaded character: midnight hair, violet eyes, star pin.
        oval(c,109,25,230,176,0xff171b37);
        path(c,0xff202a45,100,180,116,143,151,133,189,133,225,146,244,180);
        path(c,0xff566b9d,141,138,169,162,196,138,184,180,153,180);
        path(c,0xffecc0b8,156,119,184,119,184,140,170,151,156,140);
        oval(c,129,46,211,132,0xffffd9c8);
        oval(c,122,77,139,100,0xffeab8b2);oval(c,201,77,218,100,0xffeab8b2);
        path(c,0xff303a66,110,82,111,49,132,30,169,22,202,33,224,56,230,89,213,75,205,112,199,66,181,53,162,85,158,58,132,83,128,114);
        path(c,0xff6579b0,122,54,139,34,170,28,191,36,163,37,143,49,132,70);
        path(c,0xff8595c8,181,31,205,45,217,68,202,54);
        boolean blink=phase>.79f&&phase<.815f;
        for(int x:new int[]{151,189}){
            if(blink){color(0xff4a354e);p.setStrokeWidth(2);c.drawLine(x-10,91,x+10,91,p);}
            else{
                oval(c,x-12,82,x+12,101,0xff4a3557);oval(c,x-10,86,x+10,103,0xfff7ecf5);
                oval(c,x-7,85,x+7,101,0xff8f7adc);oval(c,x-3,87,x+3,98,0xff30254d);oval(c,x-5,86,x-1,90,0xfff8faff);
            }
        }
        oval(c,137,105,147,109,0xffefa9ae);oval(c,193,105,203,109,0xffefa9ae);
        color(0xffad667b);p.setStrokeWidth(1.5f);c.drawLine(166,115,174,115,p);
        path(c,0xfff5ddaa,207,47,210,54,218,55,212,60,214,68,207,64,200,68,202,60,196,55,204,54);
        c.restore();
        color(0xffb2c6f5);p.setTextSize(11);p.setTypeface(Typeface.create("sans-serif-medium",0));c.drawText("NIGHT MODE",17,153,p);
        color(0xff8e9abd);p.setTextSize(9);c.drawText("LOCKREC / 09",17,167,p);
        c.restore();
    }
}
