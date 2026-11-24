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
    Spinner resolution, fps;
    String[] resolutions, rates={"auto"};
    TextView fpsNote;
    CheckBox autoHide;
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    TextView label(String text,int size,int color){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);return t;}
    Button button(String title,int color,int textColor,Runnable action){
        Button b=new Button(this);b.setText(title);b.setTextColor(textColor);b.setAllCaps(false);b.setTextSize(16);
        b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x44ffffff),shape(color,25),null));
        b.setMinHeight(dp(54));b.setOnClickListener(v->action.run());
        b.setOnTouchListener((v,e)->{int a=e.getActionMasked();if(a==android.view.MotionEvent.ACTION_DOWN)v.animate().scaleX(.97f).scaleY(.97f).setDuration(90).start();
            else if(a==android.view.MotionEvent.ACTION_UP||a==android.view.MotionEvent.ACTION_CANCEL)v.animate().scaleX(1).scaleY(1).setDuration(140).start();return false;});return b;
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
        Intent i=new Intent(this,RecorderService.class).setAction(cmd);
        if("begin".equals(cmd)){i.putExtra("resolution",resolutions[resolution.getSelectedItemPosition()]);i.putExtra("fps",rates[fps.getSelectedItemPosition()]);i.putExtra("auto_hide",autoHide.isChecked());}
        startForegroundService(i);
    }
    Spinner spinner(String[] values){
        Spinner s=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(adapter);
        s.setBackground(shape(0xff262c36,14));s.setPadding(dp(12),dp(10),dp(12),dp(10));return s;
    }
    void detectFps(){
        new Thread(()->{
            boolean supported=false; String failure="";
            try{
                java.lang.Process p=new ProcessBuilder("su","-c","/system/bin/sh /data/adb/modules/lockrec_universal/action.sh capabilities").redirectErrorStream(true).start();
                java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
                String line;while((line=reader.readLine())!=null){if(line.trim().equals("fps=selectable"))supported=true;}
                if(p.waitFor()!=0)failure="Root/module unavailable. Install v0.8 and grant root.";
            }catch(Exception e){failure="Root/module unavailable. Install v0.8 and grant root.";}
            final boolean ok=supported;final String error=failure;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                if(!error.isEmpty()){fpsNote.setText(error);return;}
                if(!ok){fpsNote.setText("This device's root recorder cannot set a fixed FPS.");return;}
                android.util.DisplayMetrics m=new android.util.DisplayMetrics();getWindowManager().getDefaultDisplay().getRealMetrics(m);
                int[] candidates=FpsCapabilities.options(this,m.widthPixels&~1,m.heightPixels&~1);
                java.util.ArrayList<String> a=new java.util.ArrayList<>(), names=new java.util.ArrayList<>();a.add("auto");names.add("Auto");
                for(int f:new int[]{30,45,60,90,120,144,165,180,240,360})for(int supportedRate:candidates)if(f==supportedRate){a.add(""+f);names.add(f+" FPS");break;}
                rates=a.toArray(new String[0]);
                ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);fps.setAdapter(adapter);
                fpsNote.setText("FPS is a requested rate; actual frames depend on the device.");
            });
        },"lockrec-capabilities").start();
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setGravity(Gravity.CENTER);root.setPadding(dp(24),dp(32),dp(24),dp(32));scroll.addView(root);
        TextView name=label("LockRec",36,WHITE);name.setTypeface(Typeface.create("sans-serif-medium",0));add(root,name,0);
        add(root,label("NIGHT EDITION  ·  v0.9",13,BLUE),8);
        MascotView hero=new MascotView(this);LinearLayout.LayoutParams heroParams=new LinearLayout.LayoutParams(-1,dp(174));heroParams.topMargin=dp(18);root.addView(hero,heroParams);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(20),dp(24),dp(20),dp(24));panel.setBackground(shape(CARD,28));add(root,panel,16);
        add(panel,label("Capture settings",22,WHITE),0);
        add(panel,label("Lock-screen recording · Root",13,MUTED),8);
        android.util.DisplayMetrics dm=new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        int shortEdge=Math.min(dm.widthPixels,dm.heightPixels);
        java.util.ArrayList<String> r=new java.util.ArrayList<>();
        r.add("native");
        for(int n:new int[]{480,720,1080,1440,2160})if(n<shortEdge)r.add(""+n);
        resolutions=r.toArray(new String[0]);
        String[] rl=new String[resolutions.length];rl[0]="Native · "+dm.widthPixels+" × "+dm.heightPixels;
        for(int i=1;i<rl.length;i++)rl[i]=resolutions[i]+"p";
        add(panel,label("Resolution",13,MUTED),20);
        resolution=spinner(rl);add(panel,resolution,4);
        add(panel,label("Frame rate",13,MUTED),14);
        fps=spinner(new String[]{"Auto · native engine"});add(panel,fps,4);
        fpsNote=label("Checking recorder FPS support…",12,MUTED);add(panel,fpsNote,5);
        detectFps();
        autoHide=new CheckBox(this);autoHide.setText("Hide toolbar before recording");autoHide.setTextColor(WHITE);autoHide.setChecked(getPreferences(0).getBoolean("auto_hide",true));autoHide.setOnCheckedChangeListener((v,checked)->getPreferences(0).edit().putBoolean("auto_hide",checked).apply());add(panel,autoHide,10);
        add(panel,button("Start recording",BLUE,0xff111a28,()->control("begin")),24);
        add(panel,button("End recording",0xff30252a,0xffffc0c0,()->control("end")),10);
        add(root,button("Floating controls",CARD,WHITE,()->{
            if(!Settings.canDrawOverlays(this)){
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));
                Toast.makeText(this,"Allow display over other apps, then return and tap again.",Toast.LENGTH_LONG).show();
            }else control("show");
        }),16);
        add(root,label("Stop or pause from the recording notification. A visible floating toolbar may appear in videos. Videos: Movies/LockRec.",12,MUTED),14);
        add(root,label("Requires root + LockRec module v0.8. AOD depends on device.",12,MUTED),8);
        setContentView(scroll);
    }
}