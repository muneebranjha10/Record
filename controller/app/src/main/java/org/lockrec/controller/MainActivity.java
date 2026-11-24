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
    LinearLayout body;
    TextView status;
    Spinner fps;
    int chosen=60;
    boolean busy=false;
    int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    void text(String value,int size){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);
        t.setPadding(0,dp(8),0,dp(8));body.addView(t);
    }
    void button(String name,View.OnClickListener action){
        Button b=new Button(this);b.setText(name);b.setAllCaps(false);
        b.setOnClickListener(action);body.addView(b);
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
        ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(22),dp(24),dp(22),dp(24));
        scroll.addView(body);setContentView(scroll);
        text("LockRec",32);text("Universal recorder · 0.5 beta",15);
        status=new TextView(this);status.setText("Ready");status.setTextColor(0xFF9CC7FF);status.setTextSize(15);body.addView(status);
        button("Enable recording notifications",v->{
            if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1);
            else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
        });
        text("Root lock-screen mode",22);
        text("Requires the installed LockRec v0.4 module and root permission. Uses the device's native recorder. FPS is automatic; AOD varies by firmware.",14);
        button("Start · native resolution",v->{if(notifications()&&!HighFpsService.running)root("start native");});
        button("Start · performance profile",v->{if(notifications()&&!HighFpsService.running)root("start performance");});
        button("Pause / save current clip",v->root("stop"));
        button("Resume · new native clip",v->{if(notifications()&&!HighFpsService.running)root("start native");});
        button("End root recording",v->root("stop"));
        button("Check root recording status",v->root("status"));
        text("Floating controller (optional)",22);
        button("Allow display over other apps",v->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
        button("Show floating toolbar",v->{
            if(!Settings.canDrawOverlays(this)){status.setText("Enable display over other apps first.");return;}
            if(notifications())startForegroundService(new Intent(this,RecorderService.class).setAction("show"));
        });
        text("Toolbar may be recorded while shown. Use Hide or the notification controls for clean clips.",14);
        text("High FPS · screen-on mode",22);
        text("Separate Android screen-sharing mode. Requests 60, 90 or 120 FPS at current display resolution, only when an encoder reports support. Actual capture cadence may be lower. Locking may end the session. No root needed.",14);
        fps=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"60 FPS","90 FPS","120 FPS"});
        fps.setAdapter(adapter);body.addView(fps);
        button("Start high FPS recording",v->{
            if(Build.VERSION.SDK_INT<29){status.setText("High FPS mode needs Android 10 or newer.");return;}
            if(!notifications()||HighFpsService.running)return;
            chosen=new int[]{60,90,120}[fps.getSelectedItemPosition()];
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
