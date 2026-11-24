package org.lockrec.controller;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;

public class MainActivity extends Activity {
    static final int BG=0xff08090b, CARD=0xff171b22, WHITE=0xfff4f6fa, MUTED=0xffa8afbc, BLUE=0xffa8c6ff;
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    TextView label(String text,int size,int color){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);return t;}
    Button button(String title,int color,int textColor,Runnable action){
        Button b=new Button(this);b.setText(title);b.setTextColor(textColor);b.setAllCaps(false);b.setTextSize(16);
        b.setBackground(shape(color,25));b.setMinHeight(dp(54));b.setOnClickListener(v->action.run());return b;
    }
    void add(LinearLayout box,android.view.View child,int gap){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(gap);box.addView(child,lp);}
    boolean notifyAllowed(){
        if(getSystemService(NotificationManager.class).areNotificationsEnabled())return true;
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);
        else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
        return false;
    }
    void control(String cmd){
        if(!notifyAllowed()){Toast.makeText(this,"Allow notifications, then tap again.",Toast.LENGTH_LONG).show();return;}
        startForegroundService(new Intent(this,RecorderService.class).setAction(cmd));
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setGravity(Gravity.CENTER);root.setPadding(dp(24),dp(32),dp(24),dp(32));scroll.addView(root);
        TextView name=label("LockRec",36,WHITE);name.setTypeface(Typeface.create("sans-serif-medium",0));add(root,name,0);
        add(root,label("Record beyond the lock screen",14,MUTED),8);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(20),dp(24),dp(20),dp(24));panel.setBackground(shape(CARD,28));add(root,panel,36);
        TextView icon=label("●",60,BLUE);add(panel,icon,0);
        add(panel,label("Native quality",22,WHITE),6);
        add(panel,label("Automatic FPS · Lock-screen mode",13,MUTED),8);
        add(panel,button("Start recording",BLUE,0xff111a28,()->control("begin")),24);
        add(panel,button("End recording",0xff30252a,0xffffc0c0,()->control("end")),10);
        add(root,button("Floating controls",CARD,WHITE,()->{
            if(!Settings.canDrawOverlays(this)){
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));
                Toast.makeText(this,"Allow display over other apps, then return and tap again.",Toast.LENGTH_LONG).show();
            }else control("show");
        }),16);
        add(root,label("Root access and LockRec module v0.4 required. Videos: Movies/LockRec. AOD depends on the device.",13,MUTED),20);
        setContentView(scroll);
    }
}