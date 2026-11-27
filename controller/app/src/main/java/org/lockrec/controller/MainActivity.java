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
    static final int BG=0xff110c22, CARD=0xff251b38, WHITE=0xfffff4fc, MUTED=0xffc4bad8, BLUE=0xff91e0ff;
    Spinner resolution, fps;
    String[] resolutions, rates={"auto"};
    TextView fpsNote;
    CheckBox autoHide;
    AnimeSceneView scene;
    MorphRecordButton recordButton;
    TextView captureStatus, elapsed;
    Button pauseAction, toolbarAction;
    LinearLayout settingsPanel; boolean settingsOpen=false, fpsChecking=false;
    String shownState="", shownMessage="";
    String currentState="checking";
    boolean probing=false, resumed=false, manualFps=false;
    long waitUntil=0, lastProbe=0;
    final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    final Runnable observer=new Runnable(){public void run(){if(!resumed)return;syncState();handler.postDelayed(this,350);}};
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
        if(("begin".equals(cmd)||"show".equals(cmd))&&!notifyAllowed()){Toast.makeText(this,"Allow notifications, then tap again.",Toast.LENGTH_LONG).show();return;}
        Intent i=new Intent(this,RecorderService.class).setAction(cmd);
        if("begin".equals(cmd)){i.putExtra("resolution",resolutions[resolution.getSelectedItemPosition()]);i.putExtra("fps",rates[fps.getSelectedItemPosition()]);i.putExtra("auto_hide",autoHide.isChecked());}
        startForegroundService(i);
        if("begin".equals(cmd)||"end".equals(cmd)){waitUntil=android.os.SystemClock.elapsedRealtime()+1200;applyState("begin".equals(cmd)?"starting":"stopping","begin".equals(cmd)?"Preparing recorder…":"Saving video…");}
    }
    void applyState(String state,String message){
        currentState=state;
        if(recordButton!=null)recordButton.setRecorderState(state);
        if(captureStatus!=null&&!message.contentEquals(captureStatus.getText()))captureStatus.setText(message);
        boolean ready=state.equals("idle")||state.equals("error");
        if(resolution!=null&&resolution.isEnabled()!=ready)resolution.setEnabled(ready);
        if(fps!=null&&fps.isEnabled()!=(ready&&manualFps&&rates.length>1))fps.setEnabled(ready&&manualFps&&rates.length>1);
        if(autoHide!=null&&autoHide.isEnabled()!=ready)autoHide.setEnabled(ready);
        if(scene!=null)scene.setMotionEnabled(false);
        boolean active=state.equals("recording")||state.equals("paused");
        if(pauseAction!=null){
            int vis=active?android.view.View.VISIBLE:android.view.View.GONE;
            if(pauseAction.getVisibility()!=vis)pauseAction.setVisibility(vis);
            String caption=state.equals("paused")?"Resume recording":"Pause recording";
            if(!caption.contentEquals(pauseAction.getText()))pauseAction.setText(caption);
        }
        if(toolbarAction!=null){int vis=active?android.view.View.VISIBLE:android.view.View.GONE;if(toolbarAction.getVisibility()!=vis)toolbarAction.setVisibility(vis);}
        if(elapsed!=null){
            long seconds=RecorderService.startedAt>0?Math.max(0,(android.os.SystemClock.elapsedRealtime()-RecorderService.startedAt)/1000):0;
            String value=state.equals("recording")&&RecorderService.startedAt>0?String.format(java.util.Locale.US,"%02d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60):state.equals("paused")?"Paused":state.equals("starting")?"Starting":state.equals("stopping")?"Saving":"Ready";
            if(!value.contentEquals(elapsed.getText()))elapsed.setText(value);
        }
    }
    void syncState(){
        if(RecorderService.servicePresent){
            String s=RecorderService.uiState;
            if(android.os.SystemClock.elapsedRealtime()<waitUntil&&(s.equals("idle")||s.equals("checking")))return;
            applyState(s,RecorderService.uiMessage);
        }else if(android.os.SystemClock.elapsedRealtime()>=waitUntil && !probing){
            if(android.os.SystemClock.elapsedRealtime()-lastProbe>=2500)probe();
        }
    }
    void probe(){
        if(probing)return;probing=true;lastProbe=android.os.SystemClock.elapsedRealtime();
        new Thread(()->{
            String value="error";
            try{
                java.lang.Process p=new ProcessBuilder("su","-c","/system/bin/sh /data/adb/modules/lockrec_universal/action.sh status").redirectErrorStream(true).start();
                java.io.BufferedReader r=new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));String line;
                while((line=r.readLine())!=null){if(line.trim().equals("Recording"))value="recording";else if(line.trim().equals("Idle"))value="idle";else if(line.trim().equals("Finalizing"))value="stopping";}
                if(p.waitFor()!=0)value="error";
            }catch(Exception ignored){}
            final String result=value;runOnUiThread(()->{probing=false;if(isDestroyed()||!resumed||RecorderService.servicePresent)return;
                applyState(result,result.equals("recording")?"Recording active":result.equals("idle")?"Ready to record":result.equals("stopping")?"Finalizing clip…":"Grant root access / check module");
            });
        },"lockrec-status").start();
    }
    @Override protected void onResume(){super.onResume();resumed=true;if(!RecorderService.servicePresent)probe();if(!manualFps&&!fpsChecking)detectFps();handler.removeCallbacks(observer);handler.post(observer);}
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(observer);if(scene!=null)scene.setMotionEnabled(false);super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacks(observer);super.onDestroy();}
    void openChannel(){
        try{
            startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://t.me/s23_hype")));
        }catch(android.content.ActivityNotFoundException e){
            android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            if(clipboard!=null){clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Telegram channel","https://t.me/s23_hype"));Toast.makeText(this,"Channel link copied",Toast.LENGTH_LONG).show();}
        }
    }
    void showChannelPopup(){
        if(isFinishing()||isDestroyed()||getPreferences(0).getBoolean("channel_prompt_seen",false))return;
        android.app.Dialog dialog=new android.app.Dialog(this);
        LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setGravity(Gravity.CENTER_HORIZONTAL);card.setPadding(dp(22),dp(24),dp(22),dp(18));
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff352044,0xff172b49});
        bg.setCornerRadius(dp(28));bg.setStroke(dp(1),0xffac9de8);card.setBackground(bg);
        android.view.View telegram=new android.view.View(this){
            final android.graphics.Paint p=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(android.graphics.Canvas canvas){
                super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/80f,getHeight()/80f);
                p.setShader(new android.graphics.LinearGradient(0,0,80,80,0xff64dfff,0xff168ad3,android.graphics.Shader.TileMode.CLAMP));canvas.drawCircle(40,40,38,p);p.setShader(null);
                android.graphics.Path plane=new android.graphics.Path();plane.moveTo(15,37);plane.lineTo(62,19);plane.quadTo(66,18,65,23);plane.lineTo(56,61);plane.quadTo(55,65,51,62);plane.lineTo(38,51);plane.lineTo(30,58);plane.lineTo(31,44);plane.lineTo(15,40);plane.quadTo(12,39,15,37);plane.close();p.setColor(0xffffffff);canvas.drawPath(plane,p);
                android.graphics.Path fold=new android.graphics.Path();fold.moveTo(31,44);fold.lineTo(58,26);fold.lineTo(38,51);fold.lineTo(30,58);fold.close();p.setColor(0xffbce4f6);canvas.drawPath(fold,p);canvas.restore();
            }
        };telegram.setContentDescription("Telegram");
        card.addView(telegram,new LinearLayout.LayoutParams(dp(76),dp(76)));
        add(card,label("Join our Telegram",24,WHITE),16);
        add(card,label("@s23_hype",15,BLUE),6);
        add(card,label("Get new releases and updates and share your feedback with the community",14,MUTED),12);
        add(card,button("Join Channel",0xff80d8ff,0xff14223c,()->{dialog.dismiss();openChannel();}),20);
        add(card,button("Maybe later",0xff322843,WHITE,dialog::dismiss),8);
        dialog.setContentView(card);dialog.setCancelable(true);dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(x->getPreferences(0).edit().putBoolean("channel_prompt_seen",true).apply());
        dialog.show();
        android.view.Window window=dialog.getWindow();
        if(window!=null){window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(32),dp(370)),-2);}
        card.setAlpha(0);card.setScaleX(.92f);card.setScaleY(.92f);card.animate().alpha(1).scaleX(1).scaleY(1).setDuration(260).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
    }
    Spinner spinner(String[] values){
        Spinner s=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(adapter);
        s.setBackground(shape(0xff31264b,14));s.setPadding(dp(12),dp(10),dp(12),dp(10));return s;
    }
    void detectFps(){
        if(fpsChecking)return;fpsChecking=true;
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
                fpsChecking=false;if(isFinishing()||isDestroyed())return;
                if(!error.isEmpty()){fpsNote.setText(error);return;}
                if(!ok){manualFps=false;fps.setEnabled(false);fpsNote.setText("Fixed FPS is not supported by this device’s root recorder.");return;}
                manualFps=true;updateRates();
            });
        },"lockrec-capabilities").start();
    }
    void updateRates(){
        if(!manualFps||resolution==null||fps==null)return;
        android.util.DisplayMetrics m=new android.util.DisplayMetrics();getWindowManager().getDefaultDisplay().getRealMetrics(m);
        int w=m.widthPixels&~1,h=m.heightPixels&~1;
        String profile=resolutions[Math.max(0,resolution.getSelectedItemPosition())];
        if(!profile.equals("native")){int edge=Integer.parseInt(profile);float factor=Math.min(1f,edge/(float)Math.min(w,h));w=((int)(w*factor))&~1;h=((int)(h*factor))&~1;}
        int[] candidates=FpsCapabilities.options(this,w,h);
        java.util.ArrayList<String> a=new java.util.ArrayList<>(), names=new java.util.ArrayList<>();a.add("auto");names.add("Auto");
        for(int f:new int[]{30,45,60,90,120,144,165,180,240,360})for(int supportedRate:candidates)if(f==supportedRate){a.add(""+f);names.add(f+" FPS");break;}
        String previous=getPreferences(0).getString("fps",rates[Math.max(0,Math.min(rates.length-1,fps.getSelectedItemPosition()))]);
        rates=a.toArray(new String[0]);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);fps.setAdapter(adapter);fps.setSelection(Math.max(0,a.indexOf(previous)));
        fps.setEnabled((currentState.equals("idle")||currentState.equals("error"))&&rates.length>1);
        fpsNote.setText(rates.length>1?"Requested FPS at selected resolution; actual rate varies.":"No manual FPS rate reported at this resolution.");
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        FrameLayout layers=new FrameLayout(this);layers.setBackgroundColor(BG);
        scene=new AnimeSceneView(this);layers.addView(scene,new FrameLayout.LayoutParams(-1,-1));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(android.graphics.Color.TRANSPARENT);layers.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);root.setPadding(dp(22),dp(24),dp(22),dp(24));scroll.addView(root);
        TextView name=label("LockRec",36,WHITE);name.setTypeface(Typeface.create("sans-serif-medium",0));name.setShadowLayer(dp(10),0,0,0xffb963e6);add(root,name,0);
        add(root,label("LOCK-SCREEN RECORDER",12,BLUE),5);
        android.view.View spacer=new android.view.View(this);root.addView(spacer,new LinearLayout.LayoutParams(-1,dp(100)));
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(20),dp(24),dp(20),dp(24));GradientDrawable glass=shape(0xdb241b39,28);glass.setStroke(dp(1),0x99df8bcf);panel.setBackground(glass);add(root,panel,16);
        elapsed=label("Ready",30,WHITE);elapsed.setTypeface(Typeface.create("sans-serif-medium",0));add(panel,elapsed,0);
        captureStatus=label("Checking recorder…",14,BLUE);add(panel,captureStatus,5);
        recordButton=new MorphRecordButton(this);recordButton.setEnabled(false);recordButton.setContentDescription("Checking recorder");recordButton.setOnClickListener(v->{if(currentState.equals("recording")||currentState.equals("paused"))control("end");else if(currentState.equals("idle")||currentState.equals("error"))control("begin");});
        LinearLayout.LayoutParams recordParams=new LinearLayout.LayoutParams(-1,dp(74));recordParams.topMargin=dp(12);panel.addView(recordButton,recordParams);
        pauseAction=button("Pause recording",0xff372d50,WHITE,()->control("pause"));pauseAction.setVisibility(android.view.View.GONE);add(panel,pauseAction,4);
        toolbarAction=button("Show floating controls",0xff372d50,WHITE,()->{
            if(!Settings.canDrawOverlays(this))startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));
            else control("show");
        });toolbarAction.setVisibility(android.view.View.GONE);add(panel,toolbarAction,8);
        LinearLayout options=new LinearLayout(this);options.setOrientation(1);
        Button settingsToggle=button("Capture settings  ▾",0xff31264b,WHITE,()->{settingsOpen=!settingsOpen;settingsPanel.setVisibility(settingsOpen?android.view.View.VISIBLE:android.view.View.GONE);});
        add(panel,settingsToggle,12);
        settingsPanel=options;settingsPanel.setVisibility(android.view.View.GONE);add(panel,options,4);
        panel=options;
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
        resolution=spinner(rl);add(panel,resolution,4);int saved=java.util.Arrays.asList(resolutions).indexOf(getPreferences(0).getString("resolution","native"));resolution.setSelection(Math.max(0,saved));
        add(panel,label("Frame rate",13,MUTED),14);
        fps=spinner(new String[]{"Auto · native engine"});fps.setEnabled(false);add(panel,fps,4);fps.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){if(manualFps&&pos<rates.length)getPreferences(0).edit().putString("fps",rates[pos]).apply();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        resolution.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){getPreferences(0).edit().putString("resolution",resolutions[pos]).apply();updateRates();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        fpsNote=label("Checking recorder FPS support…",12,MUTED);add(panel,fpsNote,5);
        detectFps();
        autoHide=new CheckBox(this);autoHide.setText("Hide toolbar before recording");autoHide.setTextColor(WHITE);autoHide.setChecked(getPreferences(0).getBoolean("auto_hide",true));autoHide.setOnCheckedChangeListener((v,checked)->getPreferences(0).edit().putBoolean("auto_hide",checked).apply());add(panel,autoHide,10);
        add(root,label("Videos saved to Movies/LockRec",12,MUTED),16);
        add(root,label("Use the notification to pause or stop. Visible toolbar may appear in videos.",12,MUTED),7);
        add(root,button("Community · @s23_hype",0xd2362450,BLUE,this::openChannel),14);
        setContentView(layers);
        scene.setMotionEnabled(false);
    }
}