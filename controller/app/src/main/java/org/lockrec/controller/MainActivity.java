package org.lockrec.controller;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import android.view.Gravity;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40,80,40,40);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=new TextView(this); title.setText("LockRec Controller"); title.setTextSize(26); layout.addView(title);
        TextView info=new TextView(this); info.setText("Floating controls for your own recordings. Allow overlay and notifications, then open the toolbar. Grant root when prompted. Pause saves the current clip; Resume creates a new clip. Native resolution and unlimited duration on supported devices. Hide the toolbar to keep it out of the video; the recording notification stays active."); layout.addView(info);
        Button overlay=new Button(this); overlay.setText("Allow floating toolbar");
        overlay.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())))); layout.addView(overlay);
        Button notification=new Button(this); notification.setText("Allow recording notifications");
        notification.setOnClickListener(v->{
            if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1);
            else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
        }); layout.addView(notification);
        Button open=new Button(this); open.setText("Open floating controller");
        open.setOnClickListener(v->{
            if(!Settings.canDrawOverlays(this) || !getSystemService(android.app.NotificationManager.class).areNotificationsEnabled()) {
                Toast.makeText(this,"Enable overlay and notifications first",Toast.LENGTH_LONG).show(); return;
            }
            startForegroundService(new Intent(this,RecorderService.class).setAction("show")); finish();
        }); layout.addView(open);
        setContentView(layout);
        if(getIntent().getBooleanExtra("from_module",false) && Settings.canDrawOverlays(this) && getSystemService(android.app.NotificationManager.class).areNotificationsEnabled()){
            startForegroundService(new Intent(this,RecorderService.class).setAction("show")); finish();
        }
    }
}
