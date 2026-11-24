package org.lockrec.controller;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.media.projection.MediaProjectionConfig;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import android.view.View;
import java.io.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    static final int CAPTURE=91;
    static final String SCRIPT="/data/adb/modules/lockrec_universal/action.sh";
    final ExecutorService io=Executors.newSingleThreadExecutor();
    LinearLayout body, page;
    int[] rates=new int[0];
    static final int BG=0xFF080A0E, CARD=0xFF151A23, INK=0xFFF0F4FC, MUTED=0xFFA9B4C8, ACCENT=0xFF91B9FF;
    TextView status;
    Spinner fps;
    int chosen=60;
    boolean busy=false;
    int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    android.graphics.drawable.GradientDrawable shape(int color,int radius){
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
        bg.setColor(color);bg.setCornerRadius(dp(radius));return bg;
    }
    void text(String value,int size){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);
        t.setTextColor(size>=20?INK:MUTED);
        if(size>=20)t.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));
        t.setPadding(0,dp(5),0,dp(7));body.addView(t);
    }
    void section(String name){
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16),dp(15),dp(16),dp(15));body.setBackground(shape(CARD,24));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(14);
        page.addView(body,lp);text(name,20);
    }
    void button(String name,View.OnClickListener action){
        Button b=new Button(this);b.setText(name);b.setTextSize(14);b.setAllCaps(false);
        boolean primary=name.startsWith("Start");
        b.setTextColor(primary?0xFF0A1731:INK);b.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));
        b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x336E99E0),shape(primary?ACCENT:0xFF242D3B,15),null));
        b.setGravity(android.view.Gravity.CENTER_VERTICAL|android.view.Gravity.CENTER_HORIZONTAL);
        b.setMinHeight(dp(46));b.setPadding(dp(12),dp(9),dp(12),dp(9));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(7);
        body.addView(b,lp);b.setOnClickListener(action);
    }
    boolean notifications(){
        if(getSystemService(NotificationManager.class).areNotificationsEnabled())return true;
        if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1);
        else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
        status.setText("Allow notifications, then tap the control again.");
        return false;
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(BG);scroll.setFillViewport(true);
        page=new LinearLayout(this);body=page;
        body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(24),dp(20),dp(28));
        scroll.addView(body);setContentView(scroll);
        text("LockRec",36);text("UNIVERSAL RECORDING  /  0.6 BETA",12);
        android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        rates=FpsCapabilities.options(this,metrics.widthPixels&~1,metrics.heightPixels&~1);
        section("Device capabilities");
        text(metrics.widthPixels+" × "+metrics.heightPixels+"  •  "+FpsCapabilities.maxRefresh(this)+" Hz display",17);
        text(rates.length==0?"High FPS encoder support not reported":"Highest reported recording target: "+rates[rates.length-1]+" FPS",14);
        status=new TextView(this);status.setText("Ready");status.setTextColor(0xFF9CC7FF);status.setTextSize(15);body.addView(status);
        button("Enable recording notifications",v->{
            if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1);
            else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
        });
        section("Lock screen · root");
        text("Requires the installed LockRec v0.4 module and root permission. Uses the device's native recorder. FPS is automatic; AOD varies by firmware.",14);
        button("Start · native resolution",v->{if(notifications()&&!HighFpsService.running)root("start native");});
        button("Start · performance profile",v->{if(notifications()&&!HighFpsService.running)root("start performance");});
        button("Pause / save current clip",v->root("stop"));
        button("Resume · new native clip",v->{if(notifications()&&!HighFpsService.running)root("start native");});
        button("End root recording",v->root("stop"));
        button("Check root recording status",v->root("status"));
        section("Floating controls");
        button("Allow display over other apps",v->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
        button("Show floating toolbar",v->{
            if(!Settings.canDrawOverlays(this)){status.setText("Enable display over other apps first.");return;}
            if(notifications())startForegroundService(new Intent(this,RecorderService.class).setAction("show"));
        });
        text("Toolbar may be recorded while shown. Use Hide or the notification controls for clean clips.",14);
        section("High FPS · screen-on");
        text("Separate Android screen-sharing mode. Choose a hardware-reported FPS target at current display resolution, including rates beyond 120 where available. Actual capture cadence may be lower. Locking may end the session. No root needed.",14);
        fps=new Spinner(this);
        String[] labels=new String[rates.length==0?1:rates.length];
        if(rates.length==0)labels[0]="No supported rate detected";
        for(int i=0;i<rates.length;i++)labels[i]=rates[i]+" FPS"+(i==rates.length-1?" · Max":"");
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels);
        fps.setAdapter(adapter);fps.setBackground(shape(0xFF242D3B,15));fps.setPadding(dp(12),dp(12),dp(12),dp(12));body.addView(fps);
        if(rates.length>0)fps.setSelection(rates.length-1);
        button("Start high FPS recording",v->{
            if(Build.VERSION.SDK_INT<29){status.setText("High FPS mode needs Android 10 or newer.");return;}
            if(!notifications()||HighFpsService.running)return;
            if(rates.length==0){status.setText("No supported hardware rate detected.");return;}
            chosen=rates[fps.getSelectedItemPosition()];
            MediaProjectionManager manager=getSystemService(MediaProjectionManager.class);
            Intent i=Build.VERSION.SDK_INT>=34?manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay()):manager.createScreenCaptureIntent();
            startActivityForResult(i,CAPTURE);
        });
        button("Pause / resume high FPS",v->{
            if(HighFpsService.running)startService(new Intent(this,HighFpsService.class).setAction("pause"));
        });
        button("End high FPS recording",v->{
            if(HighFpsService.running)startService(new Intent(this,HighFpsService.class).setAction("stop"));
        });
        body=page;
        text("Saved to Movies/LockRec. Keep the recording indicator enabled. Do not run both modes together. Avoid recording credentials or other people's private information.",13);
    }
    void root(String action){
        if(busy)return;busy=true;status.setText("Checking module…");
        io.execute(()->{
            String result;
            try{
                Process p=new ProcessBuilder("su","-c","/system/bin/sh "+SCRIPT+" "+action).redirectErrorStream(true).start();
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                try(InputStream in=p.getInputStream()){byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1)bytes.write(b,0,n);}
                int rc=p.waitFor();result=(rc==0?"":"Error: ")+bytes.toString("UTF-8").trim();
            }catch(Exception e){result="Root or module unavailable: "+e.getMessage();}
            final String value=result;runOnUiThread(()->{busy=false;if(!isFinishing())status.setText(value);});
        });
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==CAPTURE&&result==RESULT_OK&&data!=null){
            android.util.DisplayMetrics dm=new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(dm);
            Intent i=new Intent(this,HighFpsService.class).setAction("start")
                .putExtra("consent",data).putExtra("code",result).putExtra("fps",chosen)
                .putExtra("width",dm.widthPixels).putExtra("height",dm.heightPixels).putExtra("density",dm.densityDpi);
            startForegroundService(i);
            status.setText("High FPS requested. Check recording notification for actual start or errors.");
        }
    }
    @Override protected void onDestroy(){io.shutdown();super.onDestroy();}
}
