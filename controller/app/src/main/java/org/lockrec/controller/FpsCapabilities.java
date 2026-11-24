package org.lockrec.controller;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.view.Display;
import java.util.TreeSet;

final class FpsCapabilities {
    static int maxRefresh(Context context) {
        Display display=context.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        if(display==null)return 0;
        float max=display.getRefreshRate();
        for(Display.Mode mode:display.getSupportedModes())max=Math.max(max,mode.getRefreshRate());
        return Math.round(max);
    }
    static boolean supports(int width,int height,int fps) {
        if(Build.VERSION.SDK_INT<29||width<2||height<2||fps<1)return false;
        for(MediaCodecInfo info:new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()){
            if(!info.isEncoder()||!info.isHardwareAccelerated())continue;
            for(String mime:new String[]{"video/avc","video/hevc"}){
                try{
                    MediaCodecInfo.VideoCapabilities caps=info.getCapabilitiesForType(mime).getVideoCapabilities();
                    if(caps!=null&&caps.areSizeAndRateSupported(width,height,fps))return true;
                }catch(IllegalArgumentException ignored){}
            }
        }
        return false;
    }
    static int[] options(Context context,int w,int h) {
        int ceiling=maxRefresh(context);
        TreeSet<Integer> rates=new TreeSet<>();
        for(int fps:new int[]{30,45,60,75,90,100,120,144,165,180,200,240,280,300,360,480,ceiling})
            if(fps<=ceiling&&supports(w,h,fps))rates.add(fps);
        // Also include the highest integer cadence reported at this resolution.
        if(Build.VERSION.SDK_INT>=29){
            for(MediaCodecInfo info:new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()){
                if(!info.isEncoder()||!info.isHardwareAccelerated())continue;
                for(String mime:new String[]{"video/avc","video/hevc"})try{
                    MediaCodecInfo.VideoCapabilities caps=info.getCapabilitiesForType(mime).getVideoCapabilities();
                    int candidate=(int)Math.floor(Math.min(ceiling,caps.getSupportedFrameRatesFor(w,h).getUpper()));
                    if(candidate>=30&&caps.areSizeAndRateSupported(w,h,candidate))rates.add(candidate);
                }catch(IllegalArgumentException ignored){}
            }
        }
        int[] result=new int[rates.size()];int i=0;
        for(int rate:rates)result[i++]=rate;
        return result;
    }
}
