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
    boolean pendingStart=false;
    Runnable poll=new Runnable(){public void run(){
        if(closed)return;
        if(!busy) status();
        main.postDelayed(this,2000);
    }};
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onCreate(){
        super.onCreate();
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
        return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_camera)
          .setContentTitle("LockRec").setContentText(message).setOngoing(true)
          .setVisibility(Notification.VISIBILITY_PUBLIC)
          .setContentIntent(action("show",10))
          .addAction(new Notification.Action.Builder(null,"Controls",action("show",11)).build())
          .addAction(new Notification.Action.Builder(null,paused?"Resume":"Pause",action("pause",13)).build())
          .addAction(new Notification.Action.Builder(null,"End",action("end",12)).build()).build();
    }
    @Override public int onStartCommand(Intent i,int flags,int id){
        String a=i==null?null:i.getAction();
        if("end".equals(a)) end();
        else if("show".equals(a)) show();
        else if("begin".equals(a)){pendingStart=true;if(!busy)status();}
        else if("pause".equals(a)){if(paused){pendingStart=true;if(!busy)status();}else pause();}
        return START_NOT_STICKY;
    }
    int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    Button button(String s,View.OnClickListener l){
        Button b=new Button(this); b.setText(s); b.setTextSize(12); b.setAllCaps(false);
        b.setTextColor(Color.WHITE); GradientDrawable fill=new GradientDrawable();fill.setColor(0xFF353944);fill.setCornerRadius(dp(20));b.setBackground(fill);b.setMinWidth(0); b.setMinimumWidth(0); b.setPadding(dp(12),0,dp(12),0);b.setMinHeight(dp(42));
        b.setOnClickListener(l); return b;
    }
    void show(){
        if(bar!=null || !Settings.canDrawOverlays(this))return;
        LinearLayout outer=new LinearLayout(this); outer.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg=new GradientDrawable(); bg.setColor(0xF522252B); bg.setCornerRadius(dp(22)); bg.setStroke(dp(1),0xFF5D6570);
        outer.setBackground(bg); outer.setPadding(dp(10),dp(8),dp(10),dp(8)); outer.setElevation(dp(10));
        label=new TextView(this); label.setTextColor(Color.WHITE); label.setTextSize(12);label.setPadding(dp(5),0,0,dp(5)); outer.addView(label);
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        start=button("Start",v->begin()); pause=button("Pause",v->pause()); end=button("End",v->end());
        for(Button b:new Button[]{start,pause,end,button("×",v->hide())}){LinearLayout.LayoutParams item=new LinearLayout.LayoutParams(-2,dp(42));item.setMargins(dp(2),0,dp(2),0);row.addView(b,item);}
        outer.addView(row);
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
           WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.TOP|Gravity.END; lp.x=dp(8); lp.y=dp(64);
        try{wm.addView(outer,lp);bar=outer;refresh();}catch(RuntimeException e){bar=null;Toast.makeText(this,"Floating controls unavailable; use the notification.",Toast.LENGTH_LONG).show();}
    }
    void hide(){if(bar!=null){try{wm.removeView(bar);}catch(RuntimeException ignored){}bar=null;}}
    String root(String command)throws Exception{
        if(!Pattern.matches("status|start native|stop",command))throw new IllegalArgumentException();
        java.lang.Process p=new ProcessBuilder("su","-c","/system/bin/sh "+SCRIPT+" "+command).redirectErrorStream(true).start();
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
    void status(){run(()->root("status"),s->{
        if(s.equals("Recording")){accessible=true;known=true;recording=true;if(!stopping){paused=false;message="Recording · native quality";}}
        else if(s.equals("Finalizing")){accessible=true;known=true;message="Finalizing clip…";}
        else if(s.equals("Idle")){accessible=true;known=true;recording=false;if(stopping){stopping=false;if(!paused){message="Ended";finishController();return;}}message=paused?"Paused · clip saved":"Ready";}
        else{accessible=false;known=false;pendingStart=false;message="Root or module unavailable";Toast.makeText(this,"Grant LockRec root in KernelSU and verify the module is installed.",Toast.LENGTH_LONG).show();finishController();return;}
        if(pendingStart&&accessible){if(recording){pendingStart=false;show();}else if(s.equals("Idle")&&!stopping){pendingStart=false;begin();}}
    });}
    void begin(){
        if(!known||!accessible||recording||stopping)return;
        NotificationManager nm=getSystemService(NotificationManager.class);
        if(!nm.areNotificationsEnabled()||nm.getNotificationChannel(CHANNEL).getImportance()==NotificationManager.IMPORTANCE_NONE){message="Enable notifications first";refresh();return;}
        message="Starting…";
        run(()->root("start native"),s->{if(s.startsWith("ERROR")||!s.contains("Recording requested")){message="Start failed: "+s;Toast.makeText(this,message,Toast.LENGTH_LONG).show();return;}paused=false;recording=true;message="Recording · native quality";show();});
    }
    void pause(){
        if(!recording||stopping)return;
        message="Saving clip…";
        run(()->root("stop"),s->{if(s.startsWith("ERROR")){message="Stop failed";return;}paused=true;stopping=true;recording=false;message="Finalizing clip…";});
    }
    void end(){
        if(busy){main.postDelayed(()->{if(!closed)end();},500);return;}
        paused=false;
        pendingStart=false;if(!recording&&!stopping&&known){finishController();return;}
        message="Ending…";
        run(()->root("stop"),s->{if(s.startsWith("ERROR")){message="Stop failed · use module Action";return;}stopping=true;recording=false;message="Finalizing clip…";});
    }
    void refresh(){
        if(closed)return;
        if(bar!=null){
            label.setText(message.length()>70?message.substring(0,70):message);
            start.setText(paused?"Resume":"Start");start.setVisibility(recording?View.GONE:View.VISIBLE);pause.setVisibility(recording?View.VISIBLE:View.GONE);
            start.setEnabled(!busy&&known&&accessible&&!recording&&!stopping);
            pause.setEnabled(!busy&&recording&&!stopping);
            end.setEnabled(!busy);
        }
        try{getSystemService(NotificationManager.class).notify(34,notification());}catch(RuntimeException ignored){}
    }
    void finishController(){closed=true;hide();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
    @Override public void onDestroy(){closed=true;main.removeCallbacksAndMessages(null);hide();io.shutdownNow();super.onDestroy();}
}
