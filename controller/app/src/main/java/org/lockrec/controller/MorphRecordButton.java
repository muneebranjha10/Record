package org.lockrec.controller;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.view.accessibility.AccessibilityNodeInfo;

public final class MorphRecordButton extends View {
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress=0, press=1;
    private String state="checking";
    private ValueAnimator animator;
    public MorphRecordButton(Context c){super(c);setClickable(true);setFocusable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
    private int blend(int a,int b,float t){return Color.rgb((int)(Color.red(a)+(Color.red(b)-Color.red(a))*t),(int)(Color.green(a)+(Color.green(b)-Color.green(a))*t),(int)(Color.blue(a)+(Color.blue(b)-Color.blue(a))*t));}
    private boolean active(String s){return s.equals("recording")||s.equals("paused")||s.equals("stopping");}
    private String title(){switch(state){case "recording":case "paused":return "End recording";case "starting":return "Starting…";case "stopping":return "Saving video…";case "checking":return "Checking recorder…";default:return "Start recording";}}
    public void setRecorderState(String next){
        if(next.equals(state))return;
        state=next;setContentDescription(title());setEnabled(!next.equals("starting")&&!next.equals("stopping")&&!next.equals("checking"));
        if(animator!=null)animator.cancel();
        animator=ValueAnimator.ofFloat(progress,active(next)?1:0);animator.setDuration(430);animator.setInterpolator(new PathInterpolator(.22f,1f,.36f,1f));animator.addUpdateListener(a->{progress=(Float)a.getAnimatedValue();invalidate();});animator.start();
        invalidate();
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.Button");info.setText(title());}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight(),t=progress;
        if(w<=0||h<=0)return;
        float cx=w/2,cy=h/2;
        c.save();c.scale(press,press,cx,cy);
        float inset=dp(7+17*t),top=dp(9+2*t),bottom=h-top;
        float radius=dp(32-10*t);
        int a=blend(0xffb0a6ff,0xffff829e,t),b=blend(0xff6f9dff,0xffe84471,t);
        p.setShader(null);p.setColor((a&0xffffff)|0x33000000);c.drawRoundRect(inset-dp(4),top-dp(4),w-inset+dp(4),bottom+dp(4),radius+dp(4),radius+dp(4),p);
        p.setShader(new LinearGradient(inset,top,w-inset,bottom,a,b,Shader.TileMode.CLAMP));c.drawRoundRect(inset,top,w-inset,bottom,radius,radius,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(0x55ffffff);c.drawRoundRect(inset+.5f,top+.5f,w-inset-.5f,bottom-.5f,radius,radius,p);p.setStyle(Paint.Style.FILL);
        p.setTypeface(Typeface.create("sans-serif-medium",0));p.setTextSize(dp(16));String label=title();float width=p.measureText(label);
        float iconX=cx-(width+dp(29))/2+dp(8),size=dp(9-1*t);
        p.setColor(0xff10172f);c.drawRoundRect(iconX-size,cy-size,iconX+size,cy+size,dp(9-6*t),dp(9-6*t),p);
        float y=cy-(p.ascent()+p.descent())/2;c.drawText(label,iconX+dp(19),y,p);
        c.restore();
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent e){if(!isEnabled())return false;switch(e.getActionMasked()){case android.view.MotionEvent.ACTION_DOWN:press=.975f;invalidate();break;case android.view.MotionEvent.ACTION_UP:case android.view.MotionEvent.ACTION_CANCEL:press=1;invalidate();break;}return super.onTouchEvent(e);}
    @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
}
