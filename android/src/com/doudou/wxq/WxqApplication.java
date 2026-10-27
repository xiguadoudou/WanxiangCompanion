package com.doudou.wxq;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public final class WxqApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static boolean hostVisible=true;
    private int started=0;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable leftApp=()->{if(started==0)publish(false);};
    static boolean isHostVisible(){return hostVisible;}
    private void publish(boolean visible){hostVisible=visible;if(OverlayService.instance!=null)OverlayService.instance.setHostVisible(visible);}
    @Override public void onCreate(){super.onCreate();registerActivityLifecycleCallbacks(this);}
    @Override public void onActivityCreated(Activity a,Bundle b){main.removeCallbacks(leftApp);publish(true);}
    @Override public void onActivityStarted(Activity a){started++;main.removeCallbacks(leftApp);publish(true);}
    @Override public void onActivityStopped(Activity a){started=Math.max(0,started-1);if(started==0)main.postDelayed(leftApp,140);}
    @Override public void onActivityResumed(Activity a){main.removeCallbacks(leftApp);publish(true);}
    @Override public void onActivityPaused(Activity a){}
    @Override public void onActivitySaveInstanceState(Activity a,Bundle b){}
    @Override public void onActivityDestroyed(Activity a){}
}
