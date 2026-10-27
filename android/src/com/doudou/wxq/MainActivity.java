package com.doudou.wxq;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;

public final class MainActivity extends Activity {
 private AppView view;private FrameLayout root;
 private boolean startPending=false,installPending=false,permissionPending=false,notificationPending=false,notificationPrompted=false,destroyed=false;
 private final Handler handler=new Handler(Looper.getMainLooper());private final OverlayStartGate startGate=new OverlayStartGate();private Runnable timeout;
 @Override public void onCreate(Bundle b){
  super.onCreate(b);Repository.get(this).resetRound(true);root=new FrameLayout(this);root.setBackgroundColor(0xff101724);
  root.setOnApplyWindowInsetsListener((v,i)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=i.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});setContentView(root);loadMainView();Repository.get(this).checkUpdates();
  if(Settings.canDrawOverlays(this)&&OverlayService.instance==null&&Repository.get(this).prefs.getBoolean("overlay_enabled",true))try{startForegroundService(new Intent(this,OverlayService.class));}catch(RuntimeException e){Toast.makeText(this,"自动启用未完成，请点击启用悬浮图标重试",Toast.LENGTH_LONG).show();}
  if(getIntent().getBooleanExtra("startOverlay",false)){getIntent().removeExtra("startOverlay");requestOverlay();}
 }
 private void loadMainView(){if(destroyed)return;if(view!=null){view.dispose();view=null;}root.removeAllViews();try{view=new AppView(this,false,failed->{if(view==failed){view=null;showReload();}});root.addView(view,new FrameLayout.LayoutParams(-1,-1));}catch(RuntimeException e){showReload();}}
 private void showReload(){if(destroyed)return;root.removeAllViews();Button retry=new Button(this);retry.setText("界面暂时不可用，点击重新加载");retry.setOnClickListener(v->loadMainView());root.addView(retry,new FrameLayout.LayoutParams(-1,-2,android.view.Gravity.CENTER));}
 @Override protected void onStart(){super.onStart();if(!startPending&&!installPending){Repository.get(this).resetRound(true);Repository.get(this).checkUpdates();Repository.get(this).updater.check();}}
 void requestOverlay(){if(startPending)return;startPending=true;if(Settings.canDrawOverlays(this)){notificationThenStart();return;}permissionPending=true;Toast.makeText(this,"请允许显示在其他应用上层，然后返回软件",Toast.LENGTH_LONG).show();try{startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())),42);}catch(RuntimeException e){permissionPending=false;startPending=false;Toast.makeText(this,"请在系统设置中手动开启悬浮窗权限",Toast.LENGTH_LONG).show();}}
 private void notificationThenStart(){if(Build.VERSION.SDK_INT>=33&&!notificationPrompted&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){notificationPrompted=true;notificationPending=true;try{requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},4);return;}catch(RuntimeException ignored){notificationPending=false;}}startPanel();}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==42&&permissionPending){permissionPending=false;if(Settings.canDrawOverlays(this))notificationThenStart();else{startPending=false;Toast.makeText(this,"未获得悬浮权限，尚未启用",Toast.LENGTH_LONG).show();}}}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==4&&notificationPending){notificationPending=false;startPanel();}}
 private void startPanel(){
  if(destroyed)return;final long ticket=startGate.begin();
  ResultReceiver callback=new ResultReceiver(handler){protected void onReceiveResult(int code,Bundle data){if(destroyed||!startGate.complete(ticket))return;if(timeout!=null)handler.removeCallbacks(timeout);startPending=false;if(code==1){Toast.makeText(MainActivity.this,"悬浮图标已启用，正在返回桌面",Toast.LENGTH_SHORT).show();try{startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(RuntimeException e){moveTaskToBack(true);}}else Toast.makeText(MainActivity.this,data==null?"悬浮窗启用失败，请检查权限后重试":data.getString("message","悬浮窗启用失败"),Toast.LENGTH_LONG).show();}};
  timeout=()->{if(startGate.complete(ticket)){startPending=false;Toast.makeText(this,"尚未收到系统启用回执，请点击重试或检查权限",Toast.LENGTH_LONG).show();}};handler.postDelayed(timeout,8000);
  try{startForegroundService(new Intent(this,OverlayService.class).putExtra(OverlayService.START_CALLBACK,callback));}catch(RuntimeException e){cancelOverlayStart();Toast.makeText(this,"悬浮服务未能启动："+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}
 }
 void cancelOverlayStart(){startGate.cancel();startPending=false;if(timeout!=null)handler.removeCallbacks(timeout);}
 @Override protected void onStop(){super.onStop();if(startGate.waiting()&&!permissionPending&&!notificationPending)cancelOverlayStart();}
 @Override protected void onResume(){super.onResume();if(installPending){installPending=false;if(getPackageManager().canRequestPackageInstalls())installUpdate();}}
 void installUpdate(){if(!Repository.get(this).updater.canInstall()){Toast.makeText(this,"请先下载并校验更新",Toast.LENGTH_SHORT).show();return;}try{if(!getPackageManager().canRequestPackageInstalls()){installPending=true;Toast.makeText(this,"请允许本应用安装更新，然后返回",Toast.LENGTH_LONG).show();startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));return;}Intent i=new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://"+getPackageName()+".updates/update.apk"),"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);i.setClipData(android.content.ClipData.newRawUri("软件更新",i.getData()));startActivity(i);}catch(Exception e){installPending=false;Toast.makeText(this,"无法打开系统安装器："+e.getMessage(),Toast.LENGTH_LONG).show();}}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(i.getBooleanExtra("startOverlay",false)){i.removeExtra("startOverlay");requestOverlay();}}
 @Override protected void onDestroy(){destroyed=true;cancelOverlayStart();handler.removeCallbacksAndMessages(null);if(view!=null){view.dispose();view=null;}super.onDestroy();}
 @Override public void onBackPressed(){if(view!=null)view.evaluateJavascript("window.App.back()",null);else super.onBackPressed();}
}
