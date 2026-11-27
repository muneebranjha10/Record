package org.lockrec.controller;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class RecorderService extends Service {
    static final String CHANNEL="lockrec_controls";
    static final String SCRIPT="/data/adb/modules/lockrec_universal/action.sh";
    final Handler main=new Handler(Looper.getMainLooper());
    final ExecutorService io=Executors.newSingleThreadExecutor();
    WindowManager wm; LinearLayout bar; TextView label; Button start, pause, end;
    boolean busy=false, recording=false, paused=false, stopping=false, closed=false, known=false, accessible=false;
    String message="Checking root access";
    public static volatile String uiState="checking", uiMessage="Checking recorder";
    public static volatile boolean servicePresent=false;
    public static volatile long startedAt=0;
    boolean endRequested=false;
    int failures=0;
    String stickyError="";
    boolean awaitingProcess=false;
    long startDeadline=0;
    boolean pendingStart=false, polling=false, autoHide=true;
    String profile="native", fps="auto", lastNotice="";
    Runnable poll=new Runnable(){public void run(){
        if(closed)return;
        if(!busy&&!polling) status();
        main.postDelayed(this,2000);
    }};
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onCreate(){
        super.onCreate();servicePresent=true;startedAt=0;uiState="checking";uiMessage="Checking recorder";
        wm=getSystemService(WindowManager.class);
        NotificationChannel ch=new NotificationChannel(CHANNEL,"Recording controls",NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
        startForeground(34,notification());
        main.post(poll);
    }
    PendingIntent action(String value,int code){
        return PendingIntent.getService(this,code,new Intent(this,RecorderService.class).setAction(value),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    Notification notification(){
        PendingIntent dashboard=PendingIntent.getActivity(this,20,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_camera)
          .setContentTitle("LockRec").setContentText(message).setOngoing(true).setOnlyAlertOnce(true)
          .setVisibility(Notification.VISIBILITY_PUBLIC)
          .setContentIntent(dashboard);
        if(recording||paused)builder.addAction(new Notification.Action.Builder(null,paused?"Resume":"Pause",action("pause",13)).build());
        if(recording||paused||awaitingProcess||stopping||pendingStart)builder.addAction(new Notification.Action.Builder(null,"End",action("end",12)).build());
        return builder.build();
    }
    @Override public int onStartCommand(Intent i,int flags,int id){
        String a=i==null?null:i.getAction();
        if("end".equals(a)) end();
        else if("show".equals(a)) show();
        else if("begin".equals(a)){
            if(!recording&&!pendingStart&&!stopping&&!awaitingProcess){
                stickyError="";endRequested=false;
                profile=i.getStringExtra("resolution");fps=i.getStringExtra("fps");
                if(profile==null)profile="native";if(fps==null)fps="auto";autoHide=i.getBooleanExtra("auto_hide",true);
            }
            if(!awaitingProcess&&!stopping)pendingStart=true;refresh();if(!busy)status();
        }
        else if("pause".equals(a)){if(paused){pendingStart=true;if(!busy)status();}else pause();}
        return START_NOT_STICKY;
    }
    int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    Button button(String s,View.OnClickListener l){
        Button b=new Button(this); b.setText(s); b.setTextSize(18); b.setAllCaps(false);
        b.setTextColor(Color.WHITE); GradientDrawable fill=new GradientDrawable();fill.setColor(0xFF283247);fill.setCornerRadius(dp(20));b.setBackground(fill);b.setMinWidth(0); b.setMinimumWidth(0); b.setPadding(dp(12),0,dp(12),0);b.setMinHeight(dp(42));
        b.setOnClickListener(l); return b;
    }
    void show(){
        if(bar!=null || !Settings.canDrawOverlays(this))return;
        LinearLayout outer=new LinearLayout(this); outer.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg=new GradientDrawable(); bg.setColor(0xF5171B29); bg.setCornerRadius(dp(22)); bg.setStroke(dp(1),0xFF5D6570);
        outer.setBackground(bg); outer.setPadding(dp(10),dp(8),dp(10),dp(8)); outer.setElevation(dp(10));
        label=new TextView(this); label.setTextColor(Color.WHITE); label.setTextSize(12);label.setPadding(dp(5),0,0,dp(5)); outer.addView(label);
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        start=button("▶",v->begin()); start.setContentDescription("Start or resume"); pause=button("Ⅱ",v->pause());pause.setContentDescription("Pause"); end=button("■",v->end());end.setContentDescription("End");
        for(Button b:new Button[]{start,pause,end,button("×",v->hide())}){LinearLayout.LayoutParams item=new LinearLayout.LayoutParams(dp(44),dp(44));item.setMargins(dp(2),0,dp(2),0);row.addView(b,item);}
        outer.addView(row);
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
           WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.TOP|Gravity.END; lp.x=dp(8); lp.y=dp(64);
        try{wm.addView(outer,lp);bar=outer;refresh();}catch(RuntimeException e){bar=null;Toast.makeText(this,"Floating controls unavailable; use the notification.",Toast.LENGTH_LONG).show();}
    }
    void hide(){if(bar!=null){try{wm.removeView(bar);}catch(RuntimeException ignored){}bar=null;}}
    String root(String command)throws Exception{
        if(!Pattern.matches("status|stop|start (native|480|720|1080|1440|2160) (auto|30|45|60|90|120|144|165|180|240|360)",command))throw new IllegalArgumentException();
        java.lang.Process p=new ProcessBuilder("su","-c","/system/bin/sh "+SCRIPT+" "+command).redirectErrorStream(true).start();
        if(!p.waitFor(15,TimeUnit.SECONDS)){p.destroyForcibly();throw new IOException("Root command timed out");}
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(InputStream in=p.getInputStream()){byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
        int rc=p.waitFor(); if(rc!=0)throw new IOException(out.toString("UTF-8")); return out.toString("UTF-8").trim();
    }
    interface Job {String run() throws Exception;}
    interface Result {void done(String s);}
    void run(Job job,Result callback){
        if(busy||closed)return;busy=true;refresh();
        io.execute(()->{String result;try{result=job.run();}catch(Exception e){result="ERROR: "+e.getMessage();}
            final String r=result; main.post(()->{if(closed)return;busy=false;callback.done(r);refresh();});});
    }
    void status(){
        if(busy||polling||closed)return;polling=true;
        io.execute(()->{String value;try{value=root("status");}catch(Exception e){value="ERROR: "+e.getMessage();}final String s=value;
        main.post(()->{polling=false;if(closed||busy)return;
        if(s.equals("Recording")){failures=0;accessible=true;known=true;if(!stopping){if(!recording||startedAt==0)startedAt=SystemClock.elapsedRealtime();awaitingProcess=false;recording=true;paused=false;stickyError="";message="Recording · "+(profile.equals("native")?"Native":profile+"p");if(endRequested){endRequested=false;end();return;}}}
        else if(s.equals("Finalizing")){failures=0;accessible=true;known=true;if(stopping)message="Finalizing clip…";if(awaitingProcess&&SystemClock.elapsedRealtime()>startDeadline){awaitingProcess=false;stickyError="Recorder startup timed out. Check module log.";message=stickyError;}if(!awaitingProcess&&!stopping&&!recording&&!paused&&stickyError.isEmpty()){message="Waiting for previous clip to finish…";}}
        else if(s.equals("Idle")){failures=0;accessible=true;known=true;boolean wasRecording=recording;recording=false;startedAt=0;
            if(awaitingProcess){if(SystemClock.elapsedRealtime()<startDeadline){refresh();return;}awaitingProcess=false;stickyError="Recording did not start; check module log";message=stickyError;refresh();return;}
            if(endRequested){endRequested=false;message="Ended";finishController();return;}
            if(stopping){stopping=false;if(!paused){message="Video saved · Movies/LockRec";finishController();return;}}
            if(wasRecording&&!paused)stickyError="Recorder stopped. Check Movies/LockRec and module log.";
            message=!stickyError.isEmpty()?stickyError:paused?"Paused · clip saved":"Ready to record";}
        else{if(++failures<3){message="Checking recorder connection…";refresh();return;}
            accessible=false;known=false;pendingStart=false;stickyError="Root unavailable. Check KernelSU access and module.";
            message=stickyError;
            if(!recording&&!awaitingProcess&&!stopping){refresh();return;}
            refresh();return;}
        if(pendingStart&&accessible){if(recording){pendingStart=false;if(!autoHide)show();}else if(s.equals("Idle")&&!stopping){pendingStart=false;begin();}}
        refresh();
        });});
    }
    void begin(){
        if(busy||!known||!accessible||recording||stopping||awaitingProcess)return;
        stickyError="";endRequested=false;
        NotificationManager nm=getSystemService(NotificationManager.class);
        if(!nm.areNotificationsEnabled()||nm.getNotificationChannel(CHANNEL).getImportance()==NotificationManager.IMPORTANCE_NONE){message="Enable notifications first";refresh();return;}
        message="Starting…";awaitingProcess=true;startDeadline=SystemClock.elapsedRealtime()+10000;
        if(autoHide)hide();
        run(()->root("start "+profile+" "+fps),s->{if(s.startsWith("ERROR")||!s.contains("Recording requested")){awaitingProcess=false;stickyError="Start failed: "+s;message=stickyError;Toast.makeText(this,message,Toast.LENGTH_LONG).show();return;}paused=false;message="Waiting for recorder…";main.postDelayed(()->{if(!closed)status();},350);if(!autoHide)show();});
    }
    void pause(){
        if(busy||!recording||stopping)return;
        message="Saving clip…";if(autoHide)hide();
        run(()->root("stop"),s->{if(s.startsWith("ERROR")){message="Stop failed";return;}paused=true;stopping=true;recording=false;message="Finalizing clip…";});
    }
    void end(){
        if(busy){main.postDelayed(()->{if(!closed)end();},350);return;}
        if(awaitingProcess){endRequested=true;pendingStart=false;message="Stopping after recorder starts…";refresh();return;}
        paused=false;
        pendingStart=false;if(!recording&&!stopping&&!awaitingProcess&&known){finishController();return;}awaitingProcess=false;
        message="Ending…";hide();
        run(()->root("stop"),s->{if(s.startsWith("ERROR")){message="Stop failed · use module Action";return;}stopping=true;recording=false;message="Finalizing clip…";});
    }
    void refresh(){
        if(closed)return;
        uiState=stopping||endRequested?"stopping":awaitingProcess||pendingStart?"starting":recording?"recording":paused?"paused":!stickyError.isEmpty()?"error":known?"idle":"checking";uiMessage=message;
        if(bar!=null){
            String line=message.length()>70?message.substring(0,70):message;
            if(!line.contentEquals(label.getText()))label.setText(line);
            String title="▶";
            if(!title.contentEquals(start.getText()))start.setText(title);
            if(start.getVisibility()!=(recording?View.GONE:View.VISIBLE))start.setVisibility(recording?View.GONE:View.VISIBLE);
            if(pause.getVisibility()!=(recording?View.VISIBLE:View.GONE))pause.setVisibility(recording?View.VISIBLE:View.GONE);
            boolean canStart=known&&accessible&&!recording&&!stopping, canPause=recording&&!stopping;
            if(start.isEnabled()!=canStart)start.setEnabled(canStart);
            if(pause.isEnabled()!=canPause)pause.setEnabled(canPause);
            if(!end.isEnabled())end.setEnabled(true);
        }
        String noticeKey=message+"|"+uiState;
        if(!noticeKey.equals(lastNotice)){lastNotice=noticeKey;try{getSystemService(NotificationManager.class).notify(34,notification());}catch(RuntimeException ignored){}}
    }
    void finishController(){startedAt=0;uiState="idle";uiMessage=message;closed=true;hide();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
    @Override public void onDestroy(){servicePresent=false;if(!closed){startedAt=0;uiState="checking";uiMessage="Check recorder status";}closed=true;main.removeCallbacksAndMessages(null);hide();io.shutdownNow();super.onDestroy();}
}
