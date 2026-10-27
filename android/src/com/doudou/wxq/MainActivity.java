package com.doudou.wxq;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private AppView view;
    private boolean startPending=false,installPending=false;
    @Override public void onCreate(Bundle b){
        super.onCreate(b);Repository.get(this).resetRound(true);view=new AppView(this,false);
        android.widget.FrameLayout root=new android.widget.FrameLayout(this);
        root.setBackgroundColor(0xff101724);
        root.addView(view,new android.widget.FrameLayout.LayoutParams(-1,-1));
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });setContentView(root);
        Repository.get(this).checkUpdates();
        if(Settings.canDrawOverlays(this)&&OverlayService.instance==null&&Repository.get(this).prefs.getBoolean("overlay_enabled",true))startForegroundService(new Intent(this,OverlayService.class));
        if(getIntent().getBooleanExtra("startOverlay",false))requestOverlay();
    }
    @Override protected void onStart(){super.onStart();if(!installPending&&!startPending){Repository.get(this).resetRound(true);Repository.get(this).checkUpdates();Repository.get(this).updater.check();}}
    void requestOverlay(){
        if(Settings.canDrawOverlays(this)){startPanel();return;}
        startPending=true;
        Toast.makeText(this,"请开启万象弈伴的“显示在其他应用上层”权限",Toast.LENGTH_LONG).show();
        try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));}catch(Exception e){startPending=false;Toast.makeText(this,"请在系统设置中手动开启悬浮窗权限",Toast.LENGTH_LONG).show();}
    }
    private void startPanel(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},4);
        Repository.get(this).prefs.edit().putBoolean("overlay_enabled",true).apply();
        startForegroundService(new Intent(this,OverlayService.class));
        Toast.makeText(this,"已启用，离开应用后显示悬浮图标",Toast.LENGTH_SHORT).show();
    }
    @Override protected void onResume(){super.onResume();if(installPending){installPending=false;if(getPackageManager().canRequestPackageInstalls())installUpdate();}if(startPending&&Settings.canDrawOverlays(this)){startPending=false;startPanel();}}
    void installUpdate(){
        if(!Repository.get(this).updater.canInstall()){Toast.makeText(this,"请先下载并校验更新",Toast.LENGTH_SHORT).show();return;}
        try{
            if(!getPackageManager().canRequestPackageInstalls()){installPending=true;Toast.makeText(this,"请允许本应用安装更新，然后返回",Toast.LENGTH_LONG).show();startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));return;}
            Intent i=new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://"+getPackageName()+".updates/update.apk"),"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.setClipData(android.content.ClipData.newRawUri("软件更新",i.getData()));startActivity(i);
        }catch(Exception e){installPending=false;Toast.makeText(this,"无法打开系统安装器："+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(i.getBooleanExtra("startOverlay",false))requestOverlay();}
    @Override protected void onDestroy(){if(view!=null)view.dispose();super.onDestroy();}
    @Override public void onBackPressed(){if(view!=null)view.evaluateJavascript("window.App.back()",null);else super.onBackPressed();}
}
