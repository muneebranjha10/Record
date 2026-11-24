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
    private void glow(Canvas c,float x,float y,float radius,int center,int edge){
        p.setColor(Color.WHITE);p.setShader(new RadialGradient(x,y,radius,center,edge,Shader.TileMode.CLAMP));c.drawCircle(x,y,radius,p);p.setShader(null);
    }
    private void orbit(Canvas c,float x,float y,float rx,float ry,float degrees,int col){
        c.save();c.translate(x,y);c.rotate(-19);
        p.setShader(null);p.setColor((col&0xffffff)|0x33000000);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8);
        c.drawOval(-rx,-ry,rx,ry,p);p.setColor(col);p.setStrokeWidth(1.7f);c.drawOval(-rx,-ry,rx,ry,p);
        c.drawArc(-rx,-ry,rx,ry,degrees,42,false,p);p.setStyle(Paint.Style.FILL);
        float a=(float)Math.toRadians(degrees);glow(c,(float)Math.cos(a)*rx,(float)Math.sin(a)*ry,12,col,col&0xffffff);c.restore();
    }
    private void drawCharacter(Canvas c,float originX,float originY,float scale,boolean pink,boolean mirror){
        // Anime character: rim-lit midnight hair, jacket, expressive eyes.
        c.save();c.translate(originX,originY);c.scale(scale,scale);if(mirror){c.translate(192,0);c.scale(-1,1);}
        Path hair=new Path();hair.moveTo(19,138);hair.cubicTo(-1,64,25,14,87,14);hair.cubicTo(150,8,183,58,171,133);hair.cubicTo(168,170,184,208,192,239);hair.lineTo(20,239);hair.cubicTo(45,193,18,172,19,138);hair.close();
        paintPath(c,hair,pink?0xfff8aadf:0xff9bdcff,pink?0xff51224f:0xff173a69,10,240);
        if(pink){
            ellipse(c,3,19,50,65,0xffbf70b9);ellipse(c,140,14,185,61,0xff9b4d97);
            ellipse(c,11,22,42,47,0xfff6b6e4);ellipse(c,147,17,178,43,0xffe69ed6);
        } else {
            poly(c,0xff76caff,10,79,6,44,24,20,39,13,25,48);
            poly(c,0xff3676bc,155,24,182,42,188,76,168,59);
        }
        Path coat=new Path();coat.moveTo(2,255);coat.cubicTo(14,206,46,194,73,191);coat.lineTo(112,191);coat.cubicTo(149,194,179,211,190,255);coat.lineTo(2,255);coat.close();paintPath(c,coat,pink?0xffcd6caa:0xff4ba9ee,pink?0xff381c42:0xff182849,188,255);
        poly(c,pink?0xff481f45:0xff12284b,45,201,75,193,91,219,61,253,30,253);poly(c,pink?0xffffb5df:0xff8ddfff,110,193,143,205,162,253,119,253,92,220);
        ellipse(c,73,161,112,209,0xffd9a5a3);
        Path face=new Path();face.moveTo(47,65);face.cubicTo(54,35,130,35,141,72);face.lineTo(145,128);face.cubicTo(137,156,115,177,94,180);face.cubicTo(71,175,50,154,43,129);face.close();paintPath(c,face,0xffffe4d8,0xffd6a5b1,55,180);
        ellipse(c,39,113,52,137,0xffd3a0ac);ellipse(c,136,113,150,137,0xffd3a0ac);
        Path fringe=new Path();fringe.moveTo(22,123);fringe.cubicTo(2,55,35,22,84,17);fringe.cubicTo(144,11,176,50,168,111);fringe.lineTo(146,91);fringe.lineTo(138,149);fringe.lineTo(126,74);fringe.lineTo(100,102);fringe.lineTo(111,54);fringe.lineTo(65,105);fringe.lineTo(69,62);fringe.lineTo(45,116);fringe.lineTo(39,158);fringe.close();paintPath(c,fringe,pink?0xffffc2ec:0xffb3ebff,pink?0xff963b86:0xff2456a4,15,151);
        Path shine=new Path();shine.moveTo(35,65);shine.quadTo(69,23,115,30);shine.quadTo(66,42,48,79);shine.close();paintPath(c,shine,pink?0xffffe2f6:0xffe1faff,pink?0x00ff90cb:0x005eaaff,30,82);
        boolean blink=pink?(phase>.35f&&phase<.368f):(phase>.68f&&phase<.697f);
        for(int x:new int[]{70,117}){
            if(blink){line(c,x-12,123,x+12,123,0xff56364f,2);}
            else{ellipse(c,x-13,111,x+13,135,0xff39283f);ellipse(c,x-11,115,x+11,136,0xfffff2ef);ellipse(c,x-8,114,x+8,134,pink?0xffe67dbe:0xff6fcfff);ellipse(c,x-4,115,x+4,131,pink?0xff592b65:0xff193b68);ellipse(c,x-6,115,x-1,120,0xffffffff);}
        }
        line(c,93,149,99,150,0xffa96885,1.4f);
        ellipse(c,51,141,63,145,0x559c4e78);ellipse(c,125,141,137,145,0x559c4e78);
        poly(c,pink?0xff9feaff:0xffffb9e8,144,61,148,70,158,71,150,77,152,87,144,81,136,87,138,77,130,71,140,70);
        if(!pink){line(c,151,110,163,111,0xff9ceaff,4);line(c,162,111,157,141,0xff5cc9ff,3);ellipse(c,151,137,160,145,0xffb7f5ff);}
        c.restore();
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(getWidth()==0||getHeight()==0)return;
        Canvas c=canvas; c.save();c.scale(getWidth()/360f,getHeight()/800f);
        p.setColor(Color.WHITE);p.setShader(new LinearGradient(0,0,320,800,new int[]{0xff180c27,0xff392153,0xff112950,0xff080819},new float[]{0,.30f,.62f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,360,800,p);p.setShader(null);
        float drift=(float)Math.sin(phase*6.283185)*5;
        p.setShader(new RadialGradient(266,128,165,new int[]{0x88ee8cdf,0x33519de9,0x002c3280},null,Shader.TileMode.CLAMP));c.drawCircle(266,128,165,p);p.setShader(null);
        p.setShader(new LinearGradient(214,45,300,195,0xffffe4f9,0xff8bcbff,Shader.TileMode.CLAMP));c.drawCircle(265,115,62,p);p.setShader(null);
        ellipse(c,224,83,245,103,0x17989bc7);ellipse(c,259,130,289,154,0x24969dc8);
        for(int i=0;i<62;i++){float x=(i*79+19)%356,y=(i*47+7)%380;int a=(int)(105+65*Math.sin(i+phase*6.283));fill((a<<24)|0xbed4ff);c.drawCircle(x+drift*.2f,y,i%9==0?1.6f:.65f,p);}
        // Mountain silhouettes and layered city skyline.
        poly(c,0xff32305e,0,300,0,240,33,210,70,263,107,220,148,275,188,230,234,263,300,200,360,255,360,390,0,390);
        for(int layer=0;layer<2;layer++){
            for(int i=0;i<19;i++){
                int w=11+(i*13)%19;float x=i*23-22+layer*9;float base=390+layer*49;float top=base-45-(i*37+layer*21)%115;
                fill(layer==0?0xff302343:0xff132747);c.drawRect(x,top,x+w,base,p);
                for(float y=top+10;y<base-5;y+=12)if(((int)y+i)%3!=0){fill((i%2==0?0x66ff8bce:0x6665caff));c.drawRect(x+3,y,x+5,y+4,p);}
            }
        }
        // Backlit orbit gate with perspective rings and twin foreground characters.
        glow(c,92,150,128,0x99ff55b5,0x00ff55b5);
        glow(c,265,157,126,0x9953baff,0x0053baff);
        orbit(c,180,155,123,87,phase*360,0xfff299df);
        orbit(c,180,155,112,75,-phase*360,0xff73d7ff);
        drawCharacter(c,5+drift*.45f,64+drift*.4f,.84f,true,false);
        drawCharacter(c,180-drift*.45f,59-drift*.4f,.89f,false,true);
        // Drifting petal shards and luminous foreground crystals.
        for(int i=0;i<14;i++){
            float z=.35f+((i*13)%10)*.075f;
            float x=(i*59+phase*360*z)%380-10;
            float y=(i*37+19)%300+30+(float)Math.sin(phase*6.28+i)*12;
            c.save();c.translate(x,y);c.rotate(i*29+phase*360);
            ellipse(c,-3*z,-8*z,3*z,8*z,i%2==0?0xaaff9fdf:0xaa98e4ff);c.restore();
        }
        // Perspective illuminated street, then dark readability veil across controls.
        poly(c,0xff181a37,0,800,0,450,177,369,360,450,360,800);
        for(int i=-5;i<=5;i++)line(c,180,370,180+i*90,800,0x5567caff,1);
        for(int i=0;i<7;i++){float y=390+i*i*10;line(c,0,y,360,y,0x22528cdd,1);}
        p.setColor(Color.WHITE);p.setShader(new LinearGradient(0,0,0,800,new int[]{0x080b1025,0x080b1025,0x700e1028,0xd90f1028,0xee08091b},new float[]{0,.23f,.45f,.74f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,360,800,p);p.setShader(null);
        c.restore();
    }
}
