package org.lockrec.controller;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Range;

public class HighFpsService extends Service {
    static volatile boolean running=false;
    static final String CHANNEL="lockrec_high_fps";
    static final int ID=55;
    MediaProjection projection;
    VirtualDisplay display;
    MediaRecorder recorder;
    ParcelFileDescriptor fd;
    Uri output;
    boolean started=false,paused=false,finishing=false;
    int fps;
    String mode="";
    final Handler main=new Handler(Looper.getMainLooper());
    @Override public IBinder onBind(Intent i){return null;}
    PendingIntent action(String name,int code){
        return PendingIntent.getService(this,code,new Intent(this,HighFpsService.class).setAction(name),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    Notification notice(String text,boolean active){
        Notification.Builder b=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("LockRec · high FPS").setContentText(text).setOngoing(active)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(PendingIntent.getActivity(this,91,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        if(active)b.addAction(new Notification.Action.Builder(null,paused?"Resume":"Pause",action("pause",1)).build())
            .addAction(new Notification.Action.Builder(null,"End",action("stop",2)).build());
        return b.build();
    }
    @Override public void onCreate(){
        super.onCreate();getSystemService(NotificationManager.class).createNotificationChannel(
            new NotificationChannel(CHANNEL,"High FPS recording",NotificationManager.IMPORTANCE_LOW));
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        String a=intent.getAction();
        if("stop".equals(a)){finish("Recording saved");return START_NOT_STICKY;}
        if("pause".equals(a)){
            if(started&&!finishing)try{
                if(paused)recorder.resume();else recorder.pause();paused=!paused;
                getSystemService(NotificationManager.class).notify(ID,notice(paused?"Paused":mode,true));
            }catch(Exception e){finish("Recording interrupted");}
            return START_NOT_STICKY;
        }
        if(!"start".equals(a)||running)return START_NOT_STICKY;
        running=true;
        if(Build.VERSION.SDK_INT>=29)startForeground(ID,notice("Preparing recording",true),ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        else {stopSelf();return START_NOT_STICKY;}
        try{
            if(!getSystemService(NotificationManager.class).areNotificationsEnabled()||getSystemService(NotificationManager.class).getNotificationChannel(CHANNEL).getImportance()==NotificationManager.IMPORTANCE_NONE)throw new IllegalStateException("Recording notifications disabled");
            fps=intent.getIntExtra("fps",60);if(fps!=60&&fps!=90&&fps!=120)throw new IllegalArgumentException("Unsupported FPS request");
            int w=intent.getIntExtra("width",0)&~1,h=intent.getIntExtra("height",0)&~1,dpi=intent.getIntExtra("density",0);
            if(w<2||h<2||dpi<=0)throw new IllegalArgumentException("Invalid display size");
            int encoder=0, bitrate=0;
            String[] types={"video/avc","video/hevc"};
            for(String type:types){
                for(MediaCodecInfo info:new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()){
                    if(!info.isEncoder()||!info.isHardwareAccelerated())continue;
                    try{
                        MediaCodecInfo.VideoCapabilities caps=info.getCapabilitiesForType(type).getVideoCapabilities();
                        if(!caps.areSizeAndRateSupported(w,h,fps))continue;
                        Range<Integer> range=caps.getBitrateRange();
                        bitrate=range.clamp((int)Math.min(60000000L,(long)w*h*fps/8));
                        encoder=type.equals("video/avc")?MediaRecorder.VideoEncoder.H264:MediaRecorder.VideoEncoder.HEVC;
                        break;
                    }catch(IllegalArgumentException ignored){}
                }
                if(encoder!=0)break;
            }
            if(encoder==0)throw new IllegalStateException("No hardware encoder reports "+w+"x"+h+" at "+fps+" FPS");
            Intent data=intent.getParcelableExtra("consent");
            projection=getSystemService(MediaProjectionManager.class).getMediaProjection(intent.getIntExtra("code",0),data);
            if(projection==null)throw new IllegalStateException("Screen sharing permission unavailable");
            projection.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){finish("Screen sharing ended");}},main);
            ContentValues values=new ContentValues();
            values.put(MediaStore.Video.Media.DISPLAY_NAME,"LockRec_"+fps+"fps_"+System.currentTimeMillis()+".mp4");
            values.put(MediaStore.Video.Media.MIME_TYPE,"video/mp4");
            values.put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/LockRec");
            values.put(MediaStore.Video.Media.IS_PENDING,1);
            output=getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values);
            if(output==null)throw new IllegalStateException("Cannot create video");
            fd=getContentResolver().openFileDescriptor(output,"rw");
            if(fd==null)throw new IllegalStateException("Cannot open video");
            recorder=Build.VERSION.SDK_INT>=31?new MediaRecorder(this):new MediaRecorder();
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setVideoEncoder(encoder);recorder.setVideoSize(w,h);
            recorder.setVideoFrameRate(fps);recorder.setVideoEncodingBitRate(bitrate);
            recorder.setOutputFile(fd.getFileDescriptor());
            recorder.setOnErrorListener((r,what,extra)->main.post(()->finish("Encoder stopped")));
            recorder.prepare();
            display=projection.createVirtualDisplay("LockRec",w,h,dpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,recorder.getSurface(),null,main);
            if(display==null)throw new IllegalStateException("Virtual display unavailable");
            recorder.start();started=true;mode="Recording · "+fps+" FPS requested";
            getSystemService(NotificationManager.class).notify(ID,notice(mode,true));
        }catch(Exception e){finish("Failed: "+e.getMessage());}
        return START_NOT_STICKY;
    }
    void finish(String reason){
        if(finishing)return;finishing=true;boolean saved=false;
        if(recorder!=null){if(started)try{recorder.stop();saved=true;}catch(RuntimeException ignored){}
            try{recorder.release();}catch(RuntimeException ignored){}recorder=null;}
        if(display!=null){display.release();display=null;}
        if(projection!=null){try{projection.stop();}catch(RuntimeException ignored){}projection=null;}
        if(fd!=null){try{fd.close();}catch(Exception ignored){}fd=null;}
        if(output!=null){try{if(saved){ContentValues cv=new ContentValues();cv.put(MediaStore.Video.Media.IS_PENDING,0);getContentResolver().update(output,cv,null,null);}
            else getContentResolver().delete(output,null,null);}catch(Exception ignored){}output=null;}
        started=false;running=false;stopForeground(STOP_FOREGROUND_REMOVE);
        try{getSystemService(NotificationManager.class).notify(ID+1,notice(saved?reason+" · Movies/LockRec":reason,false));}catch(Exception ignored){}
        stopSelf();
    }
    @Override public void onDestroy(){if(!finishing)finish("Recording service ended");running=false;super.onDestroy();}
}
