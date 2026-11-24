package org.lockrec.controller;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Original layered anime-inspired scene. Drawn locally; no network or image assets. */
public final class AnimeSceneView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase;
    private ValueAnimator motion;
    private boolean animate=true;
    public AnimeSceneView(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setClickable(false);}
    private void fill(int color){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);}
    private void ellipse(Canvas c,float l,float t,float r,float b,int color){fill(color);c.drawOval(l,t,r,b,p);}
    private void line(Canvas c,float x,float y,float x2,float y2,int color,float width){fill(color);p.setStrokeWidth(width);c.drawLine(x,y,x2,y2,p);}
    private void poly(Canvas c,int color,float... xy){Path a=new Path();a.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)a.lineTo(xy[i],xy[i+1]);a.close();fill(color);c.drawPath(a,p);}
    private void paintPath(Canvas c,Path path,int a,int b,float y1,float y2){
        p.setColor(Color.WHITE);p.setShader(new LinearGradient(0,y1,0,y2,a,b,Shader.TileMode.CLAMP));c.drawPath(path,p);p.setShader(null);
    }
    public void setMotionEnabled(boolean enabled){animate=enabled;if(!enabled){if(motion!=null){motion.cancel();motion=null;}}else if(isAttachedToWindow())start();}
    private void start(){if(!animate||motion!=null)return;motion=ValueAnimator.ofFloat(0,1);motion.setDuration(12000);motion.setRepeatCount(ValueAnimator.INFINITE);motion.setInterpolator(new LinearInterpolator());motion.addUpdateListener(a->{phase=(Float)a.getAnimatedValue();postInvalidateOnAnimation();});motion.start();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();start();}
    @Override protected void onDetachedFromWindow(){if(motion!=null){motion.cancel();motion=null;}super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(getWidth()==0||getHeight()==0)return;
        Canvas c=canvas; c.save();c.scale(getWidth()/360f,getHeight()/800f);
        p.setColor(Color.WHITE);p.setShader(new LinearGradient(0,0,320,800,new int[]{0xff080d23,0xff23204c,0xff121c3d,0xff060913},new float[]{0,.30f,.62f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,360,800,p);p.setShader(null);
        float drift=(float)Math.sin(phase*6.283185)*5;
        p.setShader(new RadialGradient(266,128,165,new int[]{0x887e85e9,0x334e5dcb,0x002c3280},null,Shader.TileMode.CLAMP));c.drawCircle(266,128,165,p);p.setShader(null);
        p.setShader(new LinearGradient(214,45,300,195,0xfff0e4ff,0xff8f9ada,Shader.TileMode.CLAMP));c.drawCircle(265,115,62,p);p.setShader(null);
        ellipse(c,224,83,245,103,0x17989bc7);ellipse(c,259,130,289,154,0x24969dc8);
        for(int i=0;i<62;i++){float x=(i*79+19)%356,y=(i*47+7)%380;int a=(int)(105+65*Math.sin(i+phase*6.283));fill((a<<24)|0xbed4ff);c.drawCircle(x+drift*.2f,y,i%9==0?1.6f:.65f,p);}
        // Mountain silhouettes and layered city skyline.
        poly(c,0xff1e2850,0,300,0,240,33,210,70,263,107,220,148,275,188,230,234,263,300,200,360,255,360,390,0,390);
        for(int layer=0;layer<2;layer++){
            for(int i=0;i<19;i++){
                int w=11+(i*13)%19;float x=i*23-22+layer*9;float base=390+layer*49;float top=base-45-(i*37+layer*21)%115;
                fill(layer==0?0xff151b37:0xff10172d);c.drawRect(x,top,x+w,base,p);
                for(float y=top+10;y<base-5;y+=12)if(((int)y+i)%3!=0){fill(0x555a9cd8);c.drawRect(x+3,y,x+5,y+4,p);}
            }
        }
        // Anime character: rim-lit midnight hair, jacket, expressive eyes.
        c.save();c.translate(104+drift*.25f,22);c.scale(1.13f,1.13f);
        Path hair=new Path();hair.moveTo(19,138);hair.cubicTo(-1,64,25,14,87,14);hair.cubicTo(150,8,183,58,171,133);hair.cubicTo(168,170,184,208,192,239);hair.lineTo(20,239);hair.cubicTo(45,193,18,172,19,138);hair.close();
        paintPath(c,hair,0xff6472b7,0xff171d3e,10,240);
        Path coat=new Path();coat.moveTo(2,255);coat.cubicTo(14,206,46,194,73,191);coat.lineTo(112,191);coat.cubicTo(149,194,179,211,190,255);coat.lineTo(2,255);coat.close();paintPath(c,coat,0xff485b94,0xff17213b,188,255);
        poly(c,0xff151d35,45,201,75,193,91,219,61,253,30,253);poly(c,0xff8595c8,110,193,143,205,162,253,119,253,92,220);
        ellipse(c,73,161,112,209,0xffd9a5a3);
        Path face=new Path();face.moveTo(47,65);face.cubicTo(54,35,130,35,141,72);face.lineTo(145,128);face.cubicTo(137,156,115,177,94,180);face.cubicTo(71,175,50,154,43,129);face.close();paintPath(c,face,0xffffe4d8,0xffd6a5b1,55,180);
        ellipse(c,39,113,52,137,0xffd3a0ac);ellipse(c,136,113,150,137,0xffd3a0ac);
        Path fringe=new Path();fringe.moveTo(22,123);fringe.cubicTo(2,55,35,22,84,17);fringe.cubicTo(144,11,176,50,168,111);fringe.lineTo(146,91);fringe.lineTo(138,149);fringe.lineTo(126,74);fringe.lineTo(100,102);fringe.lineTo(111,54);fringe.lineTo(65,105);fringe.lineTo(69,62);fringe.lineTo(45,116);fringe.lineTo(39,158);fringe.close();paintPath(c,fringe,0xff8194dc,0xff293052,15,151);
        Path shine=new Path();shine.moveTo(35,65);shine.quadTo(69,23,115,30);shine.quadTo(66,42,48,79);shine.close();paintPath(c,shine,0xffb1b7e8,0x005e75b0,30,82);
        boolean blink=phase>.68f&&phase<.697f;
        for(int x:new int[]{70,117}){
            if(blink){line(c,x-12,123,x+12,123,0xff56364f,2);}
            else{ellipse(c,x-13,111,x+13,135,0xff39283f);ellipse(c,x-11,115,x+11,136,0xfffff2ef);ellipse(c,x-8,114,x+8,134,0xffa18ee8);ellipse(c,x-4,115,x+4,131,0xff342a61);ellipse(c,x-6,115,x-1,120,0xffffffff);}
        }
        line(c,93,149,99,150,0xffa96885,1.4f);
        ellipse(c,51,141,63,145,0x559c4e78);ellipse(c,125,141,137,145,0x559c4e78);
        poly(c,0xfff6dba6,144,61,148,70,158,71,150,77,152,87,144,81,136,87,138,77,130,71,140,70);
        c.restore();
        // Perspective illuminated street, then dark readability veil across controls.
        poly(c,0xff11172e,0,800,0,450,177,369,360,450,360,800);
        for(int i=-5;i<=5;i++)line(c,180,370,180+i*90,800,0x22528cdd,1);
        for(int i=0;i<7;i++){float y=390+i*i*10;line(c,0,y,360,y,0x22528cdd,1);}
        p.setColor(Color.WHITE);p.setShader(new LinearGradient(0,0,0,800,new int[]{0x15060a16,0x11060a16,0x96060914,0xe9090c18,0xf6060810},new float[]{0,.23f,.45f,.74f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,360,800,p);p.setShader(null);
        c.restore();
    }
}
